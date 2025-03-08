package org.nikkatrading.wlp_automationv7;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.nikkatrading.wlp_automationv7.DB.GetAllocation;
import org.nikkatrading.wlp_automationv7.DB.GetConnection;
import org.nikkatrading.wlp_automationv7.GenerateWLP.ProcessWLP;
import org.nikkatrading.wlp_automationv7.Models.CBM.CBMGradeLevel;
import org.nikkatrading.wlp_automationv7.Models.SPI.SPIGradeLevel;
import org.nikkatrading.wlp_automationv7.Models.Table.SchoolList_TableModel;
import org.nikkatrading.wlp_automationv7.ReadExcel.ReadCBM;
import org.nikkatrading.wlp_automationv7.ReadExcel.ReadSPI;
import org.nikkatrading.wlp_automationv7.UI.LoadingUtil;
import org.nikkatrading.wlp_automationv7.UI.UI_Control;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainController {
   private Stage mainStage;  // Add this line
   
   /** @FXML is for the UI **/
   @FXML private ImageView lots_setup;
   @FXML private Text lotsDisp;
   
   @FXML private TextField inp_schoolID;
   
   @FXML private CheckBox cb_autoCheck;
   @FXML private Text txtSuccess;
   @FXML private Text txtFailed;
   
   @FXML private ChoiceBox cb_region;
   @FXML private ChoiceBox cb_division;
   
   @FXML private CheckBox cbContainer;
   
   @FXML private TableView<SchoolList_TableModel> schoolListTableView;
   @FXML private StackPane checkAllSchools;
   @FXML private TableColumn<SchoolList_TableModel, Boolean> colSelectSchool;
   @FXML private TableColumn<SchoolList_TableModel, String> colSchoolRegion;
   @FXML private TableColumn<SchoolList_TableModel, String> colSchoolDivision;
   @FXML private TableColumn<SchoolList_TableModel, Integer> colSchoolID;
   @FXML private TableColumn<SchoolList_TableModel, String> colSchoolName;
   @FXML private TableColumn<SchoolList_TableModel, String> colGradeLevel;
   @FXML private TableColumn<SchoolList_TableModel, Double> colCBM;
   
   @FXML private TableView<SchoolList_TableModel> selectedSchoolTableView;
   @FXML private StackPane deleteAllSelected;
   @FXML private TableColumn<SchoolList_TableModel, Boolean> colSelectedSchool;
   @FXML private TableColumn<SchoolList_TableModel, Integer> colSelectedNo;
   @FXML private TableColumn<SchoolList_TableModel, String> colSelectedRegion;
   @FXML private TableColumn<SchoolList_TableModel, String> colSelectedDivision;
   @FXML private TableColumn<SchoolList_TableModel, Integer> colSelectedSchoolID;
   @FXML private TableColumn<SchoolList_TableModel, String> colSelectedSchoolName;
   @FXML private TableColumn<SchoolList_TableModel, String> colSelectedGradeLevel;
   @FXML private TableColumn<SchoolList_TableModel, Double> colSelectedCBM;
   
   @FXML private TextField total_volume;
   
   @FXML private GridPane batchCbmGrid;
   
   @FXML private Button generate;
   /** End of @FXML **/
   
   
   /** Below is the data types **/
   // Store the sorted Lots from lowest to highest
   private String[] sortedLotsArray; // Store sorted lots for reuse
   // Store regions and their corresponding divisions
   private final Map<String, Set<String>> regionToDivisionsMap = new HashMap<>();
   
   private List<SchoolList_TableModel> tableDataList = new ArrayList<>(); // Store the sorted data for the table
   
   private List<SPIGradeLevel> spiGradeLevelList = new ArrayList<>(); // Store the CBM data
   private List<CBMGradeLevel> cbmGradeLevelList = new ArrayList<>(); // Store the CBM data
   
   private List<String> finalSelectedLot = new ArrayList<>();
   
   private final Set<Integer> selectedSchoolIDs = new HashSet<>();
   
   private final List<SchoolList_TableModel> selectedSchool = new ArrayList<>();
   
   private double TotalCBM = 0;
   
   // This data types is for the second table batch selection condition
   private int selectionBatch = 0;
   private final List<String> selectionColors = new ArrayList<>(List.of("#FFD700", "#87CEEB", "#90EE90", "#FF69B4", "#FFA07A")); // Gold, SkyBlue, LightGreen, HotPink, LightSalmon
   private final Map<Integer, Integer> selectedSchoolBatchMap = new HashMap<>(); // For the second table
   private final Map<Integer, Integer> unselectedSchoolBatchMap = new HashMap<>(); // For the first table
   private final Set<String> usedColors = new HashSet<>(selectionColors); // Track used colors
   private final Random random = new Random();
   
   private final Map<Integer, List<SchoolList_TableModel>> selectedBatchesMap = new HashMap<>();
   /** End of data types **/

   
   /// Add this method to set the main stage
   public void setMainStage(Stage mainStage) {
      this.mainStage = mainStage;
      initStage();  // Call initStage after mainStage is set
   }
   
   private void initStage() {
      // Register close handler for the main stage
      mainStage.setOnCloseRequest(event -> {});
   }
   
   /// Initialize Data
   @FXML
   private void initialize() {
      // Start monitoring database connection
      GetConnection.startConnectionMonitoring();
      
      // Start the loading animation when system is loaded
      executeGettingDataWithLoading();
      
      // Filter table based on the inputted SchoolID
      inp_schoolID.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
      
      checkAllSchools.setOnMouseClicked(event -> checkAllSchools());
      deleteAllSelected.setOnMouseClicked(event -> deleteAllSelected());
      
      generate.setOnAction(event -> {
         new ProcessWLP(mainStage, finalSelectedLot, selectedSchool, selectedBatchesMap);
         
         /*for (SchoolList_TableModel schoolListModel : selectedSchool) {
            System.out.println("- " + schoolListModel.getSchoolID() + " : " + schoolListModel.getSchoolName());
            
            for (SchoolGradeLevel_Model schoolGradeLvlModel : schoolListModel.getTableSchoolGradeLevelList()) {
               System.out.println("    >- Grade Level: " + schoolGradeLvlModel.getGradeLevel());
               
               for (SchoolLot_Model schoolLotModel : schoolGradeLvlModel.getTableSchoolLotList()) {
                  System.out.println("       -- " + schoolLotModel.getLotName().split(":")[0] + " <SPI: " + schoolLotModel.getSetPerItem() + ">" + " <CBM: " + schoolLotModel.getCbm() + ">");
               }
            }
         }*/
         
         /*for (Map.Entry<Integer, List<SchoolList_TableModel>> entry : selectedBatchesMap.entrySet()) {
            System.out.println("Batch " + entry.getKey() + ":");
            for (SchoolList_TableModel school : entry.getValue()) {
               System.out.println("  School ID: " + school.getSchoolID() + ", Name: " + school.getSchoolName());
            }
         }*/
      });
   }
   
   /// Show loading animation while gathering data
   public void executeGettingDataWithLoading() {
      String message = "This will take longer than \nusual due to data validation. \nThank you for your patience.";
//      String message = "Fetching Data, Please Wait...";
      LoadingUtil.showLoading(mainStage, message);
      
      Task<List<SchoolList_TableModel>> tableTask = new Task<>() {
         @Override
         protected List<SchoolList_TableModel> call() {
            if (sortedLotsArray == null || sortedLotsArray.length == 0) {
               System.out.println("Gathering raw data");
               gatherSchoolData();
            } else {
               if (finalSelectedLot == null || finalSelectedLot.isEmpty()) {
                  System.out.println("No Selected Lot");
                  System.out.println("Will use the unfiltered data");
                  tableDataList = GetAllocation.getAllocation();
               } else {
                  System.out.println("Selected LOT: " + finalSelectedLot.toString());
                  System.out.println("Will use filtered data");
                  tableDataList = GetAllocation.getSpecific_Allocation(finalSelectedLot, spiGradeLevelList, cbmGradeLevelList);
               }
            }
            
            return tableDataList;
         }
         
         @Override
         protected void succeeded() {
            LoadingUtil.closeLoading();
            Platform.runLater(() -> {
               updateUI();
               
               // Populate the choiceBoxes based on the retrieve data and will show the data in the table after filtering the data
               populateChoiceBoxes();
            });
         }
         
         @Override
         protected void failed() {
            Platform.runLater(() -> {
               LoadingUtil.closeLoading();
               Throwable exception = getException();
               System.err.println("Error: " + exception.getMessage());
               exception.printStackTrace();
               
               Alert alert = new Alert(Alert.AlertType.ERROR);
               alert.setTitle("Error");
               alert.setHeaderText(null);
               alert.setContentText("An error occurred while fetching help desk data: " + exception.getMessage());
               alert.showAndWait();
            });
         }
      };
      
      new Thread(tableTask).start();
   }
   
   /// Update the UI for latest Data
   private void updateUI() {
      // Class for controlling the UI
      new UI_Control(lots_setup, sortedLotsArray, finalSelectedLot, selectedLot -> {
//         System.out.println(selectedLot.toString());
         
         if (!Objects.equals(finalSelectedLot, selectedLot)) {
            finalSelectedLot = selectedLot;
            
            if (selectedLot.isEmpty()) {
               lotsDisp.setText("No Lot Selected");
            } else {
               String selectedLotString = String.join(", ", selectedLot);
               lotsDisp.setText("LOT: " + selectedLotString);
            }
            
            executeGettingDataWithLoading();
            System.out.println("Executed because selected lot changed");
         }
      });
   }
   
   /// This method is to gather necessary data.
   private void gatherSchoolData() {
      // Store the Set Per Item data
      spiGradeLevelList = new ReadSPI().spiGradeLevelList;
      cbmGradeLevelList = new ReadCBM().cbmGradeLevelList;
      
      // Collect lots from the database
      tableDataList = GetAllocation.getAllocation();
      populateChoiceBoxes();
      
      Set<String> gatheredLots = GetAllocation.gatheredLots;
      
      // Convert to list
      List<String> sortedLots = new ArrayList<>(gatheredLots);
      
      // Sort by extracting lot number
      sortedLots.sort(Comparator.comparingInt(this::extractLotNumber));
      
      // Store sorted lots in an array for reuse
      sortedLotsArray = sortedLots.toArray(new String[0]);
      
      /// Testing for school Allocation
      /*for (SchoolList_TableModel schoolModel : schoolList) {
         System.out.println("- " + schoolModel.getRegion() + " > " + schoolModel.getDivision() +
                 " > [" + schoolModel.getSchoolID() + "] " + schoolModel.getSchoolName());
         
         for (SchoolGradeLevel_Model schoolGradeLevel : schoolModel.getTableSchoolGradeLevelList()) {
            System.out.println("   - Grade Level: " + schoolGradeLevel.getGradeLevel());
            
            for (SchoolLot_Model schoolLot : schoolGradeLevel.getTableSchoolLotList()) {
               System.out.println("    - " + schoolLot.getLotName() + " >-SPI: " + schoolLot.getSetPerItem() + " >-CBM: " + schoolLot.getCbm());
            }
         }
         System.out.println(); // Add spacing for better readability between schools
      }*/
      
      /// Test for Delivered School
      /*for (DeliveredSchoolModel deliveredSchoolModel : deliveredSchoolList) {
         System.out.println("===============================================");
         System.out.println(" Region    : " + deliveredSchoolModel.getRegionName());
         System.out.println(" Division  : " + deliveredSchoolModel.getDivisionName());
         System.out.println(" School ID : " + deliveredSchoolModel.getSchoolId());
         System.out.println(" School    : " + deliveredSchoolModel.getSchoolName());
         System.out.println(" ----------------------------------------------- ");
         System.out.println(" Lot 6     : " + deliveredSchoolModel.getLot6());
         System.out.println(" Lot 7     : " + deliveredSchoolModel.getLot7());
         System.out.println(" Lot 8     : " + deliveredSchoolModel.getLot8());
         System.out.println(" Lot 9     : " + deliveredSchoolModel.getLot9());
         System.out.println(" Lot 10    : " + deliveredSchoolModel.getLot10());
         System.out.println(" Lot 11    : " + deliveredSchoolModel.getLot11());
         System.out.println(" Lot 13    : " + deliveredSchoolModel.getLot13());
         System.out.println(" Lot 14    : " + deliveredSchoolModel.getLot14());
         System.out.println("===============================================");
         System.out.println();
      }*/
      
      /// Test for Generated School
     /* for (GeneratedSchoolModel generatedSchoolModel : generatedSchoolList) {
         System.out.println("===============================================");
         System.out.println(" Batch No.    : " + generatedSchoolModel.getBatchNo());
         System.out.println(" Created At  : " + generatedSchoolModel.getCreatedAt());
         System.out.println(" School ID : " + generatedSchoolModel.getSchoolId());
         System.out.println(" ----------------------------------------------- ");
         System.out.println(" Lot 6     : " + generatedSchoolModel.getLot6());
         System.out.println(" Lot 7     : " + generatedSchoolModel.getLot7());
         System.out.println(" Lot 8     : " + generatedSchoolModel.getLot8());
         System.out.println(" Lot 9     : " + generatedSchoolModel.getLot9());
         System.out.println(" Lot 10    : " + generatedSchoolModel.getLot10());
         System.out.println(" Lot 11    : " + generatedSchoolModel.getLot11());
         System.out.println(" Lot 13    : " + generatedSchoolModel.getLot13());
         System.out.println(" Lot 14    : " + generatedSchoolModel.getLot14());
         System.out.println("===============================================");
         System.out.println();
      }*/
      
      /// Test for SPI
      /*for (SPIGradeLevel spiGradeLevel : spiGradeLevelList) {
         System.out.println("Grade Level: " + spiGradeLevel.getGradeLevel());
         
         for (SPILot spiLot : spiGradeLevel.getSpiLotList()) {
            System.out.println("  ├── " + spiLot.getLot());
            
            for (SPIItems spiItems : spiLot.getItemsList()) {
               System.out.println("  │   ├── Item: " + spiItems.getItem());
               System.out.println("  │   │   └── Quantity: " + spiItems.getQty());
            }
         }
         System.out.println(); // Add spacing for better readability
      }*/
      
      /// Test for CBM
      /*for (CBMGradeLevel cbmGradeLevel : cbmGradeLevelList) {
         System.out.println("Grade Level: " + cbmGradeLevel.getGradeLevel());
         
         for (CBMLot cbmLot : cbmGradeLevel.getCbmLotList()) {
            System.out.println("   ├─ Lot: " + cbmLot.getLot());
            System.out.println("   └─ CBM: " + cbmLot.getCbm());
         }
         
         System.out.println(); // Add space for better separation between grade levels
      }*/
   }
   
   /// Extracts the lot number from a given string
   private int extractLotNumber(String lot) {
      Pattern pattern = Pattern.compile("LOT (\\d+)");
      Matcher matcher = pattern.matcher(lot);
      return matcher.find() ? Integer.parseInt(matcher.group(1)) : Integer.MAX_VALUE;
   }
   
   /// Method to Populate the ChoiceBoxes with data
   private void populateChoiceBoxes() {
      // Clear existing data
      regionToDivisionsMap.clear();
      
      // Populate the map with regions and their respective divisions
      Set<String> allDivisions = new HashSet<>();
      for (SchoolList_TableModel school : tableDataList) {
         regionToDivisionsMap
                 .computeIfAbsent(school.getRegion(), k -> new HashSet<>())
                 .add(school.getDivision());
         allDivisions.add(school.getDivision());
      }
      
      // Create options list
      List<String> regionOptions = new ArrayList<>();
      regionOptions.add("All");
      regionOptions.addAll(regionToDivisionsMap.keySet());
      
      List<String> divisionOptions = new ArrayList<>();
      divisionOptions.add("All");
      divisionOptions.addAll(allDivisions);
      
      // Ensure UI updates run on JavaFX thread
      Platform.runLater(() -> {
         cb_region.getItems().setAll(regionOptions);
         cb_division.getItems().setAll(divisionOptions);
         
         // Set default selection
         cb_region.getSelectionModel().select("All");
         cb_division.getSelectionModel().select("All");
      });
      
      // Handle region selection to update divisions dynamically and filter table
      cb_region.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
         if (newVal != null) {
            Platform.runLater(() -> {
               String selectedDivision = cb_division.getSelectionModel().getSelectedItem().toString();
               
               // Reset cb_division when "All" is selected
               if ("All".equals(newVal)) {
                  cb_division.getItems().setAll(divisionOptions);
                  cb_division.getSelectionModel().select("All");
               } else {
                  List<String> filteredDivisions = new ArrayList<>();
                  filteredDivisions.add("All");
                  filteredDivisions.addAll(regionToDivisionsMap.getOrDefault(newVal, Collections.emptySet()));
                  cb_division.getItems().setAll(filteredDivisions);
                  
                  // Restore previous selection only if it exists in the updated list
                  if (selectedDivision != null && cb_division.getItems().contains(selectedDivision)) {
                     cb_division.getSelectionModel().select(selectedDivision);
                  } else {
                     cb_division.getSelectionModel().select("All");
                  }
               }
               
               // Apply filter when region changes
               filterTable();
            });
         }
      });
      
      // Handle division selection to update the region and filter table
      cb_division.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
         if (newVal != null) {
            Platform.runLater(() -> {
               if (!"All".equals(newVal)) {
                  for (Map.Entry<String, Set<String>> entry : regionToDivisionsMap.entrySet()) {
                     if (entry.getValue().contains(newVal)) {
                        cb_region.getSelectionModel().select(entry.getKey());
                        break;
                     }
                  }
               }
               
               // Apply filter when division changes
               filterTable();
            });
         }
      });
   }
   
   /// Method to Filter the Table based on ChoiceBox Selections
   private void filterTable() {
      String selectedRegion = cb_region.getSelectionModel().getSelectedItem().toString();
      String selectedDivision = cb_division.getSelectionModel().getSelectedItem().toString();
      String schoolIDInput = inp_schoolID.getText().trim();
      
      // Extract 6-digit school IDs from the input
      List<Integer> schoolIDs = new ArrayList<>();
      for (int i = 0; i <= schoolIDInput.length() - 6; i += 6) {
         try {
            int schoolID = Integer.parseInt(schoolIDInput.substring(i, i + 6));
            schoolIDs.add(schoolID);
         } catch (NumberFormatException e) {
            e.printStackTrace(); // Handle invalid numbers gracefully
         }
      }
      
      // Filter the table data based on the selected criteria
      List<SchoolList_TableModel> filteredList = tableDataList.stream()
              .filter(school -> "All".equals(selectedRegion) || school.getRegion().equals(selectedRegion))
              .filter(school -> "All".equals(selectedDivision) || school.getDivision().equals(selectedDivision))
              .filter(school -> schoolIDs.isEmpty() || schoolIDs.contains(school.getSchoolID())) // Filter by multiple School IDs
              .toList();
      
      // Identify school IDs that were not found
      List<Integer> notFoundIDs = schoolIDs.stream()
              .filter(id -> filteredList.stream().noneMatch(school -> school.getSchoolID() == id))
              .toList();
      
      // Reset all selections before applying auto-check
      for (SchoolList_TableModel school : tableDataList) {
         school.setSelect(false);
      }
      
      // Auto-select filtered schools if cb_autoCheck is checked and school IDs exist
      selectedSchool.clear();
      selectedSchoolIDs.clear();
      TotalCBM = 0; // Reset total CBM
      
      if (cb_autoCheck.isSelected() && !filteredList.isEmpty()) {
         for (SchoolList_TableModel school : filteredList) {
            school.setSelect(true); // Auto-select only filtered schools
            selectedSchool.add(school);
            selectedSchoolIDs.add(school.getSchoolID());
            TotalCBM += school.getCbm();
         }
         
         // Update TotalCBM in UI
         total_volume.setText(String.valueOf(Math.round(TotalCBM * 100.0) / 100.0));
      }
      
      // Populate the first table with the filtered list
      populateTable(schoolListTableView, filteredList, false);
      
      // Update the second table with selected schools (if auto-checked)
      populateTable(selectedSchoolTableView, selectedSchool, true);
      
      // Show success or failed message
      if (!schoolIDs.isEmpty()) {
         if (!filteredList.isEmpty()) {
            txtSuccess.setVisible(true);
            txtSuccess.setText("Search Successful!");
            txtFailed.setVisible(false);
         }
         
         if (!notFoundIDs.isEmpty()) {
            txtFailed.setVisible(true);
            txtFailed.setText("ID(s) not found: " + notFoundIDs);
         } else {
            txtFailed.setVisible(false);
         }
      } else {
         txtSuccess.setVisible(false);
         txtFailed.setVisible(false);
      }
   }
   
   /// Generic method to populate a TableView (First and Second)
   private void populateTable(TableView<SchoolList_TableModel> tableView, List<SchoolList_TableModel> tableDataList, boolean isSelectedTable) {
      
      ObservableList<SchoolList_TableModel> tableAllocation = FXCollections.observableArrayList(tableDataList);
      tableView.setItems(tableAllocation);
      
      // Select the appropriate batch map
      Map<Integer, Integer> schoolBatchMap = isSelectedTable ? selectedSchoolBatchMap : unselectedSchoolBatchMap;
      
      // Setup checkbox column
      TableColumn<SchoolList_TableModel, Boolean> selectColumn = isSelectedTable ? colSelectedSchool : colSelectSchool;
      setupCheckboxColumn(selectColumn);
      
      // Setup common columns
      setupColumn(isSelectedTable ? colSelectedRegion : colSchoolRegion, SchoolList_TableModel::getRegion);
      setupColumn(isSelectedTable ? colSelectedDivision : colSchoolDivision, SchoolList_TableModel::getDivision);
      setupColumn(isSelectedTable ? colSelectedSchoolID : colSchoolID, SchoolList_TableModel::getSchoolID);
      setupColumn(isSelectedTable ? colSelectedSchoolName : colSchoolName, SchoolList_TableModel::getSchoolName);
      setupColumn(isSelectedTable ? colSelectedGradeLevel : colGradeLevel, model -> String.join(", ", model.getGradeLevels()));
      setupColumn(isSelectedTable ? colSelectedCBM : colCBM, model -> Math.round(model.getCbm() * 1000.0) / 1000.0);
      
      if (isSelectedTable) {
         setupAutoNumberingColumn(colSelectedNo);
      }
      
      tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
      
      // Apply row coloring logic based on the correct map
      tableView.setRowFactory(tv -> new TableRow<>() {
         @Override
         protected void updateItem(SchoolList_TableModel item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
               setStyle("");
            } else {
               int schoolID = item.getSchoolID();
               if (schoolBatchMap.containsKey(schoolID)) {
                  int batchIndex = schoolBatchMap.get(schoolID);
                  setStyle("-fx-background-color: " + selectionColors.get(batchIndex) + ";");
               } else {
                  setStyle("");
               }
            }
         }
      });
      
      // Apply selection behavior only if this is the second table
      if (isSelectedTable) {
         tableView.setOnKeyPressed(event -> {
            if (cbContainer.isSelected() && event.getCode() == KeyCode.ESCAPE) {
               // Clear selection and batch data
               clearBatches();
               selectionBatch = 0;
               tableView.getSelectionModel().clearSelection();
               selectedSchoolBatchMap.clear();
               tableView.refresh(); // Refresh the table to remove highlighting
               
            }
         });
         
         tableView.setOnMousePressed(event -> {
            if (cbContainer.isSelected() && event.isControlDown()) {
               selectionBatch++;
               getNextColor(); // Ensure a new color is available
            }
         });
         
         tableView.setOnMouseDragged(event -> {
            if (cbContainer.isSelected() && event.isControlDown()) {
               Node node = event.getPickResult().getIntersectedNode();
               
               while (node != null && !(node instanceof TableRow)) {
                  node = node.getParent();
               }
               
               if (node instanceof TableRow<?> row) {
                  SchoolList_TableModel item = (SchoolList_TableModel) row.getItem();
                  if (item != null) {
                     int schoolID = item.getSchoolID();
                     
                     // Track drag direction
                     double previousY = (double) tableView.getProperties().getOrDefault("previousY", event.getSceneY());
                     double currentY = event.getSceneY();
                     
                     if (currentY > previousY) {
//                        System.out.println("Drag Down");
                        // Dragging DOWN: ADD to batch
                        if (!selectedSchoolBatchMap.containsKey(schoolID)) {
                           selectedSchoolBatchMap.put(schoolID, selectionBatch);
                           addToBatch(item);
                           row.setStyle("-fx-background-color: " + selectionColors.get(selectionBatch) + ";");
                        }
                     } else if (currentY < previousY) {
//                        System.out.println("Drag Up");
                        // Dragging UP: REMOVE from batch
                        if (selectedSchoolBatchMap.containsKey(schoolID)) {
                           removeFromBatch(item);
                           row.setStyle(""); // Reset row color
                        }
                     }
                     
                     // Update stored mouse position
                     tableView.getProperties().put("previousY", currentY);
                  }
               }
            }
         });
      }
   }
   
   /// Method to configure a checkbox column
   private void setupCheckboxColumn(TableColumn<SchoolList_TableModel, Boolean> column) {
      column.setCellValueFactory(cellData -> cellData.getValue().selectProperty());
      column.setCellFactory(param -> new TableCell<>() {
         private final CheckBox checkBox = new CheckBox();
         
         {
            checkBox.setDisable(finalSelectedLot == null);
            checkBox.setOnAction(event -> {
               SchoolList_TableModel item = getTableView().getItems().get(getIndex());
               item.setSelect(checkBox.isSelected());
               updateSelectedSchoolID(item, checkBox.isSelected());
            });
         }
         
         @Override
         protected void updateItem(Boolean item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
               setGraphic(null);
            } else {
               checkBox.setSelected(item);
               checkBox.setDisable(finalSelectedLot == null);
               setGraphic(checkBox);
            }
         }
      });
   }
   
   /// Generic method to set up a column
   private <T> void setupColumn(TableColumn<SchoolList_TableModel, T> column, Function<SchoolList_TableModel, T> extractor) {
      column.setCellValueFactory(cellData -> new SimpleObjectProperty<>(extractor.apply(cellData.getValue())));
   }
   
   /// Setup auto-numbering column for the second table
   private void setupAutoNumberingColumn(TableColumn<SchoolList_TableModel, Integer> column) {
      column.setCellFactory(param -> new TableCell<>() {
         @Override
         protected void updateItem(Integer item, boolean empty) {
            super.updateItem(item, empty);
            setText(empty ? null : String.valueOf(getIndex() + 1)); // 1-based index
         }
      });
   }
   
   private void checkAllSchools() {
      // Get the currently displayed (filtered) schools
      List<SchoolList_TableModel> displayedSchools = new ArrayList<>(schoolListTableView.getItems());
      
      // Toggle logic: If all displayed schools are selected, uncheck all. Otherwise, select all.
      boolean selectAll = displayedSchools.stream().anyMatch(school -> !school.isSelect());
      
      selectedSchool.clear();
      selectedSchoolIDs.clear();
      TotalCBM = 0; // Reset total CBM
      
      for (SchoolList_TableModel school : displayedSchools) {
         school.setSelect(selectAll); // Only update schools in the current filtered list
         
         if (selectAll) {
            selectedSchool.add(school);
            selectedSchoolIDs.add(school.getSchoolID());
            TotalCBM += school.getCbm();
         }
      }
      
      // Update UI
      total_volume.setText(String.valueOf(Math.round(TotalCBM * 100.0) / 100.0));
      
      // Refresh tables
      schoolListTableView.refresh(); // Ensures checkboxes update
      populateTable(selectedSchoolTableView, selectedSchool, true);
   }
   
   private void deleteAllSelected() {
      // Deselect all currently selected schools
      for (SchoolList_TableModel school : selectedSchool) {
         school.setSelect(false);
      }
      
      selectedSchool.clear();
      selectedSchoolIDs.clear();
      TotalCBM = 0; // Reset total CBM
      
      // Update UI
      total_volume.setText(String.valueOf(TotalCBM));
      
      // Refresh the first table (to reflect checkbox updates)
      schoolListTableView.refresh();
      
      // Refresh the second table (selected schools)
      populateTable(selectedSchoolTableView, selectedSchool, true);
   }
   
   /// Helper for processing the selected school
   private void updateSelectedSchoolID(SchoolList_TableModel tableList, boolean isSelected) {
      if (isSelected) {
         selectedSchoolIDs.add(tableList.getSchoolID());
         TotalCBM += tableList.getCbm();
         
         selectedSchool.add(tableList);
         
      } else {
         selectedSchoolIDs.remove(tableList.getSchoolID());
         TotalCBM -= tableList.getCbm();
         
         selectedSchool.remove(tableList);
         
         // Check if no schools remain selected
         if (selectedSchoolIDs.isEmpty()) {
            TotalCBM = 0; // Reset TotalCBM when no schools are selected
         }
      }
      
      // Format TotalCBM to two decimal places and update the text
      total_volume.setText(String.valueOf(Math.round(TotalCBM * 100.0) / 100.0));
//      populateSecondTable(selectedSchool);
      populateTable(selectedSchoolTableView, selectedSchool, true); // Second Table
   }
   
   private void addToBatch(SchoolList_TableModel school) {
      int batchIndex = selectionBatch;
      selectedBatchesMap.putIfAbsent(batchIndex, new ArrayList<>());
      
      List<SchoolList_TableModel> batchList = selectedBatchesMap.get(batchIndex);
      
      if (!batchList.contains(school)) {
         batchList.add(school);
         updateBatchCbmDisplay(); // Update GridPane
      }
   }
   
   private void removeFromBatch(SchoolList_TableModel school) {
      int schoolID = school.getSchoolID();
      if (selectedSchoolBatchMap.containsKey(schoolID)) {
         int batch = selectedSchoolBatchMap.get(schoolID);
         selectedBatchesMap.getOrDefault(batch, new ArrayList<>()).remove(school);
         selectedSchoolBatchMap.remove(schoolID);
         
         if (selectedBatchesMap.get(batch).isEmpty()) {
            selectedBatchesMap.remove(batch);
         }
         
         updateBatchCbmDisplay(); // Update GridPane
      }
   }
   
   private void clearBatches() {
      selectedBatchesMap.clear();
      selectedSchoolBatchMap.clear();
      batchCbmGrid.getChildren().clear(); // Clear GridPane
   }
   
   private void updateBatchCbmDisplay() {
      batchCbmGrid.getChildren().clear(); // Clear previous entries
      batchCbmGrid.getColumnConstraints().clear(); // Ensure uniform columns
      batchCbmGrid.getRowConstraints().clear(); // Ensure uniform rows
      
      int colIndex, rowIndex;
      int maxColumns = 10, maxRows = 2;
      int totalCells = maxColumns * maxRows;
      double cellSpacing = 10; // Space between cells
      
      // Adjust the column and row constraints to ensure uniform sizing
      for (int i = 0; i < maxColumns; i++) {
         ColumnConstraints colConstraints = new ColumnConstraints();
         colConstraints.setPercentWidth(100.0 / maxColumns); // Distribute columns evenly
         batchCbmGrid.getColumnConstraints().add(colConstraints);
      }
      
      for (int i = 0; i < maxRows; i++) {
         RowConstraints rowConstraints = new RowConstraints();
         rowConstraints.setPercentHeight(100.0 / maxRows); // Distribute rows evenly
         batchCbmGrid.getRowConstraints().add(rowConstraints);
      }
      
      int index = 0;
      for (Map.Entry<Integer, List<SchoolList_TableModel>> entry : selectedBatchesMap.entrySet()) {
         if (index >= totalCells) break; // Ensure it does not exceed the grid size
         
         int batchIndex = entry.getKey();
         double totalCBM = entry.getValue().stream()
                 .mapToDouble(SchoolList_TableModel::getCbm)
                 .sum();
         
         // Color Box
         Rectangle colorBox = new Rectangle(20, 20, Color.web(selectionColors.get(batchIndex)));
         
         // CBM Label
         Label cbmLabel = new Label(String.format("%.3f", totalCBM)); // Formats to 3 decimal places
         cbmLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: black;");
         
         // Stack elements inside an HBox
         HBox batchDisplay = new HBox(5, colorBox, cbmLabel);
         batchDisplay.setAlignment(Pos.CENTER);
         
         // Add spacing between cells
         GridPane.setMargin(batchDisplay, new Insets(cellSpacing));
         
         // Calculate column and row (column-wise filling)
         colIndex = index / maxRows; // Fill columns first
         rowIndex = index % maxRows;
         
         // Add to GridPane
         batchCbmGrid.add(batchDisplay, colIndex, rowIndex);
         
         index++; // Move to the next position
      }
   }
   
   ///  Helper for creating random table highlight colors without repeating
   private void getNextColor() {
      if (selectionBatch < selectionColors.size()) {
         return;
      }
      
      String newColor;
      do {
         // Generate RGB values within the light range (avoid very dark colors)
         int red = 150 + random.nextInt(106);   // 150–255
         int green = 150 + random.nextInt(106); // 150–255
         int blue = 150 + random.nextInt(106);  // 150–255
         
         newColor = String.format("#%02X%02X%02X", red, green, blue); // Convert to hex
      } while (usedColors.contains(newColor));
      
      selectionColors.add(newColor);
      usedColors.add(newColor);
   }
   
   
   
   /// Listener for lot selections
   public interface LotsSetupCallback {
      void onLotsSelected(List<String> selectedLot);
   }
   /// Call this method when UI is closing
   public void onClose(Stage stage) {
      stage.setOnCloseRequest(event -> {
         System.out.println("Application is closing...");
         GetConnection.shutdownMonitoring(); // Stop monitoring when UI closes
      });
   }
}