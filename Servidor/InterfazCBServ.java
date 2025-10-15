import java.rmi.Remote;
import java.rmi.RemoteException;

public interface InterfazCBServ extends Remote{
    
    public void registrar(InterfazCB objetoCli,String id) throws RemoteException;
    public void eliminar(InterfazCB objetoCli,String id) throws RemoteException;

}
