/** Error de validación que permite responder con un código HTTP explícito. */
public class ReglaNegocioException extends RuntimeException {
    public final int estadoHttp;
    public ReglaNegocioException(int estadoHttp, String mensaje) {
        super(mensaje);
        this.estadoHttp = estadoHttp;
    }
}
