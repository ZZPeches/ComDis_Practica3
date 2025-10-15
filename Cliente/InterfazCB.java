import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;

public interface InterfazCB extends Remote{
    
    public void notificarNuevaConexion(String id, InterfazCB amigo) throws RemoteException;
    public void listaClientes(HashMap<String,InterfazCB> amigos) throws RemoteException;
    public void listaAmigos(HashMap<String,InterfazCB> amigos)throws RemoteException;
    public void notificarDesconexion(String id, InterfazCB amigo) throws RemoteException;
    public void enviar(String mensaje)throws RemoteException;
    public void recibir(String mensaje)throws RemoteException;

}
