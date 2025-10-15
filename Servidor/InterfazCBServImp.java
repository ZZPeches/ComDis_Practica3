import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class InterfazCBServImp extends UnicastRemoteObject implements InterfazCBServ {

    private HashMap<String, InterfazCB> clientes;
    private HashMap<String, ArrayList<String>> grupos;

    private DBManager db; // para base de datos sql

    public InterfazCBServImp() throws RemoteException {
        super();
        clientes = new HashMap<>();
        try {
            db = new DBManager("usuarios.db"); // aqui se guarda la base de datos
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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
        }
        HashMap<String, InterfazCB> amigosConectados = new HashMap<>();
        if ((grupos.containsKey(id))) {
            ArrayList<String> amigos = grupos.get(id);
            for (String amigo : amigos) {
                if (clientes.containsKey(amigo)) {
                    amigosConectados.put(amigo, clientes.get(amigo));
                }
            }
            objetoCli.listaAmigos(amigosConectados);
        }
        enviarAmigosNuevasConexiones(id, objetoCli, amigosConectados);

    }

    public synchronized void eliminar(InterfazCB objetoCli, String id) throws RemoteException {

        if (!(clientes.containsValue(objetoCli))) {
            System.out.println("Cliente no registrado");
        } /*
           * else{
           * clientes.remove(id);
           * System.out.println("Cliente eliminado del registro");
           * enviarDesconexiones(objetoCli,id);
           * }
           */else {
            ArrayList<InterfazCB> amigos = new ArrayList<>();
            for (String amigo : grupos.get(id)) {
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
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        System.out.println("Callbacks completados");
    }

    private void enviarDesconexiones(InterfazCB objetoCli, String id) {

        for (InterfazCB cliente : clientes.values()) {

            try {
                cliente.notificarDesconexion(id, objetoCli);
            } catch (Exception e) {
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

    public boolean registrarUsuario(String nombre, String clave) throws RemoteException {
        return db.registrarUsuario(nombre, clave);
    }

    public boolean loginUsuario(String nombre, String clave, InterfazCB cli) throws RemoteException {
        if (!db.validarLogin(nombre, clave))
            return false;

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
        cli.listaAmigos(amigosConectados);

        // enviar solicitudes pendientes
        List<String> solicitudes = db.obtenerSolicitudes(nombre);
        for (String remitente : solicitudes) {
            cli.recibirSolicitud(remitente);
        }

        return true;
    }

    public void enviarSolicitud(String remitente, String destino) throws RemoteException {
        db.agregarSolicitud(remitente, destino);

        // está conectado --> enviar aviso
        if (clientes.containsKey(destino)) {
            clientes.get(destino).recibirSolicitud(remitente);
        }
    }

    public void rechazarSolicitud(String remitente, String destino) throws RemoteException {
        db.rechazarSolicitud(remitente, destino);
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




}
