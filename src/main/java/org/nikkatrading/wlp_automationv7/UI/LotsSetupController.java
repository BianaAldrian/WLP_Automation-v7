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
   
   private List<String> finalSelectedLot;
   
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
   
   public void setFinalSelectedLot(List<String> finalSelectedLot) {
      this.finalSelectedLot = finalSelectedLot;
//      System.out.println("finalSelectedLot received: " + finalSelectedLot);
      
      // Refresh checkboxes with previous selections
      populateLotsGridPane();
   }
   
   // Populate the GridPane using the array of sorted lot
   private void populateLotsGridPane() {
      lotsGridPane.getChildren().clear(); // Clear existing checkboxes before populating
      
      if (sortedLotsArray != null) {
         for (int i = 0; i < sortedLotsArray.length; i++) {
            String lotNumber = sortedLotsArray[i].split(":")[0].replace("LOT", "").trim();
            CheckBox checkBox = new CheckBox(lotNumber);
            checkBoxes.add(checkBox);
            
            // Automatically check if it's in finalSelectedLot
            if (finalSelectedLot != null && finalSelectedLot.contains(lotNumber)) {
               checkBox.setSelected(true);
            }
            
            // Event handler for Select All checkbox
            checkBox.setOnAction(event -> {
               rbAll.setSelected(checkBoxes.stream().allMatch(CheckBox::isSelected));
            });
            
            // Add the CheckBox to GridPane in a 4-column layout
            lotsGridPane.add(checkBox, i % 4, i / 4);
         }
         
         // Ensure rbAll is selected if all checkboxes are checked
         rbAll.setSelected(!checkBoxes.isEmpty() && checkBoxes.stream().allMatch(CheckBox::isSelected));
      } else {
         System.out.println("sortedLotsArray is null in populateLotsGridPane.");
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




