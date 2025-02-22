package org.nikkatrading.wlp_automationv7;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.nikkatrading.wlp_automationv7.DB.GetConnection;

import java.io.IOException;

public class MainApplication extends Application {
   
   public static void main(String[] args) {
      launch();
   }
   
   @Override
   public void start(Stage stage) {
      try {
         // Ensure the resource path is correct based on your package structure
         FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("/org/nikkatrading/wlp_automationv7/main-view.fxml"));
         Scene scene = new Scene(fxmlLoader.load());
         
         // Get the controller instance and pass the main stage to it
         MainController controller = fxmlLoader.getController();
         controller.setMainStage(stage);
         
         stage.setScene(scene);
         stage.setMaximized(true);  // Maximize the window
         
         // Stop monitoring when the window is closed
         stage.setOnCloseRequest(event -> {
            System.out.println("Application is closing...");
            GetConnection.shutdownMonitoring();
         });
         
         stage.show();
      } catch (IOException e) {
         e.printStackTrace();
         System.err.println("Error loading main-view.fxml. Please check the file path.");
      }
   }
}
