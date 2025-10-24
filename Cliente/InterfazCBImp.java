import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import javafx.application.Platform;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class InterfazCBImp extends UnicastRemoteObject implements InterfazCB {
    private ObservableMap<String, InterfazCB> amigosEnLinea = FXCollections.observableHashMap();
    private ObservableList<String> solicitudesPendientes = FXCollections.observableArrayList();

    private HashMap<String, List<String>> buzonMensajesPrivados = new HashMap<>();
    private ObservableList<String> usuariosConMensajesPendientes = FXCollections.observableArrayList();

    public ObservableList<String> getSolicitudesPendientes() {
        return solicitudesPendientes;
    }

    public ObservableMap<String, InterfazCB> getAmigosEnLinea() {
        return amigosEnLinea;
    }

    public HashMap<String, List<String>> getBuzonMensajesPrivados() {
        return buzonMensajesPrivados;
    }

    public ObservableList<String> getUsuariosConMensajesPendientes() {
        return usuariosConMensajesPendientes;
    }

    private ObservadorChat observador;

    public InterfazCBImp() throws RemoteException {
        super();
    }

    public void setObservadorChat(ObservadorChat observador) {
        this.observador = observador;
    }

    @Override
    public void nuevoAmigo(String id, InterfazCB amigo) throws RemoteException {
        amigosEnLinea.put(id, amigo);
    }

    @Override
    public void notificarNuevaConexion(String id, InterfazCB amigo) throws RemoteException {
        amigosEnLinea.put(id, amigo);
        this.observador.notificarConexion(id);
    }

    @Override
    public void notificarDesconexion(String id, InterfazCB amigo) throws RemoteException {
        amigosEnLinea.remove(id);
        this.observador.notificarDesconexion(id);
    }

    @Override
    public void recibir(String remitente, String mensaje) throws RemoteException {
        if (observador != null) {
            observador.mensajeRecibido(remitente, mensaje);
        }
    }

    // logica mensaje privado:
    // caso 1: si user1 envia "hola" a user2 pero user2 no está en linea --> error, se pierde el mensaje
    // caso 2: si user1 envia "hola" a user2 pero user2 no tiene la ventana abierta --> se guarda en un buzon de entrada hasta que user2 abra la ventana
    // caso 3: si user1 envia "hola" a user2 que tiene la ventana abierta --> aparece en la ventana
    public void recibirMensajePrivado(String remitente, String mensaje) throws RemoteException {
        if (observador != null) {
            if(!observador.mensajeRecibidoPrivado(remitente, mensaje)){ // si el observador no ha podido mostrar el mensaje (caso 2)
                // gardar el mensaje en el buzón del remitente
                guardarMensajeEnBuzon(remitente, mensaje);
                // notificar al observador que hay mensajes pendientes
                observador.notificarMensajesPendientes(remitente);
            }
        } else {
            // si no hay observador, también guardar en el buzón
            guardarMensajeEnBuzon(remitente, mensaje);
        }
    }

    private void guardarMensajeEnBuzon(String remitente, String mensaje) {
        if (!buzonMensajesPrivados.containsKey(remitente)) {
            buzonMensajesPrivados.put(remitente, new ArrayList<>());
            // Añadir a la lista de usuarios con mensajes pendientes
            if (!usuariosConMensajesPendientes.contains(remitente)) {
                Platform.runLater(() -> usuariosConMensajesPendientes.add(remitente));
            }
        }
        buzonMensajesPrivados.get(remitente).add(mensaje);
    }

    // metodo para recuperar mensajes pendientes de un usuario específico
    public List<String> obtenerMensajesPendientes(String remitente) {
        List<String> mensajes = buzonMensajesPrivados.get(remitente);
        if (mensajes != null) {
            List<String> mensajesCopia = new ArrayList<>(mensajes);
            mensajes.clear(); // Limpiar los mensajes ya recuperados
            // Remover de la lista de usuarios con mensajes pendientes
            Platform.runLater(() -> usuariosConMensajesPendientes.remove(remitente));
            return mensajesCopia;
        }
        return new ArrayList<>();
    }

    public void limpiarMensajesPendientes(String remitente) {
        buzonMensajesPrivados.remove(remitente);
        Platform.runLater(() -> usuariosConMensajesPendientes.remove(remitente));
    }

    @Override
    public void enviar(String remitente, String mensaje) throws RemoteException {
        for (InterfazCB amigo : this.amigosEnLinea.values()) {
            try {
                amigo.recibir(remitente, mensaje);
            } catch (Exception e) {
                System.out.println("Exception en envío");
                e.printStackTrace();
            }
        }
    }

    public void enviarMensajePrivado(String remitente, String mensaje, String receptor) throws RemoteException {
        try {
            InterfazCB amigo = amigosEnLinea.get(receptor);
            if (amigo != null) {
                amigo.recibirMensajePrivado(remitente, mensaje);
            }
        } catch (Exception e) {
            System.out.println("Exception en envío privado");
            e.printStackTrace();
        }
    }

    @Override
    public void recibirSolicitud(String mensaje) throws RemoteException {
        System.out.println("Has recibido una solicitud de amistad de " + mensaje);
        this.solicitudesPendientes.add(mensaje);
    }

    public void recibirSolicitudes(List<String> solicitudesPendientes) throws RemoteException {
        Platform.runLater(() -> {
            this.solicitudesPendientes.setAll(solicitudesPendientes);
        });
    }

    @Override
    public void listaAmigosEnLinea(HashMap<String, InterfazCB> amigosConectados) throws RemoteException {
        amigosEnLinea.putAll(amigosConectados);

        System.out.println("Amigos en línea:");
        for (String id : amigosEnLinea.keySet()) {
            System.out.println("- " + id);
        }
    }

    @Override
    public void errorAmigo() {
        javafx.application.Platform.runLater(() ->
                ErrorPopup.show("Error: No puedes enviar una petición de amistad a este usuario.")
        );
    }

    @Override
    public void notificarNuevaSolicitud(String envia){
        this.solicitudesPendientes.add(envia);
        this.observador.notificarSolicitud();
    }
}