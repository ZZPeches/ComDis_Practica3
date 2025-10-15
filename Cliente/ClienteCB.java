import java.rmi.Naming;
import java.util.Scanner;

public class ClienteCB {
    public static void main(String[] args){

        Scanner sc = new Scanner(System.in);
        String hostname,URL;
        int puerto;

        try{

            System.out.println("Nombre del cliente:");
            String nombre = sc.nextLine().trim();
            System.out.println("Introduzca el nombre del nodo: ");
            hostname = sc.nextLine().trim();
            System.out.println("Introduzca el nombre del puerto: ");
            puerto = sc.nextInt();
            URL = "rmi://" + hostname + ":" + puerto + "/objetoRemoto";
            InterfazCBServ objetoServ = (InterfazCBServ) Naming.lookup(URL);
            System.out.println("Creando objeto cliente...");
            InterfazCB objetoCli = new InterfazCBImp();
            System.out.println("Registrando al nuevo cliente...");
            objetoServ.registrar(objetoCli,nombre);
            String opcion = "";
            while(opcion != "d"){
                System.out.println("Que deseas hacer?");
                opcion = sc.nextLine().trim();
                switch (opcion) {
                    case "e":
                        objetoCli.enviar("hola soy " + nombre);
                        break;
                    
                    case "d":
                        break;
                    default:
                        System.out.println("Opciones válidas: e (enviar mensaje)");
                        break;
                }
            }
            sc.close();

        }catch(Exception e){System.out.println("Exception en ClienteRMI");e.printStackTrace();}

    }

}