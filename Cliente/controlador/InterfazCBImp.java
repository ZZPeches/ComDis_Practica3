package controlador;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

import gui.ErrorPopup;
import gui.ObservadorChat;

public class InterfazCBImp extends UnicastRemoteObject implements InterfazCB {

    private ObservadorChat observador;

    // Listas de estado
    private final ObservableMap<String, InterfazCB> amigosEnLinea = FXCollections.observableHashMap();
    private final ObservableList<String> solicitudesPendientes = FXCollections.observableArrayList();
    private final ObservableList<String> usuariosConMensajesPendientes = FXCollections.observableArrayList();

    // Buzón para mensajes privados cuando la ventana está cerrada
    private final Map<String, List<String>> buzonMensajesPrivados = new HashMap<>();

    public InterfazCBImp() throws RemoteException {
        super();
    }

    // Getters
    public ObservableList<String> getSolicitudesPendientes() {
        return solicitudesPendientes;
    }

    public ObservableMap<String, InterfazCB> getAmigosEnLinea() {
        return amigosEnLinea;
    }

    public Map<String, List<String>> getBuzonMensajesPrivados() {
        return buzonMensajesPrivados;
    }

    public ObservableList<String> getUsuariosConMensajesPendientes() {
        return usuariosConMensajesPendientes;
    }

    public void setObservadorChat(ObservadorChat observador) {
        this.observador = observador;
    }

    // ========================
    // Eventos de conexión
    // ========================

    @Override
    public void nuevoAmigo(String id, InterfazCB amigo) {
        amigosEnLinea.put(id, amigo);
    }

    @Override
    public void notificarNuevaConexion(String id, InterfazCB amigo) {
        amigosEnLinea.put(id, amigo);
        if (observador != null) {
            observador.notificarConexion(id);
        }
    }

    @Override
    public void notificarDesconexion(String id, InterfazCB amigo) {
        amigosEnLinea.remove(id);
        if (observador != null) {
            observador.notificarDesconexion(id);
        }
    }

    // ========================
    // Mensajería general
    // ========================

    @Override
    public void recibir(String remitente, String mensaje) {
        if (observador != null) {
            observador.mensajeRecibido(remitente, mensaje);
        }
    }

    @Override
    public void enviar(String remitente, String mensaje) {
        for (InterfazCB amigo : amigosEnLinea.values()) {
            try {
                amigo.recibir(remitente, mensaje);
            } catch (Exception e) {
                System.err.println("Error enviando mensaje público");
                e.printStackTrace();
            }
        }
    }

    // ========================
    // Mensajería privada
    // ========================

    @Override
    public void recibirMensajePrivado(String remitente, String mensaje) {
        if (observador != null && observador.mensajeRecibidoPrivado(remitente, mensaje)) {
            return; // ventana abierta, mensaje mostrado
        }

        guardarMensajeEnBuzon(remitente, mensaje);
        if (observador != null) {
            observador.notificarMensajesPendientes(remitente);
        }
    }

    public void enviarMensajePrivado(String remitente, String mensaje, String receptor) {
        InterfazCB amigo = amigosEnLinea.get(receptor);
        if (amigo != null) {
            try {
                amigo.recibirMensajePrivado(remitente, mensaje);
            } catch (Exception e) {
                System.err.println("Error enviando mensaje privado");
                e.printStackTrace();
            }
        }
    }

    private void guardarMensajeEnBuzon(String remitente, String mensaje) {
        buzonMensajesPrivados.computeIfAbsent(remitente, k -> new ArrayList<>()).add(mensaje);

        if (!usuariosConMensajesPendientes.contains(remitente)) {
            Platform.runLater(() -> usuariosConMensajesPendientes.add(remitente));
        }
    }

    public List<String> obtenerMensajesPendientes(String remitente) {
        List<String> mensajes = buzonMensajesPrivados.remove(remitente);

        if (mensajes != null) {
            Platform.runLater(() -> usuariosConMensajesPendientes.remove(remitente));
            return new ArrayList<>(mensajes);
        }
        return Collections.emptyList();
    }

    public void limpiarMensajesPendientes(String remitente) {
        buzonMensajesPrivados.remove(remitente);
        Platform.runLater(() -> usuariosConMensajesPendientes.remove(remitente));
    }

    // ========================
    // Solicitudes de amistad
    // ========================

    @Override
    public void recibirSolicitud(String usuario) {
        solicitudesPendientes.add(usuario);
    }

    public void recibirSolicitudes(List<String> solicitudes) {
        Platform.runLater(() -> solicitudesPendientes.setAll(solicitudes));
    }

    @Override
    public void notificarNuevaSolicitud(String usuario) {
        solicitudesPendientes.add(usuario);
        if (observador != null) {
            observador.notificarSolicitud();
        }
    }

    // ========================
    // Notificaciones varias
    // ========================

    @Override
    public void listaAmigosEnLinea(HashMap<String, InterfazCB> amigos) {
        amigosEnLinea.putAll(amigos);
        amigosEnLinea.keySet().forEach(id -> System.out.println("- " + id));
    }

    @Override
    public void errorAmigo() {
        Platform.runLater(() ->
                ErrorPopup.show("Error: No puedes enviar una petición de amistad a este usuario.")
        );
    }

    @Override
    public void ping() {
        System.out.println("Haciendo Ping...");
    }
}
