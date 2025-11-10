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

    // Constructor - crea la ventana del chat privado
    public VentanaChatPrivado(Stage owner, InterfazCBImp cliente, String usuarioActual, String amigo) {
        this.stage = new Stage();
        this.cliente = cliente;
        this.usuarioActual = usuarioActual;
        this.amigo = amigo;

        stage.setTitle("Chat Privado con " + amigo);
        stage.initOwner(owner); // hace esta ventana dependiente de la principal

        inicializarUI();        // construir interfaz
        cargarMensajesPendientes(); // cargar mensajes acumulados si existían
    }

    public String getAmigo() {
        return amigo;
    }

    // Configura la interfaz gráfica del chat
    private void inicializarUI() {

        // Área donde aparecerán los mensajes
        chatLog = new TextFlow();
        chatLog.setPadding(new Insets(5));

        // Scroll para visualizar el historial completo
        scrollPane = new ScrollPane(chatLog);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(300);
        scrollPane.setVvalue(1.0);

        // Campo de texto para escribir mensajes
        TextField inputPrivado = new TextField();
        inputPrivado.setPromptText("Mensaje para " + amigo + "...");

        // Botón para enviar
        Button btnEnviarPrivado = new Button("Enviar");
        btnEnviarPrivado.setOnAction(e -> enviarMensajePrivado(inputPrivado));

        // Permitir enviar mensaje presionando Enter
        inputPrivado.setOnAction(e -> enviarMensajePrivado(inputPrivado));

        // Contenedor de input y botón
        HBox controlesPrivado = new HBox(5, inputPrivado, btnEnviarPrivado);
        controlesPrivado.setAlignment(Pos.CENTER);

        // Layout general de la ventana
        VBox layoutPrivado = new VBox(10, new Label("Chat con " + amigo), scrollPane, controlesPrivado);
        layoutPrivado.setPadding(new Insets(15));

        // Crear escena
        Scene escenaPrivada = new Scene(layoutPrivado, 400, 400);
        stage.setScene(escenaPrivada);
    }

    // Cargar mensajes que llegaron cuando la ventana estaba cerrada
    private void cargarMensajesPendientes() {
        List<String> mensajesPendientes = cliente.obtenerMensajesPendientes(amigo);

        if (!mensajesPendientes.isEmpty()) {
            Platform.runLater(() -> {
                // Mostrar cada mensaje pendiente
                for (String mensaje : mensajesPendientes) {
                    agregarMensaje(amigo, mensaje);
                }

                // Mostrar aviso de cantidad de mensajes pendientes
                agregarMensajeSistema("--- " + mensajesPendientes.size() + " mensajes sin leer ---");

                System.out.println(mensajesPendientes.size() + " mensajes pendientes de " + amigo);
            });
        }
    }

    // Enviar mensaje privado al servidor
    private void enviarMensajePrivado(TextField inputPrivado) {
        String mensaje = inputPrivado.getText().trim();

        // Si el mensaje no está vacío, enviarlo
        if (!mensaje.isEmpty()) {
            try {
                cliente.enviarMensajePrivado(usuarioActual, mensaje, amigo); // RMI
                agregarMensaje(usuarioActual, mensaje); // Mostrar en pantalla
                inputPrivado.clear();
            } catch (Exception ex) {
                ErrorPopup.show("Error al enviar mensaje privado.");
            }
        }
    }

    // Mostrar mensaje en el chat (local o recibido)
    public void agregarMensaje(String remitente, String mensaje) {
        Platform.runLater(() -> {

            // Formatear hora
            String timestamp = java.time.LocalTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("HH:mm")
            );

            String texto = "[" + timestamp + "] <" + remitente + ">: " + mensaje + "\n";
            Text textNode = new Text(texto);

            // Color del texto según origen
            if (remitente.equals(usuarioActual)) {
                textNode.setFill(Color.BLUE); // mensajes propios
            } else {
                textNode.setFill(Color.BLACK); // mensajes del amigo
            }

            chatLog.getChildren().add(textNode);

            // Desplazar scroll al final automáticamente
            scrollPane.setVvalue(1.0);
        });
    }

    // Mensaje del sistema (informativos)
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
        Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }

    public Stage getStage() {
        return stage;
    }
}
