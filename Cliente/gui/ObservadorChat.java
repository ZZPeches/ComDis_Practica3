package gui;

public interface ObservadorChat {

    public void mensajeRecibido(String remitente, String mensaje);

    public void mensajeRecibidoAmigos(String remitente, String mensaje);

    public boolean mensajeRecibidoPrivado(String remitente, String mensaje);

    public void notificarMensajesPendientes(String remitente);

    public void notificarNuevaConexion(String nombre);

    public void notificarDesconexion(String nombre);

    public void notificarNuevaConexionAmigo(String nombre);

    public void notificarDesconexionAmigo(String nombre);

    public void notificarSolicitud();
}
