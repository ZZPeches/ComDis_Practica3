import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class InterfazCBServImp extends UnicastRemoteObject implements InterfazCBServ{
    
    private HashMap<String,InterfazCB> clientes;
    private HashMap<String,ArrayList<String>> grupos;

    public InterfazCBServImp() throws RemoteException{
        super();
        clientes = new HashMap<>();
        grupos = new HashMap<>();

        // Grupo 1: Pedro <-> Ana
        grupos.put("Pedro", new ArrayList<>(Arrays.asList("Ana")));
        grupos.put("Ana", new ArrayList<>(Arrays.asList("Pedro")));

        // Grupo 2: Manuel <-> David
        grupos.put("Manuel", new ArrayList<>(Arrays.asList("David")));
        grupos.put("David", new ArrayList<>(Arrays.asList("Manuel")));
    }

    public void registrar(InterfazCB objetoCli,String id) throws RemoteException{

        /*if(!(clientes.containsValue(objetoCli))){
            enviarNuevasConexiones(objetoCli,id);
            clientes.put(id,objetoCli);
            objetoCli.listaClientes(clientes);
        }*/
        if(!(clientes.containsValue(objetoCli))){
            clientes.put(id, objetoCli);
        }
        HashMap<String,InterfazCB> amigosConectados = new HashMap<>();
        if((grupos.containsKey(id))){
            ArrayList<String> amigos = grupos.get(id);
            for (String amigo : amigos){
                if (clientes.containsKey(amigo)){
                    amigosConectados.put(amigo,clientes.get(amigo));
                }
            }
            objetoCli.listaAmigos(amigosConectados);
        }
        enviarAmigosNuevasConexiones(id,objetoCli,amigosConectados);
        

    }

    public synchronized void eliminar(InterfazCB objetoCli,String id) throws RemoteException{

        if(!(clientes.containsValue(objetoCli))){
            System.out.println("Cliente no registrado");
        }/*else{
            clientes.remove(id);
            System.out.println("Cliente eliminado del registro");
            enviarDesconexiones(objetoCli,id);
        }*/else{
            ArrayList<InterfazCB> amigos = new ArrayList<>();
            for(String amigo : grupos.get(id)){
                if(clientes.containsKey(amigo)){
                    amigos.add(clientes.get(amigo));
                }
            }
            clientes.remove(id);
            System.out.println("Cliente " + id + " eliminado del registro");
            enviarDesconexionesAmigos(objetoCli,id,amigos);
        }


    }

    private void enviarDesconexionesAmigos(InterfazCB objetoCli, String id, ArrayList<InterfazCB> amigos) {

        for(InterfazCB cliente : amigos){

            try {
                cliente.notificarDesconexion(id,objetoCli);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        System.out.println("Callbacks completados");
    }

    private void enviarDesconexiones(InterfazCB objetoCli,String id) {
        
        for(InterfazCB cliente : clientes.values()){

            try {
                cliente.notificarDesconexion(id,objetoCli);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        System.out.println("Callbacks completados");

    }

    private synchronized void enviarNuevasConexiones(InterfazCB objetoCli,String id) {
        
        for(InterfazCB cliente : clientes.values()){

            try {
                cliente.notificarNuevaConexion(id,objetoCli);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        System.out.println("Callbacks completados");

    } 

    private void enviarAmigosNuevasConexiones(String id, InterfazCB objetoCli,HashMap<String,InterfazCB> amigosConectados) {
    
        for(InterfazCB cliente : amigosConectados.values()){

            try {
                cliente.notificarNuevaConexion(id,objetoCli);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        System.out.println("Callbacks completados");

    }

}
