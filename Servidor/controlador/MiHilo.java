package controlador;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentHashMap;

public class MiHilo extends Thread{
    
    private ConcurrentHashMap<String, InterfazCB> clientes;
    private InterfazCBServImp servidor;

    public MiHilo(InterfazCBServImp servidor){

        this.servidor = servidor;
        this.clientes = servidor.getClientes();

    }
    
    @Override
    public void run(){

        while(true){

            for (String cliente : clientes.keySet()){

                try{
                    System.out.println(cliente);
                    clientes.get(cliente).ping();
                }catch(RemoteException e){
                    try{
                        System.out.println("Eliminando cliente inacalzable");
                        servidor.eliminar(clientes.get(cliente), cliente);
                    }catch(RemoteException ex){
                        System.out.println("Algún cliente no responde");
                    }
                }

            }
        }

    }

}
