public interface ObservadorChat {
    void mensajeRecibido(String remitente, String mensaje);
    void notificarConexion(String nombre);
    void notificarDesconexion(String nombre);
    void notificarSolicitud();

}