import com.sun.net.httpserver.HttpServer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Comprueba las respuestas HTTP y la persistencia de la agenda del consultorio. */
public class ContratoHTTP {
    private static int afirmaciones = 0;
    private static final HttpClient cliente = HttpClient.newHttpClient();
    private static String base;

    private static HttpResponse<String> get(String ruta) throws Exception {
        return cliente.send(HttpRequest.newBuilder(URI.create(base + ruta)).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static HttpResponse<String> post(String ruta, String... campos) throws Exception {
        if (campos.length % 2 != 0) throw new IllegalArgumentException("Datos de prueba inválidos");
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < campos.length; i += 2) {
            if (body.length() != 0) body.append('&');
            body.append(URLEncoder.encode(campos[i], StandardCharsets.UTF_8))
                    .append('=').append(URLEncoder.encode(campos[i+1], StandardCharsets.UTF_8));
        }
        return cliente.send(HttpRequest.newBuilder(URI.create(base + ruta))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static void comprobar(String nombre, HttpResponse<String> resultado, int estado, String contiene) {
        if (resultado.statusCode() != estado || !resultado.body().contains(contiene)) {
            throw new AssertionError(nombre + ": esperado HTTP " + estado + " y '" + contiene + "'; obtenido "
                    + resultado.statusCode() + " / " + resultado.body());
        }
        afirmaciones++;
        System.out.println("OK " + String.format("%02d", afirmaciones) + " - " + nombre);
    }

    public static void main(String[] args) throws Exception {
        Path carpetaTemporal = Files.createTempDirectory("tpi-msii-contrato-");
        Path archivo = carpetaTemporal.resolve("turnos.txt");
        HttpServer servidor = null;
        try {
            servidor = App.crearServidor(0, archivo);
            servidor.start();
            base = "http://127.0.0.1:" + servidor.getAddress().getPort();

            comprobar("salud", get("/api/salud"), 200, "\"ok\":true");
            comprobar("interfaz de usuario", get("/"), 200, "Nueva reserva");
            comprobar("CSS", get("/style.css"), 200, "--bg:");
            comprobar("catálogo", get("/api/catalogo"), 200, "Tratamiento podológico");
            comprobar("agenda vacía", get("/api/turnos"), 200, "\"turnos\":[]");
            comprobar("nombre inválido", post("/api/turnos", "paciente","A","telefono","2901123456","servicioId","1","fecha","2099-07-21","hora","10:00"), 400, "Datos de paciente");
            comprobar("teléfono inválido", post("/api/turnos", "paciente","Ana Ejemplo","telefono","aaa","servicioId","1","fecha","2099-07-21","hora","10:00"), 400, "Datos de paciente");
            comprobar("servicio inválido", post("/api/turnos", "paciente","Ana Ejemplo","telefono","2901123456","servicioId","8","fecha","2099-07-21","hora","10:00"), 400, "servicio inválidos");
            comprobar("fecha inválida", post("/api/turnos", "paciente","Ana Ejemplo","telefono","2901123456","servicioId","1","fecha","2099-99-21","hora","10:00"), 400, "Fecha u horario inválidos");
            comprobar("fuera de horario", post("/api/turnos", "paciente","Ana Ejemplo","telefono","2901123456","servicioId","1","fecha","2099-07-21","hora","18:00"), 400, "fuera de la agenda");
            comprobar("reserva tratamiento 60 min", post("/api/turnos", "paciente","Ana Ejemplo","telefono","2901123456","servicioId","2","fecha","2099-07-21","hora","10:00"), 201, "\"id\":1");
            comprobar("rechazo de solapamiento", post("/api/turnos", "paciente","Beto Ejemplo","telefono","2901123457","servicioId","1","fecha","2099-07-21","hora","10:30"), 409, "se superpone");
            comprobar("turno contiguo permitido", post("/api/turnos", "paciente","Beto Ejemplo","telefono","2901123457","servicioId","1","fecha","2099-07-21","hora","11:00"), 201, "\"id\":2");
            comprobar("misma hora en otro día", post("/api/turnos", "paciente","Celia Ejemplo","telefono","2901123458","servicioId","1","fecha","2099-07-22","hora","10:00"), 201, "\"id\":3");
            comprobar("lista con turnos", get("/api/turnos"), 200, "\"paciente\":\"Ana Ejemplo\"");
            comprobar("reprogramación con choque", post("/api/turnos/reprogramar", "id","2","fecha","2099-07-21","hora","10:30"), 409, "se superpone");
            comprobar("reprogramación válida", post("/api/turnos/reprogramar", "id","2","fecha","2099-07-21","hora","11:30"), 200, "Turno reprogramado");
            comprobar("hora actualizada", get("/api/turnos"), 200, "\"hora\":\"11:30\"");
            comprobar("cancelación", post("/api/turnos/cancelar", "id","1"), 200, "Turno cancelado");
            comprobar("cancelación duplicada", post("/api/turnos/cancelar", "id","1"), 409, "ya está cancelado");
            comprobar("liberar horario cancelado", post("/api/turnos", "paciente","Diana Ejemplo","telefono","2901123459","servicioId","2","fecha","2099-07-21","hora","10:00"), 201, "\"id\":4");
            comprobar("no reprogramar cancelado", post("/api/turnos/reprogramar", "id","1","fecha","2099-07-23","hora","12:00"), 409, "ya está cancelado");
            comprobar("ID desconocido", post("/api/turnos/cancelar", "id","999"), 404, "Turno inexistente");
            comprobar("ID malformado", post("/api/turnos/cancelar", "id","abc"), 400, "ID inválido");
            servidor.stop(0);
            servidor = null;

            servidor = App.crearServidor(0, archivo);
            servidor.start();
            base = "http://127.0.0.1:" + servidor.getAddress().getPort();
            comprobar("persistencia tras reinicio", get("/api/turnos"), 200, "\"estado\":\"CANCELADO\"");
            comprobar("se conserva el próximo ID", post("/api/turnos", "paciente","Eva Ejemplo","telefono","2901123460","servicioId","1","fecha","2099-07-23","hora","09:00"), 201, "\"id\":5");
            System.out.println("RESULTADO: " + afirmaciones + "/" + afirmaciones + " comprobaciones aprobadas.");
        } finally {
            if (servidor != null) servidor.stop(0);
            Files.deleteIfExists(archivo);
            Files.deleteIfExists(carpetaTemporal);
        }
    }
}
