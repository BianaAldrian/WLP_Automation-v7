package org.nikkatrading.wlp_automationv7.UI;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.nikkatrading.wlp_automationv7.MainController;

public class UI_Control {
   
   private final MainController.LotsSetupCallback callback;
   
   public UI_Control(ImageView lots_setup, String[] sortedLotsArray, MainController.LotsSetupCallback callback) {
      this.callback = callback;
      lots_setup.setOnMouseClicked(event -> openLotsSetupView(sortedLotsArray));
   }
   
   private void openLotsSetupView(String[] sortedLotsArray) {
      try {
         FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/nikkatrading/wlp_automationv7/lots_setup.fxml"));
         Parent root = loader.load();
         
         LotsSetupController lotsSetupController = loader.getController();
         
         if (sortedLotsArray != null) {
            lotsSetupController.setSortedLotsArray(sortedLotsArray);
         } else {
            System.out.println("sortedLotsArray is null in main view.");
         }
         
         lotsSetupController.setCallback(callback); // Set the callback
         
         Stage stage = new Stage();
         lotsSetupController.setStage(stage); // Set the stage reference
         
         stage.setTitle("Lots Setup");
         stage.setScene(new Scene(root));
         stage.show();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
}

