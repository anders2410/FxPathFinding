package javafx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.stage.Stage;

// The guide used to make this file
// https://www.vojtechruzicka.com/javafx-getting-started/
public class MainApp extends Application {

    public static void main(String[] args) {
        // Here you can work with args - command line parameters
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/javafx/scene.fxml"));

        Scene scene = new Scene(root);

        stage.setTitle("Single Source Shortest Path");
        stage.setScene(scene);
        stage.show();
    }
}
