package org.nikkatrading.wlp_automationv7;

import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.nikkatrading.wlp_automationv7.DB.GetAllocation;
import org.nikkatrading.wlp_automationv7.DB.GetConnection;
import org.nikkatrading.wlp_automationv7.Models.CBM.CBMGradeLevel;
import org.nikkatrading.wlp_automationv7.Models.CBM.CBMLot;
import org.nikkatrading.wlp_automationv7.Models.DeliveredSchoolModel;
import org.nikkatrading.wlp_automationv7.Models.GeneratedSchoolModel;
import org.nikkatrading.wlp_automationv7.Models.SPI.SPIGradeLevel;
import org.nikkatrading.wlp_automationv7.Models.SPI.SPIItems;
import org.nikkatrading.wlp_automationv7.Models.SPI.SPILot;
import org.nikkatrading.wlp_automationv7.Models.Table.SchoolGradeLevel_Model;
import org.nikkatrading.wlp_automationv7.Models.Table.SchoolList_TableModel;
import org.nikkatrading.wlp_automationv7.Models.Table.SchoolLot_Model;
import org.nikkatrading.wlp_automationv7.ReadExcel.ReadCBM;
import org.nikkatrading.wlp_automationv7.ReadExcel.ReadSPI;
import org.nikkatrading.wlp_automationv7.UI.LoadingUtil;
import org.nikkatrading.wlp_automationv7.UI.UI_Control;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MainController {
   private Stage mainStage;  // Add this line
   
   /* @FXML is for the UI */
   @FXML
   private ImageView lots_setup;
   @FXML
   private Text lotsDisp;
   
   @FXML private TableView<SchoolList_TableModel> sclList_table;
   @FXML private ImageView schoolListTable_checkAll;
   @FXML private TableColumn<SchoolList_TableModel, Boolean> selectSchool;
   @FXML private TableColumn<SchoolList_TableModel, String> schoolListTable_region;
   @FXML private TableColumn<SchoolList_TableModel, String> schoolListTable_division;
   @FXML private TableColumn<SchoolList_TableModel, Integer> schoolListTable_schoolID;
   @FXML private TableColumn<SchoolList_TableModel, String> schoolListTable_schoolName;
   @FXML private TableColumn<SchoolList_TableModel, String> schoolListTable_gradeLvl;
   @FXML private TableColumn<SchoolList_TableModel, Double> schoolListTable_cbm;
   
   /** Below is the data types **/
   // Store the sorted Lots from lowest to highest
   private String[] sortedLotsArray; // Store sorted lots for reuse
   
   private List<SchoolList_TableModel> tableDataList = new ArrayList<>(); // Store the sorted data for the table
   
   private List<CBMGradeLevel> cbmGradeLevelList; // Store the CBM data
   
   private List<SchoolList_TableModel> schoolList; // Store the school data
   private List<DeliveredSchoolModel> deliveredSchoolList; // Store the delivered school data
   private List<GeneratedSchoolModel> generatedSchoolList; // Store the generated school data
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
      executeGettingDataWithLoading(null);
   }
   
   /// Show loading animation while gathering data
   public void executeGettingDataWithLoading(List<String> selectedLot) {
      LoadingUtil.showLoading(mainStage, "Fetching Data, Please Wait...");
      
      Task<List<SchoolList_TableModel>> tableTask = new Task<>() {
         @Override
         protected List<SchoolList_TableModel> call() {
            if (schoolList == null) {
               gatherSchoolData();
            } else {
               if (selectedLot == null) {
                  tableDataList = schoolList;
               } else {
                  tableDataList = sortSchoolData(selectedLot);
               }
            }
            return tableDataList;
         }
         
         @Override
         protected void succeeded() {
            LoadingUtil.closeLoading();
            updateUI();
            showAllocationInTable(tableDataList, selectedLot);
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
      new UI_Control(lots_setup, sortedLotsArray, selectedLot -> {
         // Handle the selected lots here
         if (selectedLot == null || selectedLot.isEmpty()) {
            lotsDisp.setText("No Lot Selected");
         } else {
            // Join the list elements into a single string without brackets
            String selectedLotString = String.join(", ", selectedLot);
            lotsDisp.setText("LOT: " + selectedLotString);
            
            executeGettingDataWithLoading(selectedLot);
         }
      });
   }
   
   /// This method is to gather necessary data.
   private void gatherSchoolData() {
      // Store the Set Per Item data
      List<SPIGradeLevel> spiGradeLevelList = new ReadSPI().spiGradeLevelList;
      cbmGradeLevelList = new ReadCBM().cbmGradeLevelList;
      
      // Collect lots from the database
      GetAllocation.fetchAllocation(spiGradeLevelList, cbmGradeLevelList);
      schoolList = GetAllocation.schoolListTable;
      deliveredSchoolList = GetAllocation.deliveredSchoolList;
      generatedSchoolList = GetAllocation.generatedSchoolList;
      
      Set<String> gatheredLots = GetAllocation.gatheredLots;
      
      // Convert to list
      List<String> sortedLots = new ArrayList<>(gatheredLots);
      
      // Sort by extracting lot number
      sortedLots.sort(Comparator.comparingInt(this::extractLotNumber));
      
      // Store sorted lots in an array for reuse
      sortedLotsArray = sortedLots.toArray(new String[0]);
      
      tableDataList = schoolList;
      
      /// Testing for school Allocation*/
      for (SchoolList_TableModel schoolModel : schoolList) {
         System.out.println("- " + schoolModel.getRegion() + " > " + schoolModel.getDivision() +
                 " > [" + schoolModel.getSchoolID() + "] " + schoolModel.getSchoolName());
         
         for (SchoolGradeLevel_Model schoolGradeLevel : schoolModel.getTableSchoolGradeLevelList()) {
            System.out.println("   ├── Grade Level: " + schoolGradeLevel.getGradeLevel());
            
            for (SchoolLot_Model schoolLot : schoolGradeLevel.getTableSchoolLotList()) {
               System.out.println("   │   ├── Lot: " + schoolLot.getLotName());
               System.out.println("   │   │   ├── Set Per Item: " + schoolLot.getSetPerItem());
               System.out.println("   │   │   └── CBM: " + schoolLot.getCbm());
            }
         }
         System.out.println(); // Add spacing for better readability between schools
      }
      
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
      /*for (GeneratedSchoolModel generatedSchoolModel : generatedSchoolList) {
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
   
   /// Method to display the allocation in the table
   private void showAllocationInTable(List<SchoolList_TableModel> tableDataList, List<String> selectedLot) {
      ObservableList<SchoolList_TableModel> tableAllocation = FXCollections.observableArrayList(tableDataList);
      sclList_table.setItems(tableAllocation);
      
      // Set up the cell value factory for the checkbox column
      selectSchool.setCellValueFactory(cellData -> cellData.getValue().selectProperty());
      selectSchool.setCellFactory(new Callback<>() {
         @Override
         public TableCell<SchoolList_TableModel, Boolean> call(TableColumn<SchoolList_TableModel, Boolean> param) {
            return new TableCell<>() {
               private final CheckBox checkBox = new CheckBox();
               {
                  checkBox.setDisable(selectedLot == null);
                  checkBox.setOnAction(event -> {
                     SchoolList_TableModel item = getTableView().getItems().get(getIndex());
                     item.setSelect(checkBox.isSelected());
//                     updateSelectedSchoolID(item, checkBox.isSelected());
                  });
               }
               @Override
               protected void updateItem(Boolean item, boolean empty) {
                  super.updateItem(item, empty);
                  if (empty || item == null) {
                     setGraphic(null);
                  } else {
                     checkBox.setSelected(item);
                     checkBox.setDisable(selectedLot == null);
                     setGraphic(checkBox);
                  }
               }
            };
         }
      });
      
      // Set up the cell value factories for the other columns
      schoolListTable_region.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getRegion()));
      schoolListTable_division.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDivision()));
      schoolListTable_schoolID.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getSchoolID()).asObject());
      schoolListTable_schoolName.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getSchoolName()));
      schoolListTable_gradeLvl.setCellValueFactory(cellData -> new SimpleStringProperty(String.join(", ", cellData.getValue().getGradeLevels())));
      schoolListTable_cbm.setCellValueFactory(cellData -> new SimpleDoubleProperty(Math.round(cellData.getValue().getCbm() * 100.0) / 100.0).asObject());
//      schoolListTable_cbm.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getCbm()).asObject());
   }
   
   
   /** Put utilities below **/

   /// Method for sorting and filtering the schools based on the selected lots
   private List<SchoolList_TableModel> sortSchoolData(List<String> selectedLot) {
      
      List<SchoolList_TableModel> tableDataList = new ArrayList<>();
      
      // Create a lookup map for quick access to delivered school data by schoolId
      Map<Integer, DeliveredSchoolModel> deliveredMap = deliveredSchoolList.stream()
              .collect(Collectors.toMap(DeliveredSchoolModel::getSchoolId, d -> d));
      
      // Create a lookup map for quick access to generated school data by schoolId
      Map<Integer, GeneratedSchoolModel> generatedMap = generatedSchoolList.stream()
              .collect(Collectors.toMap(GeneratedSchoolModel::getSchoolId, g -> g));
      
      // Loop through the school list and filter based on th   e selected lot criteria
      for (SchoolList_TableModel schoolModel : schoolList) {
         int schoolId = schoolModel.getSchoolID();
         // Reset CBM before adding a new value
         schoolModel.setCbm(0); // Ensure there's a setter method in SchoolList_TableModel
         
         // Check if the school exists in the delivered map
         if (deliveredMap.containsKey(schoolId)) {
            DeliveredSchoolModel delivered = deliveredMap.get(schoolId);
            
            // Determine if the school has a valid delivered lot
            List<String> nullDeliveredLots = getDeliveredLotsWithNullValues(selectedLot, delivered);
            
            if (!nullDeliveredLots.isEmpty()) {
               // If the school exists in the generated map, check its lots
               if (generatedMap.containsKey(schoolId)) {
                  GeneratedSchoolModel generated = generatedMap.get(schoolId);
                  
                  // Determine if the school has a valid generated lot
                  List<String> nullGeneratedLots = getGeneratedLotsWithNullValues(selectedLot, generated);
                  
                  // If both generated and delivered lots meet the criteria, add to list
                  if (!nullGeneratedLots.isEmpty()) {
                     // Get the total cbm
                     double cbm = getTotalCBM(selectedLot, schoolModel, nullDeliveredLots, nullGeneratedLots);
                     
                     // Add the cbm before the transfer of the data
                     schoolModel.addCbm(cbm);
                     tableDataList.add(schoolModel);
                  }
               } else {
                  // Get the total cbm
                  double cbm = getTotalCBM(selectedLot, schoolModel, nullDeliveredLots, null);
                  
                  // Add the cbm before the transfer of the data
                  schoolModel.addCbm(cbm);
                  tableDataList.add(schoolModel);
               }
            }
         } else {
            // If school is not in delivered map, check if it exists in generated map
            if (generatedMap.containsKey(schoolId)) {
               GeneratedSchoolModel generated = generatedMap.get(schoolId);
               
               // Determine if the school has a valid generated lot
               List<String> nullGeneratedLots = getGeneratedLotsWithNullValues(selectedLot, generated);
               
               // If both generated and delivered lots meet the criteria, add to list
               if (!nullGeneratedLots.isEmpty()) {
                  // Get the total cbm
                  double cbm = getTotalCBM(selectedLot, schoolModel, null, nullGeneratedLots);
                  
                  // Add the cbm before the transfer of the data
                  schoolModel.addCbm(cbm);
                  tableDataList.add(schoolModel);
               }
            } else {
               // Get the total cbm
               double cbm = getTotalCBM(selectedLot, schoolModel, null, null);
               
               // Add the cbm before the transfer of the data
               schoolModel.addCbm(cbm);
               tableDataList.add(schoolModel);
            }
         }
      }
      return tableDataList;
   }
   
   /// Helper to get the total CBM of the school based on the selected lot and the availability of the school lots
   private double getTotalCBM(List<String> selectedLot, SchoolList_TableModel schoolModel, List<String> nullDeliveredLots, List<String> nullGeneratedLots) {
      double cbm = 0; // Initialize CBM to accumulate the total volume
      
      // Retrieve the list of grade levels associated with the school
      List<SchoolGradeLevel_Model> schoolGradeLevelList = schoolModel.getTableSchoolGradeLevelList();
      
      // Iterate through each grade level in the school's list
      for (SchoolGradeLevel_Model gradeLevelModel : schoolGradeLevelList) {
         // Iterate through each lot associated with the current grade level
         for (SchoolLot_Model schoolLotModel : gradeLevelModel.getTableSchoolLotList()) {
            // Extract the lot number from the lot name
            int lotNum = extractLotNumber(schoolLotModel.getLotName());
            
            // Check if nullDeliveredLots is not null (meaning there's a delivered lot to compare)
            if (nullDeliveredLots != null) {
               // Check if nullGeneratedLots is also not null (meaning there are generated lots to compare)
               if (nullGeneratedLots != null) {
                  // If both delivered and generated lots contain this lot number, add its CBM
                  if (nullDeliveredLots.contains(String.valueOf(lotNum)) && nullGeneratedLots.contains(String.valueOf(lotNum))) {
                     cbm += schoolLotModel.getCbm();
                  }
               } else {
                  // If only delivered lots exist and contain this lot number, add its CBM
                  if (nullDeliveredLots.contains(String.valueOf(lotNum))) {
                     cbm += schoolLotModel.getCbm();
                  }
               }
            } else {
               // If there are no delivered lots, check if there are generated lots
               if (nullGeneratedLots != null) {
                  // If the generated lots contain this lot number, add its CBM
                  if (nullGeneratedLots.contains(String.valueOf(lotNum))) {
                     cbm += schoolLotModel.getCbm();
                  }
               } else {
                  // If neither delivered nor generated lots exist, check if the lot is in the selected lots
                  if (selectedLot.contains(String.valueOf(lotNum))) {
                     cbm += schoolLotModel.getCbm();
                  }
               }
            }
         }
      }
      
      return cbm; // Return the total CBM calculated for the school
   }
   
   
   /** If the next projects has different lots, modify the methods and the models for the exact lots **/
   /// Helper method to check if the delivered school has a valid lot
   private List<String> getDeliveredLotsWithNullValues(List<String> selectedLot, DeliveredSchoolModel delivered) {
      // Map delivered lots to their corresponding lot numbers
      Map<String, String> deliveredLots = new LinkedHashMap<>();
      deliveredLots.put("6", delivered.getLot6());
      deliveredLots.put("7", delivered.getLot7());
      deliveredLots.put("8", delivered.getLot8());
      deliveredLots.put("9", delivered.getLot9());
      deliveredLots.put("10", delivered.getLot10());
      deliveredLots.put("11", delivered.getLot11());
      deliveredLots.put("13", delivered.getLot13());
      deliveredLots.put("14", delivered.getLot14());
      
      // List to store lots with null values
      List<String> nullLots = new ArrayList<>();
      
      // Check if the delivered lots match any selected lots and if their value is null or empty
      for (Map.Entry<String, String> entry : deliveredLots.entrySet()) {
         String value = entry.getValue(); // Get the value
         if (selectedLot.contains(entry.getKey()) && (value == null || value.trim().isEmpty())) {
            nullLots.add(entry.getKey()); // Add the lot number to the list
         }
      }
      
      return nullLots; // Return list of lots with null values
   }
   
   /// Helper method to check if the generated school has a valid lot
   private List<String> getGeneratedLotsWithNullValues(List<String> selectedLot, GeneratedSchoolModel generated) {
      // Map delivered lots to their corresponding lot numbers
      Map<String, String> generatedLots = new LinkedHashMap<>();
      generatedLots.put("6", generated.getLot6());
      generatedLots.put("7", generated.getLot7());
      generatedLots.put("8", generated.getLot8());
      generatedLots.put("9", generated.getLot9());
      generatedLots.put("10", generated.getLot10());
      generatedLots.put("11", generated.getLot11());
      generatedLots.put("13", generated.getLot13());
      generatedLots.put("14", generated.getLot14());
      
      // List to store lots with null values
      List<String> nullLots = new ArrayList<>();
      
      // Check if the delivered lots match any selected lots and if their value is null
      for (Map.Entry<String, String> entry : generatedLots.entrySet()) {
         if (selectedLot.contains(entry.getKey()) && entry.getValue() == null) {
            nullLots.add(entry.getKey()); // Add the lot number to the list
         }
      }
      
      return nullLots; // Return list of lots with null values
   }
   
   /// Extracts the lot number from a given string
   private int extractLotNumber(String lot) {
      Pattern pattern = Pattern.compile("LOT (\\d+)");
      Matcher matcher = pattern.matcher(lot);
      return matcher.find() ? Integer.parseInt(matcher.group(1)) : Integer.MAX_VALUE;
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