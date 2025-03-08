package org.nikkatrading.wlp_automationv7.UI;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.nikkatrading.wlp_automationv7.MainController;

import java.util.List;

public class UI_Control {
   
   private final MainController.LotsSetupCallback callback;
   private Stage lotsSetupStage = null;
   
   
   public UI_Control(ImageView lots_setup, String[] sortedLotsArray, List<String> finalSelectedLot, MainController.LotsSetupCallback callback) {
      this.callback = callback;
      lots_setup.setOnMouseClicked(event -> openLotsSetupView(sortedLotsArray, finalSelectedLot));
   }
   
   private void openLotsSetupView(String[] sortedLotsArray, List<String> finalSelectedLot) {
      if (lotsSetupStage != null && lotsSetupStage.isShowing()) {
         lotsSetupStage.toFront(); // Bring the existing window to the front
         return; // Don't open a new one
      }
      
      try {
         FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/nikkatrading/wlp_automationv7/lots_setup.fxml"));
         Parent root = loader.load();
         
         LotsSetupController lotsSetupController = loader.getController();
         
         if (sortedLotsArray != null) {
            lotsSetupController.setSortedLotsArray(sortedLotsArray);
         } else {
            System.out.println("sortedLotsArray is null in main view.");
         }
         
         // Pass the finalSelectedLot list
         lotsSetupController.setFinalSelectedLot(finalSelectedLot);
         
         lotsSetupController.setCallback(callback);
         
         lotsSetupStage = new Stage();
         lotsSetupController.setStage(lotsSetupStage);
         
         lotsSetupStage.setTitle("Lots Setup");
         lotsSetupStage.setScene(new Scene(root));
         
         // Reset reference when the window is closed
         lotsSetupStage.setOnHidden(e -> lotsSetupStage = null);
         
         lotsSetupStage.show();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
   
   
}

