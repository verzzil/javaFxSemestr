package ru.itis.application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ru.itis.controllers.FirstMapController;

public class Main extends Application {

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        String fxmlFile = "/fxml/FirstMap.fxml";
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(fxmlFile));

        Parent root = fxmlLoader.load();
        primaryStage.setScene(new Scene(root));
        primaryStage.setTitle("11-906");
        primaryStage.setResizable(false);

        Scene scene = primaryStage.getScene();
        FirstMapController controller = fxmlLoader.getController();
        scene.setOnKeyPressed(controller.keyEventEventHandler);

        primaryStage.show();
    }

}
