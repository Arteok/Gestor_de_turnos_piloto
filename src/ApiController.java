import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Controlador HTTP: adapta peticiones y respuestas sin implementar reglas del negocio. */
public class ApiController {
    private final TurnoService servicio;

    public ApiController(TurnoService servicio) { this.servicio = servicio; }

    public synchronized void atender(HttpExchange e) throws IOException {
        String ruta = e.getRequestURI().getPath();
        String metodo = e.getRequestMethod();
        try {
            if (ruta.equals("/api/salud") && metodo.equals("GET")) {
                json(e, 200, "{\"ok\":true}"); return;
            }
            if (ruta.equals("/api/catalogo") && metodo.equals("GET")) {
                StringBuilder catalogo = new StringBuilder("{\"servicios\":[");
                for (Servicio item : Servicio.values()) {
                    if (catalogo.charAt(catalogo.length() - 1) != '[') catalogo.append(',');
                    catalogo.append("{\"id\":").append(item.id)
                            .append(",\"nombre\":\"").append(esc(item.nombre))
                            .append("\",\"duracionMinutos\":").append(item.duracionMinutos).append('}');
                }
                json(e, 200, catalogo.append("]}").toString()); return;
            }
            if (ruta.equals("/api/turnos") && metodo.equals("GET")) {
                StringBuilder salida = new StringBuilder("{\"turnos\":[");
                boolean primero = true;
                for (Turno turno : servicio.listar()) {
                    if (!primero) salida.append(',');
                    primero = false;
                    salida.append("{\"id\":").append(turno.id())
                            .append(",\"paciente\":\"").append(esc(turno.paciente().nombre()))
                            .append("\",\"telefono\":\"").append(esc(turno.paciente().telefono()))
                            .append("\",\"servicioId\":").append(turno.servicio().id)
                            .append(",\"servicio\":\"").append(esc(turno.servicio().nombre))
                            .append("\",\"fecha\":\"").append(turno.fecha())
                            .append("\",\"hora\":\"").append(turno.hora())
                            .append("\",\"duracionMinutos\":").append(turno.servicio().duracionMinutos)
                            .append(",\"estado\":\"").append(turno.estado()).append("\"}");
                }
                json(e, 200, salida.append("]}").toString()); return;
            }
            if (metodo.equals("POST") && (ruta.equals("/api/turnos") || ruta.equals("/api/turnos/cancelar") || ruta.equals("/api/turnos/reprogramar"))) {
                Map<String, String> datos = leerFormulario(e);
                if (ruta.equals("/api/turnos")) {
                    int servicioId;
                    try { servicioId = Integer.parseInt(datos.getOrDefault("servicioId", "")); }
                    catch (NumberFormatException ex) { throw new ReglaNegocioException(400, "Datos de paciente o servicio inválidos"); }
                    ReservaRequest solicitud = new ReservaRequest(datos.getOrDefault("paciente", ""),
                            datos.getOrDefault("telefono", ""), servicioId,
                            datos.getOrDefault("fecha", ""), datos.getOrDefault("hora", ""));
                    Turno nuevo = servicio.reservar(solicitud);
                    json(e, 201, "{\"ok\":true,\"mensaje\":\"Turno reservado\",\"id\":" + nuevo.id() + "}"); return;
                }
                int id;
                try { id = Integer.parseInt(datos.getOrDefault("id", "")); }
                catch (NumberFormatException ex) { throw new ReglaNegocioException(400, "ID inválido"); }
                if (ruta.equals("/api/turnos/cancelar")) {
                    servicio.cancelar(id);
                    json(e, 200, "{\"ok\":true,\"mensaje\":\"Turno cancelado\"}"); return;
                }
                servicio.reprogramar(id, datos.getOrDefault("fecha", ""), datos.getOrDefault("hora", ""));
                json(e, 200, "{\"ok\":true,\"mensaje\":\"Turno reprogramado\"}"); return;
            }
            if (metodo.equals("GET") && (ruta.equals("/") || ruta.equals("/app.js") || ruta.equals("/style.css"))) {
                servirEstatico(e, ruta); return;
            }
            throw new ReglaNegocioException(404, "Recurso inexistente");
        } catch (ReglaNegocioException ex) {
            json(e, ex.estadoHttp, "{\"ok\":false,\"mensaje\":\"" + esc(ex.getMessage()) + "\"}");
        }
    }

    private Map<String, String> leerFormulario(HttpExchange exchange) throws IOException {
        Map<String, String> resultado = new HashMap<>();
        String cuerpo = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        for (String par : cuerpo.split("&")) {
            if (par.isBlank()) continue;
            String[] partes = par.split("=", 2);
            resultado.put(URLDecoder.decode(partes[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(partes.length > 1 ? partes[1] : "", StandardCharsets.UTF_8));
        }
        return resultado;
    }

    private void servirEstatico(HttpExchange exchange, String ruta) throws IOException {
        String nombre = ruta.equals("/") ? "index.html" : ruta.substring(1);
        Path archivo = Path.of("web", nombre);
        if (!Files.exists(archivo)) throw new ReglaNegocioException(404, "Archivo inexistente");
        byte[] bytes = Files.readAllBytes(archivo);
        exchange.getResponseHeaders().set("Content-Type", nombre.endsWith(".css") ? "text/css; charset=utf-8"
                : nombre.endsWith(".js") ? "application/javascript; charset=utf-8" : "text/html; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (var salida = exchange.getResponseBody()) { salida.write(bytes); }
    }

    private static String esc(String texto) {
        return texto.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }

    private void json(HttpExchange exchange, int estado, String cuerpo) throws IOException {
        byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(estado, bytes.length);
        try (var salida = exchange.getResponseBody()) { salida.write(bytes); }
    }
}
