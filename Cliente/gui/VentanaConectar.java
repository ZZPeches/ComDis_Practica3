package gui;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.rmi.Naming;
import java.rmi.RemoteException;

import controlador.InterfazCBImp;
import controlador.InterfazCBServ;

public class VentanaConectar {

    private Stage stage;

    public VentanaConectar(Stage stage) {
        this.stage = stage;
    }

    // Método para construir y mostrar la interfaz de conexión
    public void mostrar() {

        TextField tfHostname = new TextField();
        tfHostname.setPromptText("Introduzca el nombre del nodo");
        tfHostname.setText("localhost"); // Valor por defecto

        TextField tfPuerto = new TextField();
        tfPuerto.setPromptText("Introduzca el puerto");
        tfPuerto.setText("9000"); // Valor por defecto

        Button btnConectar = new Button("Conectar");

        // Etiqueta para mostrar mensajes de estado (éxito o error)
        Label lblStatus = new Label();

        // Acción del botón "Conectar"
        btnConectar.setOnAction(e -> {

            try {
                String hostname = tfHostname.getText().trim();
                int puerto = Integer.parseInt(tfPuerto.getText().trim());

                // Construcción de URL RMI para acceder al objeto remoto
                String URL = "rmi://" + hostname + ":" + puerto + "/objetoRemoto";

                // Lookup al registro RMI para obtener referencia del servidor
                InterfazCBServ servidor = (InterfazCBServ) Naming.lookup(URL);

                // Crear objeto cliente RMI para recibir callbacks del servidor
                InterfazCBImp objetoCli = new InterfazCBImp();

                lblStatus.setText("Conexión exitosa al servidor RMI");

                // Abrir ventana principal del cliente
                VentanaCliente ventanaCliente = new VentanaCliente(stage, servidor, objetoCli);
                ventanaCliente.mostrar();

            } catch (Exception ex) {
                // Manejo de error si falla la conexión
                lblStatus.setText("Error: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        VBox root = new VBox(10);
        root.setPadding(new Insets(15));
        root.getChildren().addAll(tfHostname, tfPuerto, btnConectar, lblStatus);

        // Acción al cerrar la ventana
        stage.setOnCloseRequest(e -> {
            System.out.println("Ejecución con éxito");
            System.exit(0); // Finaliza aplicación
        });

        Scene scene = new Scene(root, 400, 200);
        stage.setScene(scene);
        stage.setTitle("Conexión al servidor");
        stage.show();
    }
}

