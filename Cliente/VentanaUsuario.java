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
        ObservableList<String> lista1Items = FXCollections.observableArrayList(
                amigos.keySet()
        );

        // Listener que se ejecuta cuando cambia el ObservableMap
        amigos.addListener((MapChangeListener.Change<? extends String, ? extends InterfazCB> change) -> {
            Platform.runLater(() -> {
                // Actualiza la lista visual con las claves del mapa
                lista1Items.setAll(amigos.keySet());
            });
        });

        ListView<String> listaAmistades = new ListView<>(lista1Items);
        listaSolicitudes = new ListView<>(cliente.getSolicitudesPendientes());
        Button btnAceptar = new Button("Aceptar");
        btnAceptar.setVisible(false);

        Button btnRechazar = new Button("Rechazar");
        btnRechazar.setVisible(false);

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
// chatlog (con todos tus amigos)
        chatLog = new TextFlow();
        chatLog.setPadding(new Insets(5));

        chatScrollPane = new ScrollPane(chatLog);
        chatScrollPane.setFitToWidth(true);
        chatScrollPane.setPrefHeight(400);
        chatScrollPane.setVvalue(1.0); // Start at bottom

// Auto-scroll when content changes
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

        VBox chatRoom = new VBox(5, new Label("Chat"), chatScrollPane, chatControls); // ← Use chatScrollPane here
        chatRoom.setPrefWidth(300);

        // Crear etiquetas para cada lista
        VBox vboxLista1 = new VBox(5, new Label("Amigos en linea"), listaAmistades);
        VBox vboxLista2 = new VBox(5, new Label("Solicitudes de amistad"), listaSolicitudes, btnAceptar, btnRechazar);

        vboxLista1.setAlignment(Pos.TOP_CENTER);
        vboxLista2.setAlignment(Pos.TOP_CENTER);
        chatRoom.setAlignment(Pos.CENTER);

        HBox hboxListas = new HBox(10, vboxLista1, chatRoom, vboxLista2); // 10px de espacio entre secciones
        hboxListas.setPadding(new Insets(15));
        hboxListas.setAlignment(Pos.CENTER);

        BorderPane root = new BorderPane();
        root.setTop(barraMenu);
        root.setCenter(hboxListas);

        Scene scene = new Scene(root, 800, 500); // Ventana más grande para acomodar el chat
        stage.setScene(scene);
        stage.setTitle("Ventana Usuario: " + nombreUser);
        stage.show();
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