import java.time.LocalDate;
import java.time.LocalTime;

/** Entidad de dominio; contiene las reglas que dependen de un turno específico. */
public record Turno(int id, Paciente paciente, Servicio servicio,
                    LocalDate fecha, LocalTime hora, EstadoTurno estado) {

    // Move Method: comprobación de solapamiento trasladada desde el controlador.
    public boolean seSolapa(LocalDate otraFecha, LocalTime otroInicio, int otraDuracion) {
        if (estado == EstadoTurno.CANCELADO || !fecha.equals(otraFecha)) return false;
        LocalTime fin = hora.plusMinutes(servicio.duracionMinutos);
        LocalTime otroFin = otroInicio.plusMinutes(otraDuracion);
        return otroInicio.isBefore(fin) && hora.isBefore(otroFin);
    }

    public Turno cancelar() {
        return new Turno(id, paciente, servicio, fecha, hora, EstadoTurno.CANCELADO);
    }

    public Turno reprogramar(LocalDate nuevaFecha, LocalTime nuevaHora) {
        return new Turno(id, paciente, servicio, nuevaFecha, nuevaHora, estado);
    }

    public String lineaPersistencia() {
        return String.join("|", Integer.toString(id), paciente.nombre(), paciente.telefono(),
                Integer.toString(servicio.id), fecha.toString(), hora.toString(), estado.name());
    }

    public static Turno desdeLinea(String linea) {
        String[] c = linea.split("\\|", -1);
        if (c.length != 7) throw new IllegalArgumentException("Registro con columnas incorrectas");
        return new Turno(Integer.parseInt(c[0]), new Paciente(c[1], c[2]), Servicio.porId(Integer.parseInt(c[3])),
                LocalDate.parse(c[4]), LocalTime.parse(c[5]), EstadoTurno.valueOf(c[6]));
    }
}
