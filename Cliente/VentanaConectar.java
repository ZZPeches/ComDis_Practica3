import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.rmi.Naming;

public class VentanaConectarOld {

    private Stage stage;

    public VentanaConectarOld(Stage stage) {
        this.stage = stage;
    }

    public void mostrar() {
        TextField tfHostname = new TextField();
        tfHostname.setPromptText("Introduzca el nombre del nodo");

        TextField tfPuerto = new TextField();
        tfPuerto.setPromptText("Introduzca el puerto");

        Button btnConectar = new Button("Conectar");
        Label lblStatus = new Label();

        btnConectar.setOnAction(e -> {
            try {
                String hostname = tfHostname.getText().trim();
                int puerto = Integer.parseInt(tfPuerto.getText().trim());
                String URL = "rmi://" + hostname + ":" + puerto + "/objetoRemoto";

                InterfazCBServ servidor = (InterfazCBServ) Naming.lookup(URL);
                InterfazCBImp objetoCli = new InterfazCBImp();

                lblStatus.setText("Conexión exitosa al servidor RMI");
                VentanaCliente ventanaCliente = new VentanaCliente(stage, servidor, objetoCli);
                ventanaCliente.mostrar();
            } catch (Exception ex) {
                lblStatus.setText("Error: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        
        VBox root = new VBox(10);
        root.setPadding(new Insets(15));
        root.getChildren().addAll(tfHostname, tfPuerto, btnConectar, lblStatus);

        Scene scene = new Scene(root, 400, 200);
        stage.setScene(scene);
        stage.setTitle("Cliente RMI JavaFX");
        stage.show();

    }
}
