package org.nikkatrading.wlp_automationv7.GenerateWLP;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.nikkatrading.wlp_automationv7.DB.PutHelpDesk;
import org.nikkatrading.wlp_automationv7.Models.Table.SchoolList_TableModel;
import org.nikkatrading.wlp_automationv7.UI.LoadingUtil;
import org.nikkatrading.wlp_automationv7.Utility.SessionData;
import org.nikkatrading.wlp_automationv7.Utility.WLPContext;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class ProcessWLP {
   
   public interface WLPGenerationListener {
      void onSuccess();
      void onFailure(Exception e);
   }
   
   public ProcessWLP(WLPContext context, WLPGenerationListener listener) {
      String templatePath = getFileInFolder("res/excel/loadplan_template");
      String summaryTemplate = getFileInFolder("res/excel/summary_template");
      
      LocalTime currentTime = LocalTime.now();
      DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");
      String formattedTime = currentTime.format(timeFormatter);
      
      LocalDate currentDate = LocalDate.now();
      DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMddyy");
      String formattedDate = currentDate.format(dateFormatter);
      
      String batchNo;
      
      if (context.cbContainer.isSelected()) {
         batchNo = "CONTAINER " + context.inp_containerNum.getText().trim();
         createWLPContainer(context, templatePath, summaryTemplate, batchNo, formattedTime, formattedDate, listener);
      } else {
         batchNo = "BATCH NO. " + formattedDate + "-" + context.inp_batchNumber.getText().trim();
         createWLPSingle(context, templatePath, summaryTemplate, batchNo, formattedTime, listener);
      }
   }
   
   
   private void createWLPSingle(WLPContext context, String templatePath, String summaryTemplate,
                                String batchNo, String formattedTime, WLPGenerationListener listener) {
   
   String message = "Generating single WLP, please wait...";
      LoadingUtil.showLoading(context.mainStage, message);
      
      Task<Void> generateTask = new Task<>() {
         @Override
         protected Void call() {
            // Perform the generation process
            try (FileInputStream templateFis = new FileInputStream(templatePath);
                 FileInputStream summaryTemplateFis = new FileInputStream(summaryTemplate)) {
               
               Workbook templateWorkbook = new XSSFWorkbook(templateFis);
               Workbook summaryTemplateWorkbook = new XSSFWorkbook(summaryTemplateFis);
               
               new CreateLoadPlan(context, formattedTime, batchNo, templateWorkbook, context.selectedSchool);
               new CreateSummary(context, formattedTime, batchNo, templateWorkbook, summaryTemplateWorkbook, context.selectedSchool);
               
               SessionData.getInstance().setBatch_no(batchNo);
               SessionData.getInstance().setSchool_count(context.selectedSchool.size());
               SessionData.getInstance().setSchoolList(context.selectedSchool);
               
               Platform.runLater(() -> {
                  FileChooser fileChooser = new FileChooser();
                  fileChooser.setTitle("Save File");
                  fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
                  fileChooser.setInitialFileName(batchNo + ".xlsx");
                  
                  File file = fileChooser.showSaveDialog(context.mainStage);
                  if (file != null) {
                     PutHelpDesk helpDesk = new PutHelpDesk();
                     if (helpDesk.isSuccess()) {
                        try (FileOutputStream fos = new FileOutputStream(file)) {
                           templateWorkbook.write(fos);
                           System.out.println("File saved successfully at: " + file.getAbsolutePath());
                           
                           String batchNum = context.inp_batchNumber.getText().trim();
                           writeBatchNumToFile(context.inp_batchNumber, batchNum);
                           
                           listener.onSuccess(); // ✅ Notify success
                           
                        } catch (IOException e) {
                           e.printStackTrace();
                           listener.onFailure(e); // ✅ Notify failure
                        } finally {
                           try {
                              templateWorkbook.close();
                              summaryTemplateWorkbook.close();
                           } catch (IOException e) {
                              e.printStackTrace();
                           }
                        }
                     } else {
                        System.out.println("Failed to complete database insertion. Aborting file export.");
                        listener.onFailure(new RuntimeException("Database insertion failed")); // ✅ Notify failure
                     }
                  } else {
                     System.out.println("File save was canceled by the user.");
                     listener.onFailure(new RuntimeException("File save canceled")); // ✅ Notify failure
                  }
                  
                  LoadingUtil.closeLoading();
               });
            } catch (IOException e) {
               e.printStackTrace();
            }
            return null;
         }
         
         @Override
         protected void succeeded() {
            // Don't call onSuccess here because the real logic runs inside Platform.runLater()
            // Just close loading UI
            LoadingUtil.closeLoading();
         }
         
         @Override
         protected void failed() {
            Platform.runLater(() -> {
               LoadingUtil.closeLoading();
               Throwable exception = getException();
               exception.printStackTrace();
               listener.onFailure(new Exception("Generation task failed", exception)); // ✅ Notify failure
               
               Alert alert = new Alert(Alert.AlertType.ERROR);
               alert.setTitle("Error");
               alert.setHeaderText(null);
               alert.setContentText("An error occurred during generation: " + exception.getMessage());
               alert.showAndWait();
            });
         }
      };
      
      new Thread(generateTask).start();
   }
   
   private void createWLPContainer(WLPContext context, String templatePath, String summaryTemplate,
                                   String batchNo, String formattedTime, String formattedDate,
                                   WLPGenerationListener listener) {
      String message = "Generating WLP for Container, please wait...";
      LoadingUtil.showLoading(context.mainStage, message);
      
      Task<Void> generateTask = new Task<>() {
         @Override
         protected Void call() {
            try (FileInputStream templateFis = new FileInputStream(templatePath);
                 FileInputStream summaryTemplateFis = new FileInputStream(summaryTemplate)) {
               
               Workbook templateWorkbook = new XSSFWorkbook(templateFis);
               Workbook summaryTemplateWorkbook = new XSSFWorkbook(summaryTemplateFis);
               
               new CreateLoadPlan(context, formattedTime, batchNo, templateWorkbook, context.selectedSchool);
               new CreateSummary(context, formattedTime, batchNo, templateWorkbook, summaryTemplateWorkbook, context.selectedSchool);
               
               Platform.runLater(() -> {
                  try {
                     DirectoryChooser directoryChooser = new DirectoryChooser();
                     directoryChooser.setTitle("Select Directory to Save WLP Container");
                     
                     File selectedDir = directoryChooser.showDialog(context.mainStage);
                     if (selectedDir == null) {
                        throw new IOException("Directory selection cancelled.");
                     }
                     
                     File containerFolder = new File(selectedDir, batchNo);
                     if (!containerFolder.exists() && !containerFolder.mkdirs()) {
                        throw new IOException("Failed to create folder: " + containerFolder.getAbsolutePath());
                     }
                     
                     File newFile = new File(containerFolder, batchNo + ".xlsx");
                     
                     try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        String containerDir = containerFolder.getAbsolutePath();
                        for (Map.Entry<Integer, List<SchoolList_TableModel>> entry : context.selectedBatchesMap.entrySet()) {
                           List<SchoolList_TableModel> schoolList = entry.getValue();
                           String batchNum = context.inp_batchNumber.getText().trim();
                           String batchNos = "BATCH NO. " + formattedDate + "-" + batchNum;
                           
                           saveWLPInContainer(context, containerDir, batchNum, formattedTime, batchNos,
                                   templatePath, summaryTemplate, schoolList);
                        }
                        
                        templateWorkbook.write(fos);
                        writeContainerNumToFile(context.inp_containerNum);
                        System.out.println("File saved successfully at: " + newFile.getAbsolutePath());
                        
                        listener.onSuccess();
                     }
                  } catch (Exception ex) {
                     listener.onFailure(ex);
                  } finally {
                     try {
                        templateWorkbook.close();
                        summaryTemplateWorkbook.close();
                     } catch (IOException ex) {
                        ex.printStackTrace();
                     }
                     LoadingUtil.closeLoading();
                  }
               });
               
            } catch (IOException e) {
               Platform.runLater(() -> {
                  listener.onFailure(e);
                  LoadingUtil.closeLoading();
               });
            }
            return null;
         }
      };
      
      new Thread(generateTask).start();
   }
   
   
   private void saveWLPInContainer(WLPContext context, String containerDir, String batchNum, String formattedTime, String batchNo, String templatePath, String summaryTemplate, List<SchoolList_TableModel> schoolList) {
      try (FileInputStream templateFis = new FileInputStream(templatePath);
           FileInputStream summaryTemplateFis = new FileInputStream(summaryTemplate)) {
         
         Workbook templateWorkbook = new XSSFWorkbook(templateFis);
         Workbook summaryTemplateWorkbook = new XSSFWorkbook(summaryTemplateFis);
         
         new CreateLoadPlan(context, formattedTime, batchNo, templateWorkbook, schoolList);
         new CreateSummary(context, formattedTime, batchNo, templateWorkbook, summaryTemplateWorkbook, schoolList);
         
         SessionData.getInstance().setBatch_no(batchNo);
         SessionData.getInstance().setSchool_count(schoolList.size());
         SessionData.getInstance().setSchoolList(schoolList);
         
         PutHelpDesk helpDesk = new PutHelpDesk();
         if (helpDesk.isSuccess()) {
            File outputFile = new File(containerDir, batchNo + ".xlsx");
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
               templateWorkbook.write(fos);
               System.out.println("Saved: " + outputFile.getAbsolutePath());
               writeBatchNumToFile(context.inp_batchNumber, batchNum);
            }
         } else {
            System.out.println("Failed to complete database insertion. Aborting file export.");
            // Optionally, show alert to user or log the error
         }
         
         templateWorkbook.close();
         summaryTemplateWorkbook.close();
         
      } catch (IOException e) {
         e.printStackTrace();
         throw new RuntimeException("Error saving WLP: " + batchNo, e);
      }
   }
   
   /// Method to get the first Excel file found in the specified folder
   private String getFileInFolder(String folderPath) {
      File folder = new File(folderPath);
      File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".xlsx") || name.toLowerCase().endsWith(".xls"));
      if (files != null && files.length > 0) {
         return files[0].getAbsolutePath(); // Return the first Excel file found
      }
      return null; // Return null if no Excel files are found
   }
   
   private void writeBatchNumToFile(TextField inp_batchNumber, String batchNum) {
      int addBatchNum = Integer.parseInt(batchNum) + 1;
      File file = new File("res/batchNum");
      
      try {
         // Ensure the res directory exists
         File resDir = new File("res");
         if (!resDir.exists()) {
            resDir.mkdirs();
         }
         
         // Write the batch number to the file
         Files.writeString(file.toPath(), String.valueOf(addBatchNum), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
         System.out.println("Batch number saved to: " + file.getAbsolutePath());
         inp_batchNumber.setText(String.valueOf(addBatchNum));
      } catch (IOException e) {
         e.printStackTrace();
         System.err.println("Failed to write batch number to file.");
      }
   }
   
   private void writeContainerNumToFile(TextField inp_containerNum) {
      int addContainerNum = Integer.parseInt(inp_containerNum.getText().trim()) + 1;
      String containerNum = String.valueOf(addContainerNum);
      File file = new File("res/containerNum");
      
      try {
         // Ensure the res directory exists
         File resDir = new File("res");
         if (!resDir.exists()) {
            resDir.mkdirs();
         }
         
         // Write the batch number to the file
         Files.writeString(file.toPath(), containerNum, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
         System.out.println("Container number saved to: " + file.getAbsolutePath());
         inp_containerNum.setText(containerNum);
      } catch (IOException e) {
         e.printStackTrace();
         System.err.println("Failed to write batch number to file.");
      }
   }
   
   /*private int readAndIncrementBatchCounter() {
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
   }*/
}
