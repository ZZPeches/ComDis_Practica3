
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class InterfazCBServImp extends UnicastRemoteObject implements InterfazCBServ {

    private HashMap<String, InterfazCB> clientes;

    private DBManager db; // para base de datos sql

    public InterfazCBServImp() throws RemoteException {
        super();
        clientes = new HashMap<>();
        try {
            db = new DBManager("usuarios.db"); // aqui se guarda la base de datos
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    @Override
    public void registrar(InterfazCB objetoCli, String id) throws RemoteException {

        /*
         * if(!(clientes.containsValue(objetoCli))){
         * enviarNuevasConexiones(objetoCli,id);
         * clientes.put(id,objetoCli);
         * objetoCli.listaClientes(clientes);
         * }
         */
        if (!(clientes.containsValue(objetoCli))) {
            clientes.put(id, objetoCli);
            HashMap<String, InterfazCB> amigosConectados = new HashMap<>();
            List<String> amigos = db.obtenerAmigos(id);
            for (String amigo : amigos) {
                if (clientes.containsKey(amigo)) {
                    amigosConectados.put(amigo, clientes.get(amigo));
                }
            }
            objetoCli.listaAmigosEnLinea(amigosConectados);
            enviarAmigosNuevasConexiones(id, objetoCli, amigosConectados);

        }

    }

    @Override
    public synchronized void eliminar(InterfazCB objetoCli, String id) throws RemoteException {

        if (!(clientes.containsValue(objetoCli))) {
            System.out.println("Cliente no registrado");
        } /*
           * else{
           * clientes.remove(id);
           * System.out.println("Cliente eliminado del registro");
           * enviarDesconexiones(objetoCli,id);
           * }
         */ else {
            ArrayList<InterfazCB> amigos = new ArrayList<>();
            for (String amigo : db.obtenerAmigos(id)) {
                if (clientes.containsKey(amigo)) {
                    amigos.add(clientes.get(amigo));
                }
            }
            clientes.remove(id);
            System.out.println("Cliente " + id + " eliminado del registro");
            enviarDesconexionesAmigos(objetoCli, id, amigos);
        }

    }

    private void enviarDesconexionesAmigos(InterfazCB objetoCli, String id, ArrayList<InterfazCB> amigos) {

        for (InterfazCB cliente : amigos) {

            try {
                cliente.notificarDesconexion(id, objetoCli);
            } catch (RemoteException e) {
                System.err.println(e.getMessage());
            }

        }

        System.out.println("Callbacks completados");
    }

    private void enviarDesconexiones(InterfazCB objetoCli, String id) {

        for (InterfazCB cliente : clientes.values()) {

            try {
                cliente.notificarDesconexion(id, objetoCli);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

        }

        System.out.println("Callbacks completados");

    }

    private synchronized void enviarNuevasConexiones(InterfazCB objetoCli, String id) {

        for (InterfazCB cliente : clientes.values()) {

            try {
                cliente.notificarNuevaConexion(id, objetoCli);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        System.out.println("Callbacks completados");

    }

    private void enviarAmigosNuevasConexiones(String id, InterfazCB objetoCli,
            HashMap<String, InterfazCB> amigosConectados) {

        for (InterfazCB cliente : amigosConectados.values()) {

            try {
                cliente.notificarNuevaConexion(id, objetoCli);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        System.out.println("Callbacks completados");

    }

    @Override
    public boolean registrarUsuario(String nombre, String clave) throws RemoteException {
        return db.registrarUsuario(nombre, clave);
    }

    @Override
    public boolean validarUsuarioExistente(String nombre) throws RemoteException {
        return db.validarUsuarioExistente(nombre);
    }

    @Override
    public boolean loginUsuario(String nombre, String clave, InterfazCB cli) throws RemoteException {
        if (!db.validarLogin(nombre, clave)) {
            return false;
        }

        clientes.put(nombre, cli);

        // notificar a amigos conectados
        List<String> amigos = db.obtenerAmigos(nombre);
        for (String amigo : amigos) {
            if (clientes.containsKey(amigo)) {
                clientes.get(amigo).notificarNuevaConexion(nombre, cli);
            }
        }

        // enviar lista de amigos conectados al cliente que entra
        HashMap<String, InterfazCB> amigosConectados = new HashMap<>();
        for (String amigo : amigos) {
            if (clientes.containsKey(amigo)) {
                amigosConectados.put(amigo, clientes.get(amigo));
            }
        }
        cli.listaAmigosEnLinea(amigosConectados);

        // enviar solicitudes pendientes
        List<String> solicitudes = db.obtenerSolicitudes(nombre);
        cli.recibirSolicitudes(solicitudes);

        return true;
    }

    public List<String> obtenerAmigosEnLinea(String nombre) throws RemoteException {
        List<String> amigos = db.obtenerAmigos(nombre);
        List<String> amigosEnLinea = new ArrayList<>();
        for (String amigo : amigos) {
            if (clientes.containsKey(amigo)) {
                amigosEnLinea.add(amigo);
            }
        }
        return amigosEnLinea;
    }

    @Override
    public boolean enviarSolicitudAmistad(String envia, String recibe) throws RemoteException {
        try {
            if(db.agregarSolicitud(envia, recibe)) {
                System.out.println("[Info] Solicitud de amistad de " + envia + " a " + recibe + " enviada.");
            }
            else{
                InterfazCB objetoCli = clientes.get(envia);
                objetoCli.errorAmigo();
            }
        } catch (Exception e) {
            return false;
        }
        return true;

    }

    @Override
    public java.util.List<String> obtenerAmigos(String nombre) throws RemoteException {
        return db.obtenerAmigos(nombre);
    }

    @Override
    public java.util.List<String> obtenerSolicitudesPendientes(String nombre) throws RemoteException {
        return db.obtenerSolicitudes(nombre);
    }

    @Override
    public boolean aceptarAmistad(String acepta, String recibe) {
        try {
            db.agregarAmigos(acepta, recibe);
            System.out.println("[Info] Solicitud de amistad de " + acepta + " a " + recibe + " aceptada.");
            // Notificar a ambos usuarios si están conectados
            if (clientes.containsKey(acepta)) {
                try {
                    clientes.get(acepta).notificarNuevaConexion(recibe, clientes.get(recibe));
                } catch (RemoteException e) {
                    System.err.println("Error notificando a " + acepta + ": " + e.getMessage());
                }
            }
            if (clientes.containsKey(recibe)) {
                try {
                    clientes.get(recibe).notificarNuevaConexion(acepta, clientes.get(acepta));
                } catch (RemoteException e) {
                    System.err.println("Error notificando a " + recibe + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public boolean rechazarAmistad(String rechaza, String recibe) {
        try {
            db.rechazarSolicitud(recibe, rechaza); // remitente = quien envió la solicitud, destino = quien rechaza
            System.out.println("[Info] Solicitud de amistad de " + recibe + " a " + rechaza + " rechazada.");
            clientes.get(rechaza).recibirSolicitudes(db.obtenerSolicitudes(rechaza));
        } catch (Exception e) {
            return false;
        }
        return true;
    }


    public void mostrarUsuaios(){
        db.mostrarTodosLosUsuarios();
    }

}
