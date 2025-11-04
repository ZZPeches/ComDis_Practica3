package gui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.control.PasswordField;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.rmi.RemoteException;
import java.util.List;

import controlador.InterfazCB;
import controlador.InterfazCBImp;
import controlador.InterfazCBServ;

public class VentanaUsuario implements ObservadorChat {

    private Stage stage;
    private InterfazCBServ servidor;
    private InterfazCBImp cliente;
    private String nombreUser;

    private ObservableMap<String, InterfazCB> amigos;
    private ScrollPane chatScrollPane;
    private VBox chatLogContainer;
    private TextFlow chatLog;

    private ListView<String> listaSolicitudes;
    private ListView<String> listaAmistades;
    private Button btnChatPrivado;

    private VentanaChatPrivado ventanaChatActual; // solo una a la vez

    // ------------------------------------------------------------
    // CONSTRUCTOR
    // ------------------------------------------------------------
    public VentanaUsuario(Stage stage, InterfazCBServ servidor, InterfazCBImp cliente, String nombreUser) {
        this.stage = stage;
        this.servidor = servidor;
        this.nombreUser = nombreUser;
        this.cliente = cliente;

        amigos = cliente.getAmigosEnLinea();
        cliente.setObservadorChat(this);
    }

    // ------------------------------------------------------------
    // MÉTODO PRINCIPAL DE INTERFAZ
    // ------------------------------------------------------------
    public void mostrar() {

        // ---------- Popup para agregar amigo ----------
        Stage popup = new Stage();
        TextField tfNombreNuevoAmigo = new TextField();
        tfNombreNuevoAmigo.setPromptText("Nuevo Amigo...");
        PasswordField passwdField = new PasswordField();
        passwdField.setPromptText("Contraseña...");

        Button btnAgregar = new Button("Agregar");
        btnAgregar.setOnAction(e -> {
            servidor.enviarSolicitudAmistad(nombreUser, tfNombreNuevoAmigo.getText().trim(), passwdField.getText().trim());
            popup.hide();
        });

        VBox contenido = new VBox(12, tfNombreNuevoAmigo, passwdField, btnAgregar);
        contenido.setPadding(new Insets(15));
        Scene popupScene = new Scene(contenido, 200, 150);
        popup.setScene(popupScene);
        popup.initOwner(stage);
        popup.setTitle("Ventana Agregar Amigo");
        popup.initModality(Modality.WINDOW_MODAL);

        // ---------- Menú ----------
        MenuBar barraMenu = new MenuBar();
        Menu menuSolicitudes = new Menu("Solicitudes");
        MenuItem agregar = new MenuItem("Agregar");

        agregar.setOnAction(e -> popup.show());
        menuSolicitudes.getItems().addAll(agregar);
        barraMenu.getMenus().addAll(menuSolicitudes);

        // ---------- Listas ----------
        ObservableList<String> lista1Items = FXCollections.observableArrayList();
        actualizarListaAmigosConNotificaciones(lista1Items);

        amigos.addListener((MapChangeListener.Change<? extends String, ? extends InterfazCB> change) ->
                Platform.runLater(() -> actualizarListaAmigosConNotificaciones(lista1Items)));

        cliente.getUsuariosConMensajesPendientes().addListener(
                (javafx.collections.ListChangeListener.Change<? extends String> change) ->
                        Platform.runLater(() -> actualizarListaAmigosConNotificaciones(lista1Items))
        );

        listaAmistades = new ListView<>(lista1Items);
        listaSolicitudes = new ListView<>(cliente.getSolicitudesPendientes());

        Button btnAceptar = new Button("Aceptar");
        Button btnRechazar = new Button("Rechazar");
        btnChatPrivado = new Button("Chat Privado");

        btnAceptar.setVisible(false);
        btnRechazar.setVisible(false);
        btnChatPrivado.setVisible(false);

        listaAmistades.setPrefSize(200, 300);
        listaSolicitudes.setPrefSize(200, 300);

        // ---------- Listeners ----------
        listaSolicitudes.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean visible = newVal != null;
            btnAceptar.setVisible(visible);
            btnRechazar.setVisible(visible);
        });

        listaAmistades.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) ->
                btnChatPrivado.setVisible(newVal != null));

        // ---------- Acciones botones ----------
        btnAceptar.setOnAction(e -> {
            String recibe = listaSolicitudes.getSelectionModel().getSelectedItem();
            servidor.aceptarAmistad(nombreUser, recibe);
            listaSolicitudes.getItems().setAll(cliente.getSolicitudesPendientes());
            btnAceptar.setVisible(false);
            btnRechazar.setVisible(false);
        });

        btnRechazar.setOnAction(e -> {
            String recibe = listaSolicitudes.getSelectionModel().getSelectedItem();
            servidor.rechazarAmistad(nombreUser, recibe);
            listaSolicitudes.getItems().setAll(cliente.getSolicitudesPendientes());
            btnAceptar.setVisible(false);
            btnRechazar.setVisible(false);
        });

        btnChatPrivado.setOnAction(e -> {
            String amigoSeleccionado = listaAmistades.getSelectionModel().getSelectedItem();
            if (amigoSeleccionado != null) {
                String amigoLimpio = amigoSeleccionado.replace("  [!]", "");
                crearVentanaChatPrivado(amigoLimpio);
            }
        });

        // ---------- Chat general ----------
        chatLog = new TextFlow();
        chatLog.setPadding(new Insets(5));

        chatScrollPane = new ScrollPane(chatLog);
        chatScrollPane.setFitToWidth(true);
        chatScrollPane.setPrefHeight(400);
        chatScrollPane.setVvalue(1.0);

        chatLog.heightProperty().addListener((obs, oldHeight, newHeight) ->
                Platform.runLater(() -> chatScrollPane.setVvalue(1.0)));

        TextField chatInput = new TextField();
        chatInput.setPromptText("Escribe tu mensaje aquí...");

        Button btnEnviar = new Button("Enviar");
        btnEnviar.setOnAction(e -> {
            String mensaje = chatInput.getText().trim();
            if (!mensaje.isEmpty()) {
                try {
                    cliente.enviar(nombreUser, mensaje);
                    this.recibirMensaje(nombreUser, mensaje);
                } catch (RemoteException ex) {
                    ErrorPopup.show("Se ha producido un error al enviar el mensaje.");
                }
                chatInput.clear();
            }
        });

        chatInput.setOnAction(e -> btnEnviar.fire());

        HBox chatControls = new HBox(5, chatInput, btnEnviar);
        chatControls.setAlignment(Pos.CENTER);

        VBox chatRoom = new VBox(5, new Label("Chat"), chatScrollPane, chatControls);
        chatRoom.setPrefWidth(300);
        chatRoom.setAlignment(Pos.CENTER);

        // ---------- Layout principal ----------
        VBox vboxLista1 = new VBox(5, new Label("Amigos en línea"), listaAmistades, btnChatPrivado);
        VBox vboxLista2 = new VBox(5, new Label("Solicitudes de amistad"), listaSolicitudes, btnAceptar, btnRechazar);

        vboxLista1.setAlignment(Pos.TOP_CENTER);
        vboxLista2.setAlignment(Pos.TOP_CENTER);

        HBox hboxListas = new HBox(10, vboxLista1, chatRoom, vboxLista2);
        hboxListas.setPadding(new Insets(15));
        hboxListas.setAlignment(Pos.CENTER);

        BorderPane root = new BorderPane();
        root.setTop(barraMenu);
        root.setCenter(hboxListas);

        // ---------- Evento al cerrar ----------
        stage.setOnCloseRequest(e -> {
            try {
                servidor.eliminar(cliente, nombreUser);
                System.exit(0);
            } catch (RemoteException exception) {
                exception.printStackTrace();
                ErrorPopup.show("Error al salir");
            }
        });

        // ---------- Mostrar ----------
        Scene scene = new Scene(root, 800, 500);
        stage.setScene(scene);
        stage.setTitle("Ventana Usuario: " + nombreUser);
        stage.show();
    }

    // ------------------------------------------------------------
    // MÉTODOS AUXILIARES
    // ------------------------------------------------------------
    private void actualizarListaAmigosConNotificaciones(ObservableList<String> listaItems) {
        listaItems.clear();
        for (String amigo : amigos.keySet()) {
            String nombreAmigo = amigo;
            if (cliente.getUsuariosConMensajesPendientes().contains(amigo)) {
                nombreAmigo += "  [!]";
            }
            listaItems.add(nombreAmigo);
        }
    }

    private void crearVentanaChatPrivado(String amigo) {
        if (ventanaChatActual != null) {
            ventanaChatActual.getStage().close();
        }

        ventanaChatActual = new VentanaChatPrivado(stage, cliente, nombreUser, amigo);
        ventanaChatActual.getStage().setOnHidden(e -> {
            ventanaChatActual = null;
            Platform.runLater(() -> {
                ObservableList<String> items = listaAmistades.getItems();
                for (int i = 0; i < items.size(); i++) {
                    String item = items.get(i);
                    if (item.contains(amigo)) {
                        items.set(i, amigo);
                        break;
                    }
                }
            });
        });

        ventanaChatActual.mostrar();
    }

    private void agregarLineaChat(String texto, Color color) {
        Platform.runLater(() -> {
            Text textNode = new Text(texto + "\n");
            if (color != null) textNode.setFill(color);
            chatLog.getChildren().add(textNode);
            chatScrollPane.setVvalue(1.0);
        });
    }

    private void agregarMensajeChat(String remitente, String mensaje) {
        String timestamp = java.time.LocalTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        agregarLineaChat("[" + timestamp + "] <" + remitente + ">: " + mensaje, null);
    }

    private void agregarNotificacionSistema(String mensaje, Color color) {
        agregarLineaChat(mensaje, color);
    }

    // ------------------------------------------------------------
    // MÉTODOS DEL OBSERVADOR
    // ------------------------------------------------------------
    public void recibirMensaje(String remitente, String mensaje) {
        agregarMensajeChat(remitente, mensaje);
    }

    @Override
    public boolean mensajeRecibidoPrivado(String remitente, String mensaje) {
        if (ventanaChatActual != null && ventanaChatActual.getAmigo().equals(remitente)) {
            ventanaChatActual.agregarMensaje(remitente, mensaje);
            return true;
        }
        return false;
    }

    @Override
    public void mensajeRecibido(String remitente, String mensaje) {
        agregarMensajeChat(remitente, mensaje);
    }

    @Override
    public void notificarMensajesPendientes(String remitente) {
        Platform.runLater(() -> {
            ObservableList<String> items = listaAmistades.getItems();
            for (int i = 0; i < items.size(); i++) {
                String item = items.get(i);
                if (item.equals(remitente) && !item.contains(" [!]")) {
                    items.set(i, remitente + "  [!]");
                    break;
                }
            }
        });
    }

    @Override
    public void notificarConexion(String nombre) {
        agregarNotificacionSistema(nombre + " se ha conectado.", Color.GREEN);
    }

    @Override
    public void notificarDesconexion(String nombre) {
        agregarNotificacionSistema(nombre + " se ha desconectado.", Color.RED);
    }

    @Override
    public void notificarSolicitud() {
        Platform.runLater(() -> listaSolicitudes.setItems(cliente.getSolicitudesPendientes()));
    }
}
