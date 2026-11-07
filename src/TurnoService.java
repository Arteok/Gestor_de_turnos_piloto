import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/** SRP: centraliza los casos de uso sin conocer HTTP ni HTML. */
public class TurnoService {
    private static final LocalTime INICIO_ATENCION = LocalTime.of(9, 0);
    private static final LocalTime FIN_ATENCION = LocalTime.of(18, 0);
    private final TurnoRepository repositorio;

    public TurnoService(TurnoRepository repositorio) { this.repositorio = repositorio; }

    public List<Turno> listar() { return repositorio.todos(); }

    public Turno reservar(ReservaRequest solicitud) throws IOException {
        Servicio servicio = validarDatos(solicitud);
        LocalDate fecha = leerFecha(solicitud.fecha());
        LocalTime hora = leerHora(solicitud.hora());
        validarDisponibilidad(0, fecha, hora, servicio.duracionMinutos);
        Turno turno = new Turno(repositorio.proximoId(), new Paciente(solicitud.paciente().trim(),
                solicitud.telefono().trim()), servicio, fecha, hora, EstadoTurno.PENDIENTE);
        repositorio.guardarNuevo(turno);
        return turno;
    }

    public void cancelar(int id) throws IOException {
        Turno turno = buscarActivo(id);
        repositorio.actualizar(turno.cancelar());
    }

    public void reprogramar(int id, String nuevaFecha, String nuevaHora) throws IOException {
        Turno turno = buscarActivo(id);
        LocalDate fecha = leerFecha(nuevaFecha);
        LocalTime hora = leerHora(nuevaHora);
        validarDisponibilidad(id, fecha, hora, turno.servicio().duracionMinutos);
        repositorio.actualizar(turno.reprogramar(fecha, hora));
    }

    // Extract Method: se evita duplicar este control entre reservar y reprogramar.
    private void validarDisponibilidad(int idExcluido, LocalDate fecha, LocalTime hora, int duracion) {
        if (hora.getMinute() % 30 != 0 || hora.getSecond() != 0
                || hora.isBefore(INICIO_ATENCION) || hora.plusMinutes(duracion).isAfter(FIN_ATENCION)) {
            throw new ReglaNegocioException(400, "Fecha u horario fuera de la agenda");
        }
        for (Turno existente : repositorio.todos()) {
            if (existente.id() != idExcluido && existente.seSolapa(fecha, hora, duracion)) {
                throw new ReglaNegocioException(409, "El horario se superpone con otro turno");
            }
        }
    }

    private Servicio validarDatos(ReservaRequest solicitud) {
        String nombre = solicitud.paciente().trim();
        String telefono = solicitud.telefono().trim();
        if (nombre.length() < 2 || nombre.length() > 80 || nombre.contains("|") || nombre.contains("\n")
                || nombre.contains("\r") || !telefono.matches("[0-9]{6,15}")) {
            throw new ReglaNegocioException(400, "Datos de paciente o servicio inválidos");
        }
        return Servicio.porId(solicitud.servicioId());
    }

    private Turno buscarActivo(int id) {
        Turno turno = repositorio.todos().stream().filter(t -> t.id() == id).findFirst()
                .orElseThrow(() -> new ReglaNegocioException(404, "Turno inexistente"));
        if (turno.estado() == EstadoTurno.CANCELADO) {
            throw new ReglaNegocioException(409, "El turno ya está cancelado");
        }
        return turno;
    }

    private LocalDate leerFecha(String valor) {
        try { return LocalDate.parse(valor); }
        catch (DateTimeParseException ex) { throw new ReglaNegocioException(400, "Fecha u horario inválidos"); }
    }

    private LocalTime leerHora(String valor) {
        try { return LocalTime.parse(valor); }
        catch (DateTimeParseException ex) { throw new ReglaNegocioException(400, "Fecha u horario inválidos"); }
    }
}
