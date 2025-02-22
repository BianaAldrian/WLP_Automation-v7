package org.nikkatrading.wlp_automationv7.UI;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class LoadingUtil {
   private static Stage loadingStage;
   
   public static void showLoading(Stage owner, String message) {
      if (loadingStage != null && loadingStage.isShowing()) {
         return; // Prevent multiple loading screens
      }
      
      loadingStage = new Stage();
      loadingStage.initOwner(owner);
      loadingStage.initModality(Modality.APPLICATION_MODAL);
      loadingStage.initStyle(StageStyle.TRANSPARENT);
      loadingStage.setAlwaysOnTop(true);
      
      // Background effect
      StackPane root = new StackPane();
      root.setStyle("-fx-background-color: rgba(0, 0, 0, 0.7); -fx-padding: 20px; -fx-background-radius: 10;");
      
      // Progress indicator
      ProgressIndicator progressIndicator = new ProgressIndicator();
      progressIndicator.setStyle("-fx-progress-color: white;");
      
      // Loading label
      Label loadingLabel = new Label(message);
      loadingLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
      
      // Layout setup
      VBox vbox = new VBox(15, progressIndicator, loadingLabel);
      vbox.setAlignment(Pos.CENTER);
      root.getChildren().add(vbox);
      
      Scene scene = new Scene(root, 250, 150);
      scene.setFill(Color.TRANSPARENT);
      loadingStage.setScene(scene);
      
      // Fade animation (pulse effect)
      FadeTransition fade = new FadeTransition(Duration.seconds(1), root);
      fade.setFromValue(0.5);
      fade.setToValue(1.0);
      fade.setCycleCount(FadeTransition.INDEFINITE);
      fade.setAutoReverse(true);
      fade.play();
      
      // Show the loading animation
      Platform.runLater(() -> loadingStage.show());
   }
   
   public static void closeLoading() {
      if (loadingStage != null) {
         Platform.runLater(() -> {
            loadingStage.close();
            loadingStage = null; // Reset the stage reference
         });
      }
   }
}
