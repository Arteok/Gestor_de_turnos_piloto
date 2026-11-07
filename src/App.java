import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gestiona los turnos del consultorio de podología.
 * Atiende las solicitudes HTTP y guarda las reservas en un archivo local.
 */
public class App {
    private final List<Map<String, String>> turnos = new ArrayList<>();
    private final Path archivo;
    private int proximoId = 1;

    public App(Path archivo) throws IOException {
        this.archivo = archivo;
        if (Files.exists(archivo)) {
            for (String linea : Files.readAllLines(archivo, StandardCharsets.UTF_8)) {
                if (linea.isBlank()) continue;
                String[] c = linea.split("\\|", -1);
                if (c.length != 7) continue;
                Map<String, String> turno = new HashMap<>();
                turno.put("id", c[0]); turno.put("paciente", c[1]);
                turno.put("telefono", c[2]); turno.put("servicioId", c[3]);
                turno.put("fecha", c[4]); turno.put("hora", c[5]);
                turno.put("estado", c[6]);
                turnos.add(turno);
                try { proximoId = Math.max(proximoId, Integer.parseInt(c[0]) + 1); }
                catch (NumberFormatException ignored) { /* Datos viejos */ }
            }
        }
    }

    public static HttpServer crearServidor(int puerto, Path archivo) throws IOException {
        App app = new App(archivo);
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", puerto), 0);
        servidor.createContext("/api/catalogo", app::atender);
        servidor.createContext("/api/turnos", app::atender);
        servidor.createContext("/api/salud", app::atender);
        servidor.createContext("/", app::atender);
        return servidor;
    }

    public static void main(String[] args) throws Exception {
        int puerto = Integer.parseInt(System.getProperty("puerto", "8080"));
        Path ruta = Path.of(System.getProperty("datos", "data/turnos.txt"));
        HttpServer servidor = crearServidor(puerto, ruta);
        servidor.start();
        System.out.println("Gestor de turnos piloto: http://127.0.0.1:" + servidor.getAddress().getPort());
    }

