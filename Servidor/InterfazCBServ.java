
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface InterfazCBServ extends Remote {

    public void registrar(InterfazCB objetoCli, String id) throws RemoteException;

    public void eliminar(InterfazCB objetoCli, String id) throws RemoteException;

    public boolean validarUsuarioExistente(String nombre) throws RemoteException;

    public boolean registrarUsuario(String nombre, String clave) throws RemoteException;

    public boolean loginUsuario(String nombre, String clave, InterfazCB objetoCli) throws RemoteException;

    public boolean enviarSolicitudAmistad(String envia, String recibe) throws RemoteException;

    public java.util.List<String> obtenerAmigos(String nombre) throws RemoteException;

    public java.util.List<String> obtenerSolicitudesPendientes(String nombre) throws RemoteException;

    public boolean aceptarAmistad(String acepta, String recibe) throws RemoteException;

    public boolean rechazarAmistad(String rechaza, String recibe) throws RemoteException;

}
