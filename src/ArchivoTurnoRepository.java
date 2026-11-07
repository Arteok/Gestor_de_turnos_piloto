import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/** Extract Class: separa la lectura/escritura del sistema de archivos de la lógica de negocio. */
public class ArchivoTurnoRepository implements TurnoRepository {
    private final Path archivo;
    private final List<Turno> turnos = new ArrayList<>();

    public ArchivoTurnoRepository(Path archivo) throws IOException {
        this.archivo = archivo;
        if (Files.exists(archivo)) {
            int lineaNumero = 0;
            for (String linea : Files.readAllLines(archivo, StandardCharsets.UTF_8)) {
                lineaNumero++;
                if (linea.isBlank()) continue;
                try { turnos.add(Turno.desdeLinea(linea)); }
                catch (RuntimeException ex) {
                    throw new IOException("Datos corruptos en línea " + lineaNumero + " de " + archivo, ex);
                }
            }
        }
    }

    public List<Turno> todos() { return List.copyOf(turnos); }

    public int proximoId() {
        return turnos.stream().mapToInt(Turno::id).max().orElse(0) + 1;
    }

    public void guardarNuevo(Turno turno) throws IOException {
        turnos.add(turno);
        try { persistir(); }
        catch (IOException ex) { turnos.remove(turno); throw ex; }
    }

    public void actualizar(Turno actualizado) throws IOException {
        for (int i = 0; i < turnos.size(); i++) {
            Turno anterior = turnos.get(i);
            if (anterior.id() == actualizado.id()) {
                turnos.set(i, actualizado);
                try { persistir(); }
                catch (IOException ex) { turnos.set(i, anterior); throw ex; }
                return;
            }
        }
        throw new IllegalStateException("No existe turno para actualizar");
    }

    private void persistir() throws IOException {
        Path carpeta = archivo.toAbsolutePath().getParent();
        Files.createDirectories(carpeta);
        Path temporal = Files.createTempFile(carpeta, "turnos-", ".tmp");
        try {
            List<String> lineas = turnos.stream().map(Turno::lineaPersistencia).toList();
            Files.write(temporal, lineas, StandardCharsets.UTF_8);
            try { Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally { Files.deleteIfExists(temporal); }
    }
}
