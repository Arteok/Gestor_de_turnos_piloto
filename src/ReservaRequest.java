/** Introduce Parameter Object: reúne los datos de una solicitud de reserva. */
public record ReservaRequest(String paciente, String telefono, int servicioId,
                             String fecha, String hora) { }
