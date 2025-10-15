import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

public class ServidorCB {
    
    public static void main(String[] args){

        Scanner sc = new Scanner(System.in);
        String puerto,URL;

        try{

            System.out.println("Ingrese puerto: ");
            puerto = sc.nextLine().trim();
            int numPuerto = Integer.parseInt(puerto);
            arrancarRegistro(numPuerto);
            InterfazCBServImp objExp = new InterfazCBServImp();
            URL = "rmi://localhost:" + numPuerto + "/objetoRemoto";
            Naming.rebind(URL, objExp);
            System.out.println("Objeto registrado");
            String [] nombres = Naming.list(URL);
            for (int i = 0;i < nombres.length; i++){
                System.out.println(nombres[i]);
            }
            System.out.println("Servidor listo");

            sc.close();

        }catch (Exception e){System.out.println("Exception en main Servidor");e.printStackTrace();}

    }

    private static void arrancarRegistro(int numPuerto) throws RemoteException {
        
        try{

            Registry registro = LocateRegistry.getRegistry(numPuerto);
            registro.list();

        }catch(RemoteException e){

            System.out.println("Creando registro en el puerto" + numPuerto);
            LocateRegistry.createRegistry(numPuerto);
            System.out.println("Registro creado en el puerto " + numPuerto);

        }

    }

}