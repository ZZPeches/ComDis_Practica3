import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import javafx.application.Platform;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import java.util.HashMap;
import java.util.List;

public class InterfazCBImp extends UnicastRemoteObject implements InterfazCB {
    private ObservableMap<String, InterfazCB> amigosEnLinea = FXCollections.observableHashMap();
    private ObservableList<String> solicitudesPendientes = FXCollections.observableArrayList();

    public ObservableList<String> getSolicitudesPendientes() {
        return solicitudesPendientes;
    }

    public ObservableMap<String, InterfazCB> getAmigosEnLinea() {
        return amigosEnLinea;
    }

    public InterfazCBImp() throws RemoteException {
        super();
    }

    @Override
    public void nuevoAmigo(String id, InterfazCB amigo) throws RemoteException {
        amigosEnLinea.put(id, amigo);
    }

    @Override
    public void notificarNuevaConexion(String id, InterfazCB amigo) throws RemoteException {
        amigosEnLinea.put(id, amigo);
        System.out.println("Tu amigo " + id + " está en línea.");
    }

    @Override
    public void notificarDesconexion(String id, InterfazCB amigo) throws RemoteException {
        amigosEnLinea.remove(id);
        System.out.println("Tu amigo " + id + " se ha desconectado.");
    }

    @Override
    public void recibir(String mensaje) throws RemoteException {
        System.out.println(mensaje);
    }

    @Override
    public void enviar(String mensaje) throws RemoteException {
        for (InterfazCB amigo : this.amigosEnLinea.values()) {
            try {
                amigo.recibir(mensaje);
            } catch (Exception e) {
                System.out.println("Exception en envío");
                e.printStackTrace();
            }
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




}
