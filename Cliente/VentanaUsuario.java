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
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.rmi.RemoteException;
import java.util.List;

public class VentanaUsuario implements ObservadorChat  {

    private Stage stage;
    private InterfazCBServ servidor;
    private InterfazCBImp cliente;
    private String nombreUser;
    private ObservableMap<String,InterfazCB> amigos;
    private ScrollPane chatScrollPane;
    private VBox chatLogContainer;
    private TextFlow chatLog;
    private ListView<String> listaSolicitudes;
    private ListView<String> listaAmistades;
    private Button btnChatPrivado;
    private VentanaChatPrivado ventanaChatActual; //solo unha á vez

    public VentanaUsuario(Stage stage, InterfazCBServ servidor, InterfazCBImp cliente, String nombreUser) {

        this.stage = stage;
        this.servidor = servidor;
        this.nombreUser = nombreUser;
        this.cliente = cliente;
        amigos = cliente.getAmigosEnLinea();
        cliente.setObservadorChat(this);

    }


    public void mostrar() {

        //Creacion de popup
        Stage popup = new Stage();

        TextField tfNombreNuevoAmigo = new TextField();
        tfNombreNuevoAmigo.setPromptText("Nuevo Amigo...");

        Button btnAgregar = new Button("Agregar");
        btnAgregar.setOnAction(e -> {
            servidor.enviarSolicitudAmistad(nombreUser, tfNombreNuevoAmigo.getText().trim());
            popup.hide();
        });

        VBox contenido = new VBox(10,tfNombreNuevoAmigo,btnAgregar);
        Scene popupScene = new Scene(contenido,200,150);
        popup.setScene(popupScene);
        popup.initOwner(stage);
        popup.initModality(Modality.WINDOW_MODAL);

        //Creacion de menus
        MenuBar barraMenu = new MenuBar();

        Menu menuSolicitudes = new Menu("Solicitudes");
        MenuItem agregar = new MenuItem("Agregar");

        agregar.setOnAction(e -> {
            popup.show();
        });

        menuSolicitudes.getItems().addAll(agregar);
        barraMenu.getMenus().addAll(menuSolicitudes);

        //Crear listas actulizables
        ObservableList<String> lista1Items = FXCollections.observableArrayList();

        // Actualizar lista de amigos con indicadores de mensajes pendientes
        actualizarListaAmigosConNotificaciones(lista1Items);

        // Listener que se ejecuta cuando cambia el ObservableMap de amigos
        amigos.addListener((MapChangeListener.Change<? extends String, ? extends InterfazCB> change) -> {
            Platform.runLater(() -> {
                actualizarListaAmigosConNotificaciones(lista1Items);
            });
        });

        // actualizar cuando hay nuevos mensajes pendientes
        cliente.getUsuariosConMensajesPendientes().addListener((javafx.collections.ListChangeListener.Change<? extends String> change) -> {
            Platform.runLater(() -> {
                actualizarListaAmigosConNotificaciones(lista1Items);
            });
        });

        listaAmistades = new ListView<>(lista1Items);
        listaSolicitudes = new ListView<>(cliente.getSolicitudesPendientes());
        Button btnAceptar = new Button("Aceptar");
        btnAceptar.setVisible(false);

        Button btnRechazar = new Button("Rechazar");
        btnRechazar.setVisible(false);

        btnChatPrivado = new Button("Chat Privado");
        btnChatPrivado.setVisible(false);

        listaAmistades.setPrefSize(200, 300); // ancho x alto
        listaSolicitudes.setPrefSize(200, 300);

        listaSolicitudes.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable,oldValue,newValue) -> {
                    if (newValue != null){
                        btnAceptar.setVisible(true);
                        btnRechazar.setVisible(true);
                    }else{
                        btnAceptar.setVisible(false);
                        btnRechazar.setVisible(false);
                    }
                });

        listaAmistades.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable,oldValue,newValue) -> {
                    if (newValue != null){
                        btnChatPrivado.setVisible(true);
                    }else{
                        btnChatPrivado.setVisible(false);
                    }
                });

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
            listaSolicitudes.getItems().setAll((cliente.getSolicitudesPendientes()));
            btnAceptar.setVisible(false);
            btnRechazar.setVisible(false);
        });

        btnChatPrivado.setOnAction(e -> {
            String amigoSeleccionado = listaAmistades.getSelectionModel().getSelectedItem();
            if (amigoSeleccionado != null) {
                // Limpiar el indicador de mensajes pendientes
                String amigoLimpio = amigoSeleccionado.replace("  [!]", "");
                crearVentanaChatPrivado(amigoLimpio);
            }
        });

        // chatlog (con todos tus amigos)
        chatLog = new TextFlow();
        chatLog.setPadding(new Insets(5));

        chatScrollPane = new ScrollPane(chatLog);
        chatScrollPane.setFitToWidth(true);
        chatScrollPane.setPrefHeight(400);
        chatScrollPane.setVvalue(1.0);

        chatLog.heightProperty().addListener((obs, oldHeight, newHeight) -> {
            Platform.runLater(() -> {
                chatScrollPane.setVvalue(1.0);
            });
        });

        TextField chatInput = new TextField();
        chatInput.setPromptText("Escribe tu mensaje aquí...");

        Button btnEnviar = new Button("Enviar");
        btnEnviar.setOnAction(e -> {
            String mensaje = chatInput.getText().trim();
            if (!mensaje.isEmpty()) {
                try {
                    cliente.enviar(nombreUser, mensaje);
                    this.recibirMensaje(nombreUser, mensaje);
                }
                catch (RemoteException ex){
                    ErrorPopup.show("Se ha producido un error al enviar el mensaje.");
                }
                chatInput.clear();
            }
        });

        // Enter key también envía el mensaje
        chatInput.setOnAction(e -> btnEnviar.fire());

        HBox chatControls = new HBox(5, chatInput, btnEnviar);
        chatControls.setAlignment(Pos.CENTER);

        VBox chatRoom = new VBox(5, new Label("Chat"), chatScrollPane, chatControls);
        chatRoom.setPrefWidth(300);

        // Crear etiquetas para cada lista
        VBox vboxLista1 = new VBox(5, new Label("Amigos en linea"), listaAmistades, btnChatPrivado);
        VBox vboxLista2 = new VBox(5, new Label("Solicitudes de amistad"), listaSolicitudes, btnAceptar, btnRechazar);

        vboxLista1.setAlignment(Pos.TOP_CENTER);
        vboxLista2.setAlignment(Pos.TOP_CENTER);
        chatRoom.setAlignment(Pos.CENTER);

        HBox hboxListas = new HBox(10, vboxLista1, chatRoom, vboxLista2);
        hboxListas.setPadding(new Insets(15));
        hboxListas.setAlignment(Pos.CENTER);

        BorderPane root = new BorderPane();
        root.setTop(barraMenu);
        root.setCenter(hboxListas);

        Scene scene = new Scene(root, 800, 500);
        stage.setScene(scene);
        stage.setTitle("Ventana Usuario: " + nombreUser);
        stage.show();
    }

    private void actualizarListaAmigosConNotificaciones(ObservableList<String> listaItems) {
        listaItems.clear();
        for (String amigo : amigos.keySet()) {
            String nombreAmigo = amigo;
            // verificar si este amigo tiene mensajes pendientes
            if (cliente.getUsuariosConMensajesPendientes().contains(amigo)) {
                nombreAmigo += "  [!]"; // Añadir emoji de notificación
            }
            listaItems.add(nombreAmigo);
        }
    }

    private void crearVentanaChatPrivado(String amigo) {
        if (ventanaChatActual != null) {
            // cerrar a previa
            ventanaChatActual.getStage().close();
        }

        ventanaChatActual = new VentanaChatPrivado(stage, cliente, nombreUser, amigo);
        ventanaChatActual.getStage().setOnHidden(e -> {
            ventanaChatActual = null;
            // Actualizar la lista para quitar el indicador de mensajes
            Platform.runLater(() -> {
                ObservableList<String> items = listaAmistades.getItems();
                for (int i = 0; i < items.size(); i++) {
                    String item = items.get(i);
                    if (item.contains(amigo)) {
                        items.set(i, amigo); // Quitar el emoji
                        break;
                    }
                }
            });
        });

        ventanaChatActual.mostrar();
    }

    // devuelve true si la ventana ya está abierta y no hay que guardar el mensaje en buzon
    // devuelve false si hay que guardar el mensaje en buzon de entrada
    @Override
    public boolean mensajeRecibidoPrivado(String remitente, String mensaje){
        if (ventanaChatActual!=null) {
            if (ventanaChatActual.getAmigo().equals(remitente)) {
                ventanaChatActual.agregarMensaje(remitente, mensaje);
                return true;
            }
        }
        return false;
    }

    @Override
    public void notificarMensajesPendientes(String remitente) {
        Platform.runLater(() -> {
            // Actualizar la lista de amigos para mostrar el indicador
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

    // metodo que interactua coa GUI
    private void agregarLineaChat(String texto, Color color) {
        Platform.runLater(() -> {
            Text textNode = new Text(texto + "\n");
            if (color != null) {
                textNode.setFill(color);
            }
            chatLog.getChildren().add(textNode);
            chatScrollPane.setVvalue(1.0);
        });
    }

    private void agregarMensajeChat(String remitente, String mensaje) {
        String timestamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        String texto = "[" + timestamp + "] <" + remitente + ">: " + mensaje;
        agregarLineaChat(texto, null);
    }

    //igual que agregarmensaje pero con color custom
    private void agregarNotificacionSistema(String mensaje, Color color) {
        agregarLineaChat(mensaje, color);
    }

    public void recibirMensaje(String remitente, String mensaje) {
        agregarMensajeChat(remitente, mensaje);
    }

    @Override
    public void mensajeRecibido(String remitente, String mensaje) {
        agregarMensajeChat(remitente, mensaje);
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
        Platform.runLater(() -> {
            listaSolicitudes.setItems(cliente.getSolicitudesPendientes());
        });
    }
}