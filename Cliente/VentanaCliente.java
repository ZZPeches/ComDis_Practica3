import java.rmi.Naming;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class VentanaCliente {

    private Stage stage;
    private InterfazCBServ servidor;
    private InterfazCBImp cliente;
    private String nombreUser = "";

    public VentanaCliente(Stage stage, InterfazCBServ servidor, InterfazCBImp cliente) {
        this.stage = stage;
        this.servidor = servidor;
        this.cliente = cliente;
    }

    public void mostrar() {
        // --- ESCENA LOGIN ---
        Label lblBienvenida = new Label("Bienvenido");
        Label lblStatus = new Label();

        TextField tfNombreUser = new TextField();
        tfNombreUser.setPromptText("Nombre de usuario");

        PasswordField pfPasswdUser = new PasswordField();
        pfPasswdUser.setPromptText("Contraseña");

        Button btnLogin = new Button("Login");
        Button btnCerrar = new Button("Cerrar");

        // Botones de acción agrupados en HBox
        HBox botonesLogin = new HBox(10, btnLogin, btnCerrar);

        // BorderPane para la escena login
        BorderPane root = new BorderPane();
        root.setTop(lblBienvenida);
        BorderPane.setMargin(lblBienvenida, new Insets(10));

        root.setCenter(new VBox(10, tfNombreUser, pfPasswdUser, lblStatus));
        root.setBottom(botonesLogin);
        BorderPane.setMargin(botonesLogin, new Insets(10));

        // márgenes individuales entre los nodos del VBox
        VBox.setMargin(tfNombreUser, new Insets(5));
        VBox.setMargin(pfPasswdUser, new Insets(5));
        VBox.setMargin(lblStatus, new Insets(10, 0, 0, 0));

        Scene scene = new Scene(root, 400, 250);

        // --- ESCENA REGISTRO ---
        Label lblRegistro = new Label("Registro");
        PasswordField pfPasswdScene2 = new PasswordField();
        pfPasswdScene2.setPromptText("Introduzca su contraseña");

        Button btnSi = new Button("Si");
        Button btnNo = new Button("No");
        HBox botonesRegistro = new HBox(10, btnSi, btnNo);

        BorderPane root2 = new BorderPane();
        root2.setTop(lblRegistro);
        BorderPane.setMargin(lblRegistro, new Insets(10));

        root2.setCenter(pfPasswdScene2);
        root2.setBottom(botonesRegistro);
        BorderPane.setMargin(botonesRegistro, new Insets(10));

        Scene scene2 = new Scene(root2, 400, 200);

        stage.setScene(scene);
        stage.show();

        // --- EVENTOS ---
        btnLogin.setOnAction(e -> {
            try {
                nombreUser = tfNombreUser.getText().trim();
                if(!servidor.validarUsuarioExistente(nombreUser)){
                    stage.setScene(scene2);
                } else {
                    if(servidor.loginUsuario(nombreUser, pfPasswdUser.getText().trim(), cliente)){
                        VentanaUsuario ventanaUsuario = new VentanaUsuario(stage, servidor, cliente,nombreUser);
                        ventanaUsuario.mostrar();
                    }
                }
            } catch (Exception ex) {
                lblStatus.setText("Error: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        btnSi.setOnAction(e -> {
            try {
                if(pfPasswdScene2.getText().trim().isEmpty()){
                    lblStatus.setText("Debe introducir una contraseña");
                } else {
                    servidor.registrarUsuario(nombreUser, pfPasswdScene2.getText().trim());
                    stage.setScene(scene);
                }
            } catch (Exception ex) {
                lblStatus.setText("Error: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        btnNo.setOnAction(e -> stage.close());
        btnCerrar.setOnAction(e -> stage.close());
    }
}
