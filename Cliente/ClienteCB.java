
import java.io.Console;
import java.rmi.Naming;
import java.util.List;
import java.util.Scanner;

public class ClienteCB {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);
        Console console = System.console(); // para ocultar la contraseña
        try {
            System.out.println("Introduzca el nombre del nodo: ");
            String hostname = sc.nextLine().trim();
            System.out.println("Introduzca el puerto: ");
            int puerto = Integer.parseInt(sc.nextLine().trim());
            String URL = "rmi://" + hostname + ":" + puerto + "/objetoRemoto";

            InterfazCBServ servidor = (InterfazCBServ) Naming.lookup(URL);
            InterfazCBImp objetoCli = new InterfazCBImp(); // cliente remoto

            String nombre = "";
            String clave;
            boolean logueado = false;

            while (!logueado) {
                System.out.println("Nombre de usuario:");
                nombre = sc.nextLine().trim();

                // primero pide nombre, despues comprueba si hay ese nombre y segun eso registra o inicia sesion
                if (!servidor.validarUsuarioExistente(nombre)) {
                    System.out.println("No hay un usuario registrado con ese nombre. ¿Deseas registrarte? (y/n)");
                    String respuesta = sc.nextLine().trim().toLowerCase();
                    if (!respuesta.equals("y")) {
                        continue;
                    }

                    // pedir contraseña de forma oculta (si fue posible iniciar console) para registrar
                    if (console != null) {
                        char[] passwordArray = console.readPassword("Introduce una nueva contraseña: ");
                        clave = new String(passwordArray);
                    } else {
                        System.out.println("Introduce una nueva contraseña:");
                        clave = sc.nextLine().trim();
                    }

                    if (servidor.registrarUsuario(nombre, clave)) {
                        System.out.println("Usuario registrado con éxito. Ahora puedes iniciar sesión.");
                    } else {
                        System.out.println("Error al registrar el usuario.");
                        continue;
                    }
                }

                // pedir contraseña oculta para login
                if (console != null) {
                    char[] passwordArray = console.readPassword("Contraseña: ");
                    clave = new String(passwordArray);
                } else {
                    System.out.println("Contraseña:");
                    clave = sc.nextLine().trim();
                }

                // login
                if (servidor.loginUsuario(nombre, clave, objetoCli)) {
                    System.out.println("Login exitoso. Bienvenido " + nombre + "!");
                    logueado = true;
                } else {
                    System.out.println("Usuario o contraseña incorrectos, intenta de nuevo.");
                }
            }

            // bucle principal de cliente
            String opcion = "";
            while (!opcion.equalsIgnoreCase("d")) {

                System.out.print("Selecciona una opción: ");

                opcion = sc.nextLine().trim().toLowerCase();

                switch (opcion) {
                    case "e": {

                        System.out.print("Escribe el mensaje: ");
                        String texto = sc.nextLine().trim();
                        //objetoCli.enviar(texto);
                        break;
                    }

                    case "a": {
                        System.out.print("Introduce el nombre del usuario al que quieres enviar una solicitud de amistad: ");
                        String amigo = sc.nextLine().trim();

                        if (servidor.enviarSolicitudAmistad(nombre, amigo)) {
                            System.out.println("Solicitud de amistad enviada a " + amigo);
                        } else {
                            System.out.println("No se pudo enviar la solicitud (usuario no encontrado o ya es amigo).");
                        }
                        break;
                    }

                    case "l": {
                        List<String> amigos = servidor.obtenerAmigos(nombre);
                        if (amigos.isEmpty()) {
                            System.out.println("No tienes amigos registrados todavía");
                        } else {
                            System.out.println("Tus amigos:");
                            amigos.forEach(a -> System.out.println(" - " + a));
                        }
                        break;
                    }

                    case "p": {
                        List<String> pendientes = servidor.obtenerSolicitudesPendientes(nombre);
                        if (pendientes.isEmpty()) {
                            System.out.println("No tienes solicitudes pendientes.");
                        } else {
                            System.out.println("Solicitudes pendientes:");
                            for (String remitente : pendientes) {
                                System.out.println(" - " + remitente);
                                System.out.print("¿Deseas aceptar a " + remitente + "? (s/n): ");
                                String respuesta = sc.nextLine().trim().toLowerCase();
                                if (respuesta.equals("s")) {
                                    servidor.aceptarAmistad(remitente, nombre);
                                    System.out.println(":) Ahora eres amigo de " + remitente);
                                } else {
                                    servidor.rechazarAmistad(remitente, nombre);
                                    System.out.println(":( Has rechazado la solicitud de " + remitente);
                                }
                            }
                        }
                        break;
                    }

                    case "d": {
                        System.out.println("Saliendo del sistema...");
                        servidor.eliminar(objetoCli, nombre);
                        break;
                    }

                    default:
                        System.out.println("Opción no válida. Usa: e, a, l, p o d.");
                }
            }

            sc.close();
        } catch (Exception e) {
            System.out.println("Exception en ClienteRMI");
            System.out.println(e.getMessage());
        }
    }
}
