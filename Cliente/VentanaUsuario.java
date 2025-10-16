import java.rmi.RemoteException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.application.Platform;
import javafx.collections.ObservableMap;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.scene.control.ListView;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class VentanaUsuario {

    private Stage stage;
    private InterfazCBServ servidor;
    private InterfazCBImp cliente;
    private String nombreUser;
    private ObservableMap<String,InterfazCB> amigos;
    private ObservableList<String> solicitudes;

    public VentanaUsuario(Stage stage, InterfazCBServ servidor, InterfazCBImp cliente, String nombreUser) {
        this.stage = stage;
        this.servidor = servidor;
        this.nombreUser = nombreUser;
        this.cliente = cliente;
        amigos = cliente.getAmigosEnLinea();
        solicitudes = cliente.getSolicitudesPendientes();
    }

    public void mostrar() {
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
        ListView<String> listaSolicitudes = new ListView<>(cliente.getSolicitudesPendientes());

        listaAmistades.setPrefSize(200, 300); // ancho x alto
        listaSolicitudes.setPrefSize(200, 300);

        // --- Crear etiquetas para cada lista ---
        VBox vboxLista1 = new VBox(5, new Label("Amigos en linea"), listaAmistades);
        VBox vboxLista2 = new VBox(5, new Label("Solicitudes de amistad"), listaSolicitudes);

        vboxLista1.setAlignment(Pos.TOP_CENTER);
        vboxLista2.setAlignment(Pos.TOP_CENTER);

        // --- Poner las dos listas lado a lado ---
        HBox hboxListas = new HBox(20, vboxLista1, vboxLista2); // 20px de espacio entre listas
        hboxListas.setPadding(new Insets(15));
        hboxListas.setAlignment(Pos.CENTER);

        // --- BorderPane para organizar la ventana ---
        BorderPane root = new BorderPane();
        root.setCenter(hboxListas);

        Scene scene = new Scene(root, 450, 350);
        stage.setScene(scene);
        stage.setTitle("Ventana Usuario: ");
        stage.show();
    }
}
