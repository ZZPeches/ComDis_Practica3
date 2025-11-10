package controlador;

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
    //? logica de pings de servidor
    public void run() {
        long lastPingLog = 0;
        
        while(true) {
            long currentTime = System.currentTimeMillis();
            boolean shouldPrint = (currentTime - lastPingLog > 5000); // solo se imprime cada 5 segundos para non encher a pantalla
            
            if (shouldPrint) { // fai ping aos clientes para desconectar aos que non respondan
                System.out.print("\rComprobando clientes conectados... ");
                System.out.flush();
            }

            for (String cliente : clientes.keySet()) {
                try {
                    // intenta facer ping a cada cliente
                    clientes.get(cliente).ping();
                } catch(RemoteException e) {
                    try {
                        // elimina os que non consigue
                        System.out.println("\nEliminando cliente inalcanzable: " + cliente);
                        servidor.eliminar(clientes.get(cliente), cliente);
                    } catch(RemoteException ex) {
                        System.out.println("\nError al eliminar cliente: " + cliente);
                    }
                }
            }

            if (shouldPrint) {
                // cada 5 segundos imprime a lista de clientes activos que responden aos pings
                String timestamp = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
                int clientCount = clientes.size();
                System.out.print("\rÚltima comprobación: " + timestamp + " - Clientes activos: " + clientCount + " ");
                System.out.flush();
                lastPingLog = currentTime;
            }

        
            try {
                // fai esta comprobacion detodosos clientes 1 vez por segundo
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // se algo interrumpe ao hilo pode salir do bucle
                break;
            }
        }
    }

}
