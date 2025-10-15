import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;

public class InterfazCBImp extends UnicastRemoteObject implements InterfazCB{

    private HashMap<String,InterfazCB> amigos;

    public InterfazCBImp() throws RemoteException{
        super();
        this.amigos = new HashMap<>();
    }

    public void notificarNuevaConexion(String id, InterfazCB amigo) throws RemoteException{
        
        amigos.put(id, amigo);
        System.out.println("Nuevo cliente conectado: " + id + "|" + amigo);

    }

    public void listaClientes(HashMap<String,InterfazCB> amigos) throws RemoteException{

        this.amigos = amigos;
        for (String amigo : this.amigos.keySet()){
            System.out.println("Amigo: " + amigo);
        }


    }

    public void listaAmigos(HashMap<String,InterfazCB> amigos) throws RemoteException{
        this.amigos = amigos;
        for (String amigo : this.amigos.keySet()){
            System.out.println("Amigo: " + amigo);
        }
    }
    
    public void notificarDesconexion(String id, InterfazCB amigo) throws RemoteException{

        amigos.remove(id);
        System.out.println("Nuevo cliente desconectado: " + id + "|" + amigo);

    }


    public void recibir(String mensaje) throws RemoteException{
        System.out.println(mensaje);
    }

    public void enviar(String mensaje) throws RemoteException{
        for(InterfazCB amigo : this.amigos.values()){
            try{
                amigo.recibir(mensaje);
            }catch(Exception e){System.out.println("Exception en envío");e.printStackTrace();}
        }
    }

    

}
