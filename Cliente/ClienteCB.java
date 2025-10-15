import java.rmi.Naming;
import java.util.Scanner;

public class ClienteCB {
    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        try {
            System.out.println("Introduzca el nombre del nodo: ");
            String hostname = sc.nextLine().trim();
            System.out.println("Introduzca el puerto: ");
            int puerto = Integer.parseInt(sc.nextLine().trim());
            String URL = "rmi://" + hostname + ":" + puerto + "/objetoRemoto";

            InterfazCBServ servidor = (InterfazCBServ) Naming.lookup(URL);
            InterfazCBImp objetoCli = new InterfazCBImp(); // cliente remoto

            String nombre="";
            String clave;
            boolean logueado = false;

            while (!logueado) {
                System.out.println("Nombre de usuario:");
                nombre = sc.nextLine().trim();
                System.out.println("Contraseña:");
                clave = sc.nextLine().trim();

                if (!servidor.validarUsuarioExistente(nombre)) {
                    // registrar nuevo usuario
                    if (servidor.registrarUsuario(nombre, clave)) {
                        System.out.println("Usuario registrado con éxito.");
                    } else {
                        System.out.println("Error al registrar el usuario, prueba otro nombre.");
                        continue;
                    }
                }

                // intentar login
                if (servidor.loginUsuario(nombre, clave, objetoCli)) {
                    System.out.println("Login exitoso. Bienvenido " + nombre + "!");
                    logueado = true;
                } else {
                    System.out.println("Usuario o contraseña incorrectos, intenta de nuevo.");
                }
            }

            // ahora se puede usar el cliente
            String opcion = "";
            while (!opcion.equals("d")) {
                System.out.println("Qué deseas hacer? (e=enviar mensaje, d=salir)");
                opcion = sc.nextLine().trim();
                switch (opcion) {
                    case "e":
                        objetoCli.enviar("Hola, soy " + nombre);
                        break;
                    case "d":
                        System.out.println("Saliendo...");
                        servidor.eliminar(objetoCli, nombre);
                        break;
                    default:
                        System.out.println("Opciones válidas: e (enviar mensaje), d (salir)");
                }
            }

            sc.close();
        } catch (Exception e) {
            System.out.println("Exception en ClienteRMI");
            e.printStackTrace();
        }
    }
}
