package controlador;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class InterfazCBServImp extends UnicastRemoteObject implements InterfazCBServ {

    private ConcurrentHashMap<String, InterfazCB> clientes;
    private DBManager db;

    public InterfazCBServImp() throws RemoteException {
        super();
        clientes = new ConcurrentHashMap<>();
        try {
            db = new DBManager("data/usuarios.db");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    @Override
    public void registrar(InterfazCB objetoCli, String id) throws RemoteException {
        if (!clientes.containsValue(objetoCli)) {
            clientes.put(id, objetoCli);
            
            // Enviar lista de amigos conectados
            HashMap<String, InterfazCB> amigosConectados = new HashMap<>();
            List<String> amigos = db.obtenerAmigos(id);
            for (String amigo : amigos) {
                if (clientes.containsKey(amigo)) {
                    amigosConectados.put(amigo, clientes.get(amigo));
                }
            }
            objetoCli.listaAmigosEnLinea(amigosConectados);
            
            // Enviar list usuarios conectados
            objetoCli.listaTodosUsuariosEnLinea(new HashMap<>(clientes));
            
            // Notificar usuarios sobre el nuevo usuario
            notificarTodosUsuariosNuevaConexion(id, objetoCli);
        }
    }

    @Override
    public synchronized void eliminar(InterfazCB objetoCli, String id) throws RemoteException {
        if (!clientes.containsValue(objetoCli)) {
            System.out.println("Cliente no registrado");
        } else {
            clientes.remove(id);
            System.out.println("Cliente " + id + " eliminado del registro");
            
            // Notificar a todoslos usuarios sobre la desconexión
            notificarTodosUsuariosDesconexion(id, objetoCli);
        }
    }

    private void notificarTodosUsuariosNuevaConexion(String id, InterfazCB nuevoCliente) {
        for (String usuario : clientes.keySet()) {
            InterfazCB cliente = clientes.get(usuario);
                try {
                    cliente.notificarNuevaConexion(id, nuevoCliente);
                    List<String> amigos = db.obtenerAmigos(usuario);

                    if (amigos.contains(id)){
                        System.out.println("Además, es su amigo! añadiendo a su lista de amigos en línea");
                        cliente.nuevoAmigo(id, nuevoCliente);
                    }
                } catch (RemoteException e) {
                    System.err.println("Error notificando conexión a " + usuario);
                }
        }
        System.out.println("Notificada conexión de " + id + " a todos los usuarios");
    }

    private void notificarTodosUsuariosDesconexion(String id, InterfazCB clienteDesconectado) {
        for (String usuario : clientes.keySet()) {
            InterfazCB cliente = clientes.get(usuario);
            try {
                cliente.notificarDesconexion(id, clienteDesconectado);
            } catch (RemoteException e) {
                System.err.println("Error notificando desconexión a " + usuario);
            }
        }
        System.out.println("Notificada desconexión de " + id + " atodoslos usuarios");
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

        // Registrar el nuevo cliente
        clientes.put(nombre, cli);

        // Enviar lista de amigos conectados
        List<String> amigos = db.obtenerAmigos(nombre);
        HashMap<String, InterfazCB> amigosConectados = new HashMap<>();
        for (String amigo : amigos) {
            if (clientes.containsKey(amigo)) {
                amigosConectados.put(amigo, clientes.get(amigo));
            }
        }
        cli.listaAmigosEnLinea(amigosConectados);
        
        // Enviar lista de todos los usuarios conectados
        cli.listaTodosUsuariosEnLinea(new HashMap<>(clientes));

        // Notificar a todos los usuarios existentes sobre el nuevo usuario
        notificarTodosUsuariosNuevaConexion(nombre, cli);

        // Enviar solicitudes pendientes
        List<String> solicitudes = db.obtenerSolicitudes(nombre);
        cli.recibirSolicitudes(solicitudes);

        System.out.println("Usuario " + nombre + " logueado correctamente. Total usuarios: " + clientes.size());
        return true;
    }

    @Override
    public boolean enviarSolicitudAmistad(String envia, String recibe, String passwd) throws RemoteException {
        try {
            InterfazCB objetoCli = clientes.get(envia);
            if (db.validarLogin(envia, passwd)) {
                if (db.agregarSolicitud(envia, recibe)) {
                    System.out.println("[Info] Solicitud de amistad de " + envia + " a " + recibe + " enviada.");
                    if (clientes.containsKey(recibe)) {
                        InterfazCB receptor = clientes.get(recibe);
                        receptor.notificarNuevaSolicitud(envia);
                    }
                } else {
                    objetoCli.errorAmigo();
                }
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
            System.out.println("[Info] Solicitud de amistad de " + recibe + " a " + acepta + " aceptada.");
            
            // Actualizar listas de amigos para ambos usuarios si están conectados
            if (clientes.containsKey(acepta) && clientes.containsKey(recibe)) {
                InterfazCB clienteAcepta = clientes.get(acepta);
                InterfazCB clienteRecibe = clientes.get(recibe);
                
                // Agregar como amigos mutuamente
                clienteAcepta.nuevoAmigo(recibe, clienteRecibe);
                clienteRecibe.nuevoAmigo(acepta, clienteAcepta);
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public boolean rechazarAmistad(String rechaza, String recibe) {
        try {
            db.rechazarSolicitud(recibe, rechaza);
            System.out.println("[Info] Solicitud de amistad de " + recibe + " a " + rechaza + " rechazada.");
            if (clientes.containsKey(rechaza)) {
                clientes.get(rechaza).recibirSolicitudes(db.obtenerSolicitudes(rechaza));
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public ConcurrentHashMap<String, InterfazCB> getClientes() {
        return clientes;
    }
}