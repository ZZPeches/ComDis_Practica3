import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface InterfazCBServ extends Remote {

    // registro y sesión
    boolean registrarUsuario(String nombre, String clave) throws RemoteException;

    boolean validarUsuarioExistente(String nombre) throws RemoteException;

    boolean loginUsuario(String nombre, String clave, InterfazCB cli) throws RemoteException;

    // desconexión
    void eliminar(InterfazCB objetoCli, String id) throws RemoteException;

    // solicitudes de amistad
    void enviarSolicitud(String remitente, String destino) throws RemoteException;

    void aceptarSolicitud(String usuario1, String usuario2) throws RemoteException;

    void rechazarSolicitud(String remitente, String destino) throws RemoteException;

    // consulta
    List<String> obtenerAmigosEnLinea(String nombre) throws RemoteException;
}
