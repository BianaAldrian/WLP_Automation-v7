package org.nikkatrading.wlp_automationv7.GenerateWLP;

import javafx.application.Platform;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.nikkatrading.wlp_automationv7.Models.Table.SchoolList_TableModel;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProcessWLP {
   
   public ProcessWLP(Stage mainStage, List<String> finalSelectedLot, List<SchoolList_TableModel> selectedSchool, Map<Integer, List<SchoolList_TableModel>> selectedBatchesMap) {
      String templatePath = getFileInFolder("res/excel/loadplan_template");
      String summaryTemplate = getFileInFolder("res/excel/summary_template");
      
      try (FileInputStream templateFis = new FileInputStream(templatePath);
           FileInputStream summaryTemplateFis = new FileInputStream(summaryTemplate)) {
         
         Workbook templateWorkbook = new XSSFWorkbook(templateFis);
         Workbook summaryTemplateWorkbook = new XSSFWorkbook(summaryTemplateFis);
         
         Sheet templateSheet = templateWorkbook.getSheetAt(0);
         Sheet summaryTemplateSheet = summaryTemplateWorkbook.getSheetAt(0);
         
         String version = "7.0";
         
         LocalTime currentTime = LocalTime.now();
         DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");
         String formattedTime = currentTime.format(timeFormatter);
         
         LocalDate currentDate = LocalDate.now();
         DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMddyy");
         String formattedDate = currentDate.format(dateFormatter);
         
         int batchCounter = readAndIncrementBatchCounter();
         
         new CreateLoadPlan(version, formattedTime, formattedDate, batchCounter, templateSheet, templateWorkbook, selectedSchool);
         
         Platform.runLater(() -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save File");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
            fileChooser.setInitialFileName(formattedDate + "-" + batchCounter + ".xlsx");
            
            File file = fileChooser.showSaveDialog(mainStage);
            if (file != null) {
               try (FileOutputStream fos = new FileOutputStream(file)) {
                  templateWorkbook.write(fos); // Write workbook data
                  System.out.println("File saved successfully at: " + file.getAbsolutePath());
               } catch (IOException e) {
                  e.printStackTrace();
               } finally {
                  try {
                     templateWorkbook.close(); // Close the workbook after saving
                     summaryTemplateWorkbook.close();
                  } catch (IOException e) {
                     e.printStackTrace();
                  }
               }
            } else {
               System.out.println("File save was canceled by the user.");
               try {
                  templateWorkbook.close();
                  summaryTemplateWorkbook.close();
               } catch (IOException e) {
                  e.printStackTrace();
               }
            }
         });
         
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   
   /// Method to get the first Excel file found in the specified folder
   public static String getFileInFolder(String folderPath) {
      File folder = new File(folderPath);
      File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".xlsx") || name.toLowerCase().endsWith(".xls"));
      if (files != null && files.length > 0) {
         return files[0].getAbsolutePath(); // Return the first Excel file found
      }
      return null; // Return null if no Excel files are found
   }
   
   private int readAndIncrementBatchCounter() {
      int batchCounter = 0;
      // Read the current batch counter
      try (BufferedReader reader = new BufferedReader(new FileReader("res/batchNum"))) {
         String line = reader.readLine();
         if (line != null) {
            batchCounter = Integer.parseInt(line.trim());
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
      
      // Increment the batch counter
      batchCounter++;
      
      // Write the new batch counter back to the file
      try (BufferedWriter writer = new BufferedWriter(new FileWriter("res/batchNum"))) {
         writer.write(Integer.toString(batchCounter));
      } catch (IOException e) {
         e.printStackTrace();
      }
      
      return batchCounter;
   }
}
