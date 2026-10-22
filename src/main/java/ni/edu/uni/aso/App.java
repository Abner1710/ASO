package ni.edu.uni.aso;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * Punto de entrada principal de la aplicación JavaFX.
 * Inicializa y carga la vista Dashboard.fxml junto con su hoja de estilos.
 */
public class App extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        URL fxmlLocation = App.class.getResource("Dashboard.fxml");
        if (fxmlLocation == null) {
            throw new IllegalStateException("No se pudo localizar el archivo Dashboard.fxml en el classpath.");
        }

        FXMLLoader fxmlLoader = new FXMLLoader(fxmlLocation);
        Parent root = fxmlLoader.load();

        Scene scene = new Scene(root, 1180, 840);

        URL cssLocation = App.class.getResource("styles.css");
        if (cssLocation != null) {
            scene.getStylesheets().add(cssLocation.toExternalForm());
        }

        stage.setTitle("Simulador de Entrada/Salida - Arquitectura de SO [UNI DACTIC]");
        stage.setMinWidth(1080);
        stage.setMinHeight(750);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
