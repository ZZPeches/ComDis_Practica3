
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;

public interface InterfazCB extends Remote {

    public void notificarNuevaConexion(String id, InterfazCB amigo) throws RemoteException;

    public void notificarDesconexion(String id, InterfazCB amigo) throws RemoteException;

    public void enviar(String mensaje) throws RemoteException;

    public void recibir(String mensaje) throws RemoteException;

    public void recibirSolicitud(String mensaje) throws RemoteException;

    public void recibirSolicitudes(List<String> solicitudesPendientes) throws RemoteException;

    public void nuevoAmigo(String id, InterfazCB amigo) throws RemoteException;

    public void listaAmigosEnLinea(HashMap<String, InterfazCB> amigosEnLinea) throws RemoteException;

}
