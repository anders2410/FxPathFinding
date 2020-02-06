package org.openjfx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.net.URL;

// The guide used to make this file
// https://www.vojtechruzicka.com/javafx-getting-started/
public class MainApp extends Application {

    public static void main(String[] args) {
        // Here you can work with args - command line parameters
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/scene.fxml"));

        Scene scene = new Scene(root);

        stage.setTitle("JavaFX and Gradle");
        stage.setScene(scene);
        stage.show();
    }
}
