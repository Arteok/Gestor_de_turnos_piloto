import java.io.IOException;
import java.util.List;

/** DIP: contrato mínimo entre los casos de uso y cualquier implementación de persistencia. */
public interface TurnoRepository {
    List<Turno> todos();
    int proximoId();
    void guardarNuevo(Turno turno) throws IOException;
    void actualizar(Turno turno) throws IOException;
}
