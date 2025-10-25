package controlador;

import java.rmi.Naming;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;

import gui.VentanaConectar;


public class ClienteCBGUI extends Application{
    @Override
    public void start(Stage primaryStage) {

        VentanaConectar ventana = new VentanaConectar(primaryStage);
        ventana.mostrar();
    }

    public static void main(String[] args) {
        launch();
    }
}
