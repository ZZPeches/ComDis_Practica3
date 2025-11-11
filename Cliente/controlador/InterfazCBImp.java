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
    private final ObservableMap<String, InterfazCB> todosUsuariosEnLinea = FXCollections.observableHashMap();
    private final ObservableList<String> solicitudesPendientes = FXCollections.observableArrayList();
    private final ObservableList<String> usuariosConMensajesPendientes = FXCollections.observableArrayList();

    // Buzón para mensajes privados cuando la ventana está cerrada
    private final Map<String, List<String>> buzonMensajesPrivados = new HashMap<>();

    private long lastPingLog = 0;

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

    public ObservableMap<String, InterfazCB> getTodosUsuariosEnLinea() {
        return todosUsuariosEnLinea;
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
        todosUsuariosEnLinea.put(id, amigo);
        if (observador != null) {
            observador.notificarNuevaConexionAmigo(id);
        }
    }

    @Override
    public void notificarNuevaConexion(String id, InterfazCB usuario) {
        todosUsuariosEnLinea.put(id, usuario);

        if (observador != null) {
            observador.notificarNuevaConexion(id);
        }

        System.out.println("Usuario " + id + " conectado. Total usuarios: " + todosUsuariosEnLinea.size());
    }

    @Override
    public void notificarDesconexion(String id, InterfazCB usuario) {

        // Solo notificar al observador si era amigo
        if (observador != null) {
            if (amigosEnLinea.containsKey(id)) {
                observador.notificarDesconexionAmigo(id);
            } else {
                observador.notificarDesconexion(id);
            }
        }
        amigosEnLinea.remove(id);
        todosUsuariosEnLinea.remove(id);

        System.out.println("Usuario " + id + " desconectado. Total usuarios: " + todosUsuariosEnLinea.size());
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
        for (InterfazCB usuario : todosUsuariosEnLinea.values()) {
            if (!usuario.equals(this)) { // No enviarse a sí mismo
                try {
                    usuario.recibir(remitente, mensaje);
                } catch (Exception e) {
                    System.err.println("Error enviando mensaje público a " + usuario);
                }
            }
        }
    }

    // ========================
    // Mensajería a amigos
    // ========================
    @Override
    public void enviarMensajeAmigos(String remitente, String mensaje) {
        for (InterfazCB amigo : amigosEnLinea.values()) {
            if (!amigo.equals(this)) { // No enviarse a sí mismo
                try {
                    amigo.recibirMensajeAmigos(remitente, mensaje);
                } catch (Exception e) {
                    System.err.println("Error enviando mensaje a amigo " + amigo);
                }
            }
        }
    }

    @Override
    public void recibirMensajeAmigos(String remitente, String mensaje) {
        // Validar que el remitente está en la lista de amigos
        if (!amigosEnLinea.containsKey(remitente)) {
            System.err.println("Intento de mensaje de amigos desde usuario no autorizado: " + remitente);
            return;
        }

        if (observador != null) {
            observador.mensajeRecibidoAmigos(remitente, mensaje);
        }
    }

    // ========================
    // Mensajería privada
    // ========================
    @Override
    public void recibirMensajePrivado(String remitente, String mensaje) {
        // Validar que el remitente está en la lista de amigos
        if (!amigosEnLinea.containsKey(remitente)) {
            System.err.println("Intento de mensaje privado desde usuario no autorizado: " + remitente);
            return;
        }

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
        System.out.println("Amigos en línea recibidos: " + amigosEnLinea.size());
    }

    @Override
    public void listaTodosUsuariosEnLinea(HashMap<String, InterfazCB> usuarios) {
        todosUsuariosEnLinea.putAll(usuarios);
        System.out.println("Todos los usuarios en línea recibidos: " + todosUsuariosEnLinea.size());
    }

    @Override
    public void errorAmigo() {
        Platform.runLater(() -> ErrorPopup.show("Error: No puedes enviar una petición de amistad a este usuario."));
    }

    // sobreescribe a mesma liña para non encher terminal
    @Override
    public void ping() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastPingLog > 5000) { // cada 5 segundos
            String timestamp = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
            System.out.print("\rÚltimo ping: " + timestamp + " - Cliente activo");
            System.out.flush();
            lastPingLog = currentTime;
        }
    }
}
