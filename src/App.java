import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;

/** Composition Root: conecta repositorio, lógica y controlador web. */
public class App {
    public static HttpServer crearServidor(int puerto, Path archivo) throws IOException {
        TurnoRepository repositorio = new ArchivoTurnoRepository(archivo);
        TurnoService servicio = new TurnoService(repositorio);
        ApiController controlador = new ApiController(servicio);
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", puerto), 0);
        servidor.createContext("/", controlador::atender);
        return servidor;
    }

    public static void main(String[] args) throws Exception {
        int puerto = Integer.parseInt(System.getProperty("puerto", "8080"));
        Path archivo = Path.of(System.getProperty("datos", "data/turnos.txt"));
        HttpServer servidor = crearServidor(puerto, archivo);
        servidor.start();
        System.out.println("Gestor de turnos piloto corregido: http://127.0.0.1:" + servidor.getAddress().getPort());
    }
}
