package org.nikkatrading.wlp_automationv7.UI;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.nikkatrading.wlp_automationv7.MainController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LotsSetupController {
   private Stage stage; // Add a reference to the stage
   
   @FXML
   private RadioButton rbAll;
   @FXML
   private GridPane lotsGridPane;
   @FXML
   private Button btnDone;
   
   private String[] sortedLotsArray; // Array for storing the sorted lots
   private final List<CheckBox> checkBoxes = new ArrayList<>();  // Store references to all CheckBoxes
   
   
   private MainController.LotsSetupCallback callback; // Add a reference to the callback
   
   @FXML
   public void initialize() {
      btnDone.setOnMouseClicked(event -> handleDoneAction());
      
      // Add an event handler to rbAll
      rbAll.setOnAction(event -> {
         boolean selected = rbAll.isSelected();
         checkBoxes.forEach(checkBox -> checkBox.setSelected(selected));
      });
   }
   
   // Set the sorted lots in the variable array
   public void setSortedLotsArray(String[] sortedLotsArray) {
      this.sortedLotsArray = sortedLotsArray;
      if (this.sortedLotsArray == null) {
         System.out.println("Received sortedLotsArray is null.");
      } else {
//         System.out.println("Received sortedLotsArray: " + Arrays.toString(this.sortedLotsArray));
         populateLotsGridPane();
      }
   }
   
   // Populate the GridPane using the array of sorted lot
   private void populateLotsGridPane() {
      // Check if sortedLotsArray is null before using it
      if (sortedLotsArray != null) {
         // Loop through sortedLotsArray and create CheckBox for each entry
         for (int i = 0; i < sortedLotsArray.length; i++) {
            // Create a CheckBox for each lot
            CheckBox checkBox = new CheckBox(sortedLotsArray[i].split(":")[0].replace("LOT", "").trim());
            checkBoxes.add(checkBox);  // Add CheckBox to the list
            
            // Add an event handler to each CheckBox
            checkBox.setOnAction(event -> {
               rbAll.setSelected(checkBoxes.stream().allMatch(CheckBox::isSelected));
            });
            
            // Add the CheckBox to the GridPane at row i, column 0 (or wherever you want)
            lotsGridPane.add(checkBox, i % 4, i / 4);  // Distribute checkboxes in a 4-column grid
         }
      } else {
         System.out.println("sortedLotsArray is null in populateLotsGridPane method.");
      }
   }
   
   private void handleDoneAction() {
      // Collect selected CheckBoxes
      List<String> selectedLots = new ArrayList<>();
      for (Node node : lotsGridPane.getChildren()) {
         if (node instanceof CheckBox checkBox) {
            if (checkBox.isSelected()) {
               selectedLots.add(checkBox.getText());
            }
         }
      }
      
     /* // Print the selected lots
      if (!selectedLots.isEmpty()) {
         System.out.println("Selected Lots: " + selectedLots);
      } else {
         System.out.println("No CheckBoxes selected.");
      }*/
      
      // Pass the selected lots to the callback
      if (callback != null) {
         callback.onLotsSelected(selectedLots);
      }
      
      // Close the stage
      if (stage != null) {
         stage.close();
      }
   }
   
   // Method to set the stage reference
   public void setStage(Stage stage) {
      this.stage = stage;
   }
   
   // Method to set the callback
   public void setCallback(MainController.LotsSetupCallback callback) {
      this.callback = callback;
   }
}




