import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ErrorPopup {

    public static void show(String message) {
        Stage window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL);
        window.setTitle("Error");
        window.setResizable(false);

        Label label = new Label(message);
        label.setWrapText(true); // allow line wrapping
        label.setStyle("-fx-font-size: 14px; -fx-text-fill: red;");

        VBox layout = new VBox(10);
        layout.getChildren().add(label);
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-padding: 20;");

        // set a fixed width so wrapping can occur
        Scene scene = new Scene(layout, 400, 120);
        window.setScene(scene);
        window.showAndWait();
    }
}
