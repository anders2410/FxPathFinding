package javafx;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.stage.Stage;

/*  The guide used to make this file
    https://www.vojtechruzicka.com/javafx-getting-started/

    This file is the mainApp in JavaFX and is used to launch the application.
*/
public class MainApp extends Application {

    public static void main(String[] args) {
        // Here you can work with args - command line parameters
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        // Loads the FXML file and attach it to the Scene.
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/javafx/scene.fxml"));
        Parent root = fxmlLoader.load();
        ((FXMLController) fxmlLoader.getController()).setStage(stage);
        Scene scene = new Scene(root);

        stage.setTitle("Single Source Shortest Path");
        stage.setScene(scene);
        stage.show();
    }
}
