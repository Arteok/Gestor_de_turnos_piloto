/** Catálogo tipado de servicios. Evita IDs y duraciones mágicas dispersas. */
public enum Servicio {
    CONSULTA(1, "Consulta podológica", 30),
    TRATAMIENTO(2, "Tratamiento podológico", 60);

    public final int id;
    public final String nombre;
    public final int duracionMinutos;

    Servicio(int id, String nombre, int duracionMinutos) {
        this.id = id;
        this.nombre = nombre;
        this.duracionMinutos = duracionMinutos;
    }

    public static Servicio porId(int id) {
        for (Servicio servicio : values()) if (servicio.id == id) return servicio;
        throw new ReglaNegocioException(400, "Datos de paciente o servicio inválidos");
    }
}