    private synchronized void atender(HttpExchange e) throws IOException {
        String ruta = e.getRequestURI().getPath();
        String metodo = e.getRequestMethod();
        if (ruta.equals("/api/salud") && metodo.equals("GET")) {
            responder(e, 200, "{\"ok\":true}", "application/json; charset=utf-8"); return;
        }
        if (ruta.equals("/api/catalogo") && metodo.equals("GET")) {
            responder(e, 200, "{\"servicios\":[{\"id\":1,\"nombre\":\"Consulta podológica\",\"duracionMinutos\":30},"
                + "{\"id\":2,\"nombre\":\"Tratamiento podológico\",\"duracionMinutos\":60}]}", "application/json; charset=utf-8"); return;
        }
        if (ruta.equals("/api/turnos") && metodo.equals("GET")) {
            StringBuilder salida = new StringBuilder("{\"turnos\":[");
            for (int i = 0; i < turnos.size(); i++) {
                Map<String, String> t = turnos.get(i);
                if (i > 0) salida.append(',');
                salida.append("{\"id\":").append(t.get("id"))
                    .append(",\"paciente\":\"").append(esc(t.get("paciente")))
                    .append("\",\"telefono\":\"").append(esc(t.get("telefono")))
                    .append("\",\"servicioId\":").append(t.get("servicioId"))
                    .append(",\"servicio\":\"").append(t.get("servicioId").equals("1") ? "Consulta podológica" : "Tratamiento podológico")
                    .append("\",\"fecha\":\"").append(esc(t.get("fecha")))
                    .append("\",\"hora\":\"").append(esc(t.get("hora")))
                    .append("\",\"duracionMinutos\":").append(t.get("servicioId").equals("1") ? "30" : "60")
                    .append(",\"estado\":\"").append(t.get("estado")).append("\"}");
            }
            salida.append("]}");
            responder(e, 200, salida.toString(), "application/json; charset=utf-8"); return;
        }
        if (metodo.equals("POST") && (ruta.equals("/api/turnos") || ruta.equals("/api/turnos/cancelar") || ruta.equals("/api/turnos/reprogramar"))) {
            Map<String, String> datos = new HashMap<>();
            String cuerpo = new String(e.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            for (String par : cuerpo.split("&")) {
                if (par.isBlank()) continue;
                String[] kv = par.split("=", 2);
                datos.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8), URLDecoder.decode(kv.length > 1 ? kv[1] : "", StandardCharsets.UTF_8));
            }
            if (ruta.equals("/api/turnos")) {
                String paciente = datos.getOrDefault("paciente", "").trim();
                String telefono = datos.getOrDefault("telefono", "").trim();
                String servicio = datos.getOrDefault("servicioId", "");
                String fecha = datos.getOrDefault("fecha", "");
                String hora = datos.getOrDefault("hora", "");
                if (paciente.length() < 2 || paciente.length() > 80 || paciente.contains("|") || paciente.contains("\n") || paciente.contains("\r")
                        || !telefono.matches("[0-9]{6,15}") || !(servicio.equals("1") || servicio.equals("2"))) {
                    error(e, 400, "Datos de paciente o servicio inválidos"); return;
                }
                if (!validarDisponibilidad(e, fecha, hora, servicio, null)) {
                    return;
                }
                Map<String, String> t = new HashMap<>();
                t.put("id", String.valueOf(proximoId++)); t.put("paciente", paciente); t.put("telefono", telefono);
                t.put("servicioId", servicio); t.put("fecha", fecha); t.put("hora", hora); t.put("estado", "PENDIENTE");
                turnos.add(t);
                guardar();
                responder(e, 201, "{\"ok\":true,\"mensaje\":\"Turno reservado\",\"id\":" + t.get("id") + "}", "application/json; charset=utf-8"); return;
            }
            int id;
            try { id = Integer.parseInt(datos.getOrDefault("id", "")); }
            catch (Exception ex) { error(e, 400, "ID inválido"); return; }
            Map<String, String> encontrado = null;
            for (Map<String, String> t : turnos) if (Integer.parseInt(t.get("id")) == id) { encontrado = t; break; }
            if (encontrado == null) { error(e, 404, "Turno inexistente"); return; }
            if (encontrado.get("estado").equals("CANCELADO")) { error(e, 409, "El turno ya está cancelado"); return; }
            if (ruta.equals("/api/turnos/cancelar")) {
                encontrado.put("estado", "CANCELADO"); guardar();
                responder(e, 200, "{\"ok\":true,\"mensaje\":\"Turno cancelado\"}", "application/json; charset=utf-8"); return;
            }
            String fecha = datos.getOrDefault("fecha", "");
            String hora = datos.getOrDefault("hora", "");
            if (!validarDisponibilidad(e, fecha, hora, encontrado.get("servicioId"), encontrado)) {
                return;
            }
            encontrado.put("fecha", fecha); encontrado.put("hora", hora); guardar();
            responder(e, 200, "{\"ok\":true,\"mensaje\":\"Turno reprogramado\"}", "application/json; charset=utf-8"); return;
        }
        if (metodo.equals("GET") && (ruta.equals("/") || ruta.equals("/app.js") || ruta.equals("/style.css"))) {
            String archivoWeb = ruta.equals("/") ? "index.html" : ruta.substring(1);
            Path archivoEstatico = Path.of("web", archivoWeb);
            if (!Files.exists(archivoEstatico)) { error(e, 404, "Archivo inexistente"); return; }
            byte[] bytes = Files.readAllBytes(archivoEstatico);
            e.getResponseHeaders().set("Content-Type", archivoWeb.endsWith(".css") ? "text/css; charset=utf-8"
                    : archivoWeb.endsWith(".js") ? "application/javascript; charset=utf-8" : "text/html; charset=utf-8");
            e.sendResponseHeaders(200, bytes.length);
            try (var os = e.getResponseBody()) { os.write(bytes); }
            return;
        }
        error(e, 404, "Recurso inexistente");
    }


    private boolean validarDisponibilidad(HttpExchange e, String fecha, String hora,
            String servicioId, Map<String, String> turnoActual) throws IOException {
        LocalTime inicio;
        try {
            LocalDate.parse(fecha);
            inicio = LocalTime.parse(hora);
        } catch (java.time.format.DateTimeParseException ex) {
            error(e, 400, "Fecha u horario inválidos");
            return false;
        }

        int duracion = servicioId.equals("1") ? 30 : 60;
        if (inicio.getMinute() % 30 != 0 || inicio.getSecond() != 0
                || inicio.isBefore(LocalTime.of(9, 0))
                || inicio.plusMinutes(duracion).isAfter(LocalTime.of(18, 0))) {
            error(e, 400, "Fecha u horario fuera de la agenda");
            return false;
        }

        int nuevoInicio = inicio.toSecondOfDay() / 60;
        int nuevoFin = nuevoInicio + duracion;
        for (Map<String, String> turno : turnos) {
            if (turno == turnoActual || !turno.get("fecha").equals(fecha)
                    || turno.get("estado").equals("CANCELADO")) {
                continue;
            }
            int anteriorInicio = LocalTime.parse(turno.get("hora")).toSecondOfDay() / 60;
            int anteriorFin = anteriorInicio + (turno.get("servicioId").equals("1") ? 30 : 60);
            if (nuevoInicio < anteriorFin && anteriorInicio < nuevoFin) {
                error(e, 409, "El horario se superpone con otro turno");
                return false;
            }
        }
        return true;
    }

    private void guardar() throws IOException {
        if (archivo.getParent() != null) Files.createDirectories(archivo.getParent());
        List<String> lineas = new ArrayList<>();
        for (Map<String, String> t : turnos) lineas.add(String.join("|", t.get("id"), t.get("paciente"), t.get("telefono"),
                t.get("servicioId"), t.get("fecha"), t.get("hora"), t.get("estado")));
        Files.write(archivo, lineas, StandardCharsets.UTF_8);
    }

    private static String esc(String v) { return v.replace("\\", "\\\\").replace("\"", "\\\"")
            .replace("\n", "\\n").replace("\r", "\\r"); }
    private void error(HttpExchange e, int estado, String mensaje) throws IOException {
        responder(e, estado, "{\"ok\":false,\"mensaje\":\"" + esc(mensaje) + "\"}", "application/json; charset=utf-8");
    }
    private void responder(HttpExchange e, int estado, String cuerpo, String tipo) throws IOException {
        byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
        e.getResponseHeaders().set("Content-Type", tipo);
        e.getResponseHeaders().set("Cache-Control", "no-store");
        e.sendResponseHeaders(estado, bytes.length);
        try (var os = e.getResponseBody()) { os.write(bytes); }
    }
}
