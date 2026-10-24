package ni.edu.uni.aso;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * Punto de entrada principal de la aplicación JavaFX.
 * Ajusta de forma adaptativa las dimensiones iniciales a cualquier resolución de pantalla
 * (1080p, laptops de 1366x768 o 1536x864 con escala DPI, proyectores y monitores 4K).
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

        // Calcular dimensiones visuales útiles del monitor (excluyendo la barra de tareas de Windows)
        Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
        double initialWidth = Math.min(1240, visualBounds.getWidth() * 0.94);
        double initialHeight = Math.min(820, visualBounds.getHeight() * 0.92);

        Scene scene = new Scene(root, initialWidth, initialHeight);

        URL cssLocation = App.class.getResource("styles.css");
        if (cssLocation != null) {
            scene.getStylesheets().add(cssLocation.toExternalForm());
        }

        stage.setTitle("Simulador de Entrada/Salida - Arquitectura de SO [UNI DACTIC]");
        stage.setMinWidth(850);
        stage.setMinHeight(480);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
