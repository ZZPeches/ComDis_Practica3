import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

import java.rmi.RemoteException;

public class VentanaChatPrivado {
    private Stage stage;
    private InterfazCBImp cliente;
    private String usuarioActual;
    private String amigo;
    private TextFlow chatLog;
    private ScrollPane scrollPane;

    public VentanaChatPrivado(Stage owner, InterfazCBImp cliente, String usuarioActual, String amigo) {
        this.stage = new Stage();
        this.cliente = cliente;
        this.usuarioActual = usuarioActual;
        this.amigo = amigo;

        stage.setTitle("Chat Privado con " + amigo);
        stage.initOwner(owner);

        inicializarUI();
    }

    private void inicializarUI() {
        chatLog = new TextFlow();
        chatLog.setPadding(new Insets(5));

        scrollPane = new ScrollPane(chatLog);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(300);
        scrollPane.setVvalue(1.0);

        TextField inputPrivado = new TextField();
        inputPrivado.setPromptText("Mensaje para " + amigo + "...");

        Button btnEnviarPrivado = new Button("Enviar");
        btnEnviarPrivado.setOnAction(e -> enviarMensajePrivado(inputPrivado));

        inputPrivado.setOnAction(e -> enviarMensajePrivado(inputPrivado));

        HBox controlesPrivado = new HBox(5, inputPrivado, btnEnviarPrivado);
        controlesPrivado.setAlignment(Pos.CENTER);

        VBox layoutPrivado = new VBox(10, new Label("Chat con " + amigo), scrollPane, controlesPrivado);
        layoutPrivado.setPadding(new Insets(15));

        Scene escenaPrivada = new Scene(layoutPrivado, 400, 400);
        stage.setScene(escenaPrivada);
    }

    private void enviarMensajePrivado(TextField inputPrivado) {
        String mensaje = inputPrivado.getText().trim();
        if (!mensaje.isEmpty()) {
            try {
                cliente.enviarMensajePrivado(usuarioActual, amigo, mensaje);
                agregarMensaje(usuarioActual, mensaje, Color.BLUE);
                inputPrivado.clear();
            } catch (RemoteException ex) {
                ErrorPopup.show("Error al enviar mensaje privado.");
            }
        }
    }

    public void agregarMensaje(String remitente, String mensaje, Color color) {
        Platform.runLater(() -> {
            String timestamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
            String texto = "[" + timestamp + "] <" + remitente + ">: " + mensaje + "\n";
            Text textNode = new Text(texto);
            if (color != null) {
                textNode.setFill(color);
            }
            chatLog.getChildren().add(textNode);
            scrollPane.setVvalue(1.0);
        });
    }

    public void mostrar() {
        stage.show();
    }

    public Stage getStage() {
        return stage;
    }
}