package gui;

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
import java.util.List;

import controlador.InterfazCBImp;
import controlador.InterfazCBServ;

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
        cargarMensajesPendientes();
    }

    public String getAmigo() {
        return amigo;
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

    // enseña al usuario los mensajes sin leer que envio el remitente mientras estaba la ventana cerrada
    private void cargarMensajesPendientes() {
        List<String> mensajesPendientes = cliente.obtenerMensajesPendientes(amigo);

        if (!mensajesPendientes.isEmpty()) {
            Platform.runLater(() -> {

                for (String mensaje : mensajesPendientes) {
                    agregarMensaje(amigo, mensaje);
                }

                agregarMensajeSistema("--- " + mensajesPendientes.size() + " mensajes sin leer ---");

                System.out.println(mensajesPendientes.size() + " mensajes pendientes de " + amigo);
            });
        }
    }

    private void enviarMensajePrivado(TextField inputPrivado) {
        String mensaje = inputPrivado.getText().trim();
        if (!mensaje.isEmpty()) {
            try {
                cliente.enviarMensajePrivado(usuarioActual, mensaje, amigo);
                agregarMensaje(usuarioActual, mensaje);
                inputPrivado.clear();
            } catch (RemoteException ex) {
                ErrorPopup.show("Error al enviar mensaje privado.");
            }
        }
    }

    public void agregarMensaje(String remitente, String mensaje) {
        Platform.runLater(() -> {
            String timestamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
            String texto = "[" + timestamp + "] <" + remitente + ">: " + mensaje + "\n";
            Text textNode = new Text(texto);

            if (remitente.equals(usuarioActual)) {
                textNode.setFill(Color.BLUE); //  propios en azul
            } else {
                textNode.setFill(Color.BLACK); // del amigo en negro
            }

            chatLog.getChildren().add(textNode);
            scrollPane.setVvalue(1.0);
        });
    }

    // " -- x mensajes sin leer -- "
    private void agregarMensajeSistema(String mensaje) {
        Platform.runLater(() -> {
            Text textNode = new Text(mensaje + "\n");
            textNode.setFill(Color.GRAY);
            textNode.setStyle("-fx-font-style: italic;");
            chatLog.getChildren().add(textNode);
            scrollPane.setVvalue(1.0);
        });
    }

    public void mostrar() {
        stage.show();
        Platform.runLater(() -> {
            scrollPane.setVvalue(1.0);
        });
    }

    public Stage getStage() {
        return stage;
    }
}