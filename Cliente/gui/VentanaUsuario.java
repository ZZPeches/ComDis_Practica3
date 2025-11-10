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
    private ScrollPane chatGeneralScrollPane;
    private ScrollPane chatAmigosScrollPane;
    private TextFlow chatGeneralLog;
    private TextFlow chatAmigosLog;

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

        amigos.addListener((MapChangeListener.Change<? extends String, ? extends InterfazCB> change)
                -> Platform.runLater(() -> actualizarListaAmigosConNotificaciones(lista1Items)));

        cliente.getUsuariosConMensajesPendientes().addListener(
                (javafx.collections.ListChangeListener.Change<? extends String> change)
                -> Platform.runLater(() -> actualizarListaAmigosConNotificaciones(lista1Items))
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

        listaAmistades.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal)
                -> btnChatPrivado.setVisible(newVal != null));

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

        // ---------- Chat General (todos los usuarios) ----------
        chatGeneralLog = new TextFlow();
        chatGeneralLog.setPadding(new Insets(5));

        chatGeneralScrollPane = new ScrollPane(chatGeneralLog);
        chatGeneralScrollPane.setFitToWidth(true);
        chatGeneralScrollPane.setPrefHeight(400);
        chatGeneralScrollPane.setVvalue(1.0);

        chatGeneralLog.heightProperty().addListener((obs, oldHeight, newHeight)
                -> Platform.runLater(() -> chatGeneralScrollPane.setVvalue(1.0)));

        TextField chatGeneralInput = new TextField();
        chatGeneralInput.setPromptText("Escribe tu mensaje aquí...");

        Button btnEnviarGeneral = new Button("Enviar");
        btnEnviarGeneral.setOnAction(e -> {
            String mensaje = chatGeneralInput.getText().trim();
            if (!mensaje.isEmpty()) {
                try {
                    cliente.enviar(nombreUser, mensaje);
                    //this.recibirMensaje(nombreUser, mensaje);
                } catch (Exception ex) {
                    ErrorPopup.show("Se ha producido un error al enviar el mensaje.");
                }
                chatGeneralInput.clear();
            }
        });

        chatGeneralInput.setOnAction(e -> btnEnviarGeneral.fire());

        HBox chatGeneralControls = new HBox(5, chatGeneralInput, btnEnviarGeneral);
        chatGeneralControls.setAlignment(Pos.CENTER);

        VBox chatGeneralRoom = new VBox(5, new Label("Chat General"), chatGeneralScrollPane, chatGeneralControls);
        chatGeneralRoom.setPrefWidth(300);
        chatGeneralRoom.setAlignment(Pos.CENTER);

        // ---------- Chat Amigos (solo amigos) ----------
        chatAmigosLog = new TextFlow();
        chatAmigosLog.setPadding(new Insets(5));

        chatAmigosScrollPane = new ScrollPane(chatAmigosLog);
        chatAmigosScrollPane.setFitToWidth(true);
        chatAmigosScrollPane.setPrefHeight(400);
        chatAmigosScrollPane.setVvalue(1.0);

        chatAmigosLog.heightProperty().addListener((obs, oldHeight, newHeight)
                -> Platform.runLater(() -> chatAmigosScrollPane.setVvalue(1.0)));

        TextField chatAmigosInput = new TextField();
        chatAmigosInput.setPromptText("Escribe tu mensaje aquí...");

        Button btnEnviarAmigos = new Button("Enviar");
        btnEnviarAmigos.setOnAction(e -> {
            String mensaje = chatAmigosInput.getText().trim();
            if (!mensaje.isEmpty()) {
                try {
                    cliente.enviarMensajeAmigos(nombreUser, mensaje);
                    this.mensajeRecibidoAmigos(nombreUser, mensaje);
                } catch (Exception ex) {
                    ErrorPopup.show("Se ha producido un error al enviar el mensaje.");
                }
                chatAmigosInput.clear();
            }
        });

        chatAmigosInput.setOnAction(e -> btnEnviarAmigos.fire());

        HBox chatAmigosControls = new HBox(5, chatAmigosInput, btnEnviarAmigos);
        chatAmigosControls.setAlignment(Pos.CENTER);

        VBox chatAmigosRoom = new VBox(5, new Label("Chat Amigos"), chatAmigosScrollPane, chatAmigosControls);
        chatAmigosRoom.setPrefWidth(300);
        chatAmigosRoom.setAlignment(Pos.CENTER);

        // ---------- Panel de pestañas ----------
        TabPane tabPaneChats = new TabPane();

        Tab tabChatGeneral = new Tab("Chat General", chatGeneralRoom);
        tabChatGeneral.setClosable(false);

        Tab tabChatAmigos = new Tab("Chat Amigos", chatAmigosRoom);
        tabChatAmigos.setClosable(false);

        tabPaneChats.getTabs().addAll(tabChatGeneral, tabChatAmigos);
        tabPaneChats.setPrefWidth(300);

        // ---------- Layout principal ----------
        VBox vboxLista1 = new VBox(5, new Label("Amigos en línea"), listaAmistades, btnChatPrivado);
        VBox vboxLista2 = new VBox(5, new Label("Solicitudes de amistad"), listaSolicitudes, btnAceptar, btnRechazar);

        vboxLista1.setAlignment(Pos.TOP_CENTER);
        vboxLista2.setAlignment(Pos.TOP_CENTER);

        HBox hboxListas = new HBox(10, vboxLista1, tabPaneChats, vboxLista2);
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
            } catch (Exception exception) {
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

    private void agregarLineaChatGeneral(String texto, Color color) {
        Platform.runLater(() -> {
            Text textNode = new Text(texto + "\n");
            if (color != null) {
                textNode.setFill(color);
            }
            chatGeneralLog.getChildren().add(textNode);
            chatGeneralScrollPane.setVvalue(1.0);
        });
    }

    private void agregarLineaChatAmigos(String texto, Color color) {
        Platform.runLater(() -> {
            Text textNode = new Text(texto + "\n");
            if (color != null) {
                textNode.setFill(color);
            }
            chatAmigosLog.getChildren().add(textNode);
            chatAmigosScrollPane.setVvalue(1.0);
        });
    }

    private void agregarMensajeChatGeneral(String remitente, String mensaje) {
        String timestamp = java.time.LocalTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        agregarLineaChatGeneral("[" + timestamp + "] <" + remitente + ">: " + mensaje, null);
    }

    private void agregarMensajeChatAmigos(String remitente, String mensaje) {
        String timestamp = java.time.LocalTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        agregarLineaChatAmigos("[" + timestamp + "] <" + remitente + ">: " + mensaje, null);
    }

    private void agregarNotificacionSistemaGeneral(String mensaje, Color color) {
        agregarLineaChatGeneral(mensaje, color);
    }

    private void agregarNotificacionSistemaAmigos(String mensaje, Color color) {
        agregarLineaChatAmigos(mensaje, color);
    }

    // ------------------------------------------------------------
    // MÉTODOS DEL OBSERVADOR
    // ------------------------------------------------------------
    public void recibirMensaje(String remitente, String mensaje) {
        agregarMensajeChatGeneral(remitente, mensaje);
        if (amigos.containsKey(remitente)) {
            agregarMensajeChatAmigos(remitente, mensaje);
        }
    }

    @Override
    public void mensajeRecibidoAmigos(String remitente, String mensaje) {
        agregarMensajeChatAmigos(remitente, mensaje);
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
        agregarMensajeChatGeneral(remitente, mensaje);
        if (amigos.containsKey(remitente)) {
            agregarMensajeChatAmigos(remitente, mensaje);
        }
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
    public void notificarNuevaConexion(String nombre) {
        agregarNotificacionSistemaGeneral(nombre + " se ha conectado.", Color.GREEN);
    }

    @Override
    public void notificarDesconexion(String nombre) {
        agregarNotificacionSistemaGeneral(nombre + " se ha desconectado.", Color.RED);
    }

    @Override
    public void notificarNuevaConexionAmigo(String nombre) {
        agregarNotificacionSistemaAmigos(nombre + " se ha conectado.", Color.GREEN);
    }

    @Override
    public void notificarDesconexionAmigo(String nombre) {
        agregarNotificacionSistemaAmigos(nombre + " se ha desconectado.", Color.RED);
    }

    @Override
    public void notificarSolicitud() {
        Platform.runLater(() -> listaSolicitudes.setItems(cliente.getSolicitudesPendientes()));
    }
}
