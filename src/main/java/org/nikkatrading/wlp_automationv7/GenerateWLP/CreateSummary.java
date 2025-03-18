package org.nikkatrading.wlp_automationv7.GenerateWLP;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.nikkatrading.wlp_automationv7.Models.Table.SchoolGradeLevel_Model;
import org.nikkatrading.wlp_automationv7.Models.Table.SchoolList_TableModel;
import org.nikkatrading.wlp_automationv7.Models.Table.SchoolLot_Model;

import java.util.*;

public class CreateSummary {
   
   public CreateSummary(String version, String formattedTime, String formattedDate, int batchCounter, Workbook templateWorkbook, Workbook summaryTemplateWorkbook, List<SchoolList_TableModel> selectedSchool) {
      Sheet summaryTemplateSheet = summaryTemplateWorkbook.getSheetAt(0);
      
      Set<Integer> availableLot = new TreeSet<>();
      
      // Get selected school lots
      for (SchoolList_TableModel schoolModel : selectedSchool) {
         for (SchoolGradeLevel_Model gradeLevelModel : schoolModel.getTableSchoolGradeLevelList()) {
            // Populate lots
            for (SchoolLot_Model lotModel : gradeLevelModel.getTableSchoolLotList()) {
               String lotString = lotModel.getLotName().split(":")[0].replace("LOT", "").trim();
               try {
                  int lotNumber = Integer.parseInt(lotString);
                  availableLot.add(lotNumber); // Store as an Integer for correct sorting
               } catch (NumberFormatException e) {
                  System.err.println("Invalid lot number format: " + lotString);
               }
            }
         }
      }
      
      // Create a new "Summary" sheet in the workbook
      Sheet summarySheet = templateWorkbook.createSheet("Summary");
      copyPageLayout(summaryTemplateSheet, summarySheet);
      
      // Copy the header part of the summary
      for (int i = 0; i < 3; i++) {
         Row summarySourceRow = summaryTemplateSheet.getRow(i); // Row 1 (Index 0)
         if (summarySourceRow != null) {
            Row newRow = summarySheet.createRow(i);
            copyRow(summaryTemplateSheet, summarySheet, summarySourceRow, newRow, 0, summarySourceRow.getLastCellNum() - 1);
         }
      }
      
      setCellValue(summarySheet, templateWorkbook, 0, 2, version); // set the version in row 1 col C;
      setCellValue(summarySheet, templateWorkbook, 0, 5, formattedTime); // set the time in row 1 col F;
      setCellValue(summarySheet, templateWorkbook, 0, 7, "Batch No. " + formattedDate + "-" + batchCounter); // set the batch no in row 1 col H;
      setCellValue(summarySheet, templateWorkbook, 2, 0, "Summary"); // set the batch no in row 3 col A;
      
      setLotValues(summaryTemplateSheet, summarySheet, availableLot);
   }
   
   private void setLotValues(Sheet summaryTemplateSheet, Sheet summarySheet, Set<Integer> availableLot) {
      List<LotSummary_Model> lotSummaryList = templateLotRows();
      int currentRowIndex = 3;
      
      for (LotSummary_Model lotSummaryModel : lotSummaryList) {
         if (availableLot.contains(lotSummaryModel.getLot())) {
            for (int i = lotSummaryModel.getStartRow(); i <= lotSummaryModel.getEndRow(); i++) {
               Row summarySourceRow = summaryTemplateSheet.getRow(i);
               if (summarySourceRow != null) {
                  Row newRow = summarySheet.createRow(currentRowIndex);
                  copyRow(summaryTemplateSheet, summarySheet, summarySourceRow, newRow, 0, summarySourceRow.getLastCellNum() - 1);
                  currentRowIndex++;
                  
                  // Insert row 1 after every 31 rows
                  if (currentRowIndex % 31 == 0) { // (currentRowIndex - 3) ensures it starts counting from the first added row
                     Row summaryHeaderRow = summarySheet.getRow(0); // Row 1 (Index 0-based)
                     if (summaryHeaderRow != null) {
                        Row insertedRow = summarySheet.createRow(currentRowIndex);
                        copyRow(summarySheet, summarySheet, summaryHeaderRow, insertedRow, 0, summaryHeaderRow.getLastCellNum() - 1);
                        currentRowIndex++; // Move to the next row after inserting the header
                     }
                  }
               }
            }
            currentRowIndex++;
         }
      }
   }
   
   /// Helper to set a value in a cell without affecting its current style
   private static void setCellValue(Sheet sheet, Workbook workbook, int rowIndex, int colIndex, Object value) {
      Row row = sheet.getRow(rowIndex);
      if (row == null) {
         row = sheet.createRow(rowIndex);
      }
      
      Cell cell = row.getCell(colIndex);
      if (cell == null) {
         cell = row.createCell(colIndex);
      }
      
      // Preserve existing cell style
      CellStyle originalStyle = cell.getCellStyle();
      
      // Set value based on data type
      if (value instanceof String) {
         cell.setCellValue((String) value);
      } else if (value instanceof Double) {
         cell.setCellValue((Double) value);
      } else if (value instanceof Integer) {
         cell.setCellValue((Integer) value);
      } else if (value instanceof Boolean) {
         cell.setCellValue((Boolean) value);
      } else if (value instanceof Date) {
         CreationHelper createHelper = workbook.getCreationHelper();
         cell.setCellValue((Date) value);
         cell.setCellStyle(originalStyle); // Retain date formatting
      } else {
         cell.setCellValue(value.toString()); // Fallback to string
      }
      
      // Restore the original cell style
      cell.setCellStyle(originalStyle);
   }
   
   public void copyRow(Sheet sourceSheet, Sheet targetSheet, Row sourceRow, Row newRow, int startColumn, int endColumn) {
      Workbook targetWorkbook = targetSheet.getWorkbook(); // Get target workbook
      
      newRow.setHeight(sourceRow.getHeight()); // Copy row height
      
      for (int i = startColumn; i <= endColumn; i++) {
         Cell sourceCell = sourceRow.getCell(i);
         if (sourceCell == null) continue; // Skip empty cells
         
         Cell newCell = newRow.createCell(i);
         
         // Copy column width
         targetSheet.setColumnWidth(i, sourceSheet.getColumnWidth(i));
         
         // Copy cell style safely
         if (sourceCell.getCellStyle() != null) {
            CellStyle newCellStyle = targetWorkbook.createCellStyle();
            newCellStyle.cloneStyleFrom(sourceCell.getCellStyle()); // Clone style
            newCell.setCellStyle(newCellStyle);
         }
         
         // Copy cell values
         switch (sourceCell.getCellType()) {
            case STRING:
               newCell.setCellValue(sourceCell.getStringCellValue());
               break;
            case NUMERIC:
               newCell.setCellValue(sourceCell.getNumericCellValue());
               break;
            case BOOLEAN:
               newCell.setCellValue(sourceCell.getBooleanCellValue());
               break;
            case FORMULA:
               newCell.setCellFormula(sourceCell.getCellFormula());
               break;
            default:
               newCell.setCellValue(sourceCell.toString());
         }
      }
      
      // Copy merged regions if they exist in this row
      copyMergedRegions(sourceSheet, targetSheet, sourceRow.getRowNum(), newRow.getRowNum());
   }
   
   private void copyMergedRegions(Sheet sourceSheet, Sheet targetSheet, int sourceRowNum, int targetRowNum) {
      for (int i = 0; i < sourceSheet.getNumMergedRegions(); i++) {
         CellRangeAddress mergedRegion = sourceSheet.getMergedRegion(i);
         if (mergedRegion.getFirstRow() <= sourceRowNum && mergedRegion.getLastRow() >= sourceRowNum) {
            int newFirstRow = targetRowNum - (sourceRowNum - mergedRegion.getFirstRow());
            int newLastRow = targetRowNum + (mergedRegion.getLastRow() - sourceRowNum);
            CellRangeAddress newMergedRegion = new CellRangeAddress(newFirstRow, newLastRow, mergedRegion.getFirstColumn(), mergedRegion.getLastColumn());
            
            // Check if the merged region already exists in targetSheet to avoid duplication
            boolean exists = false;
            for (int j = 0; j < targetSheet.getNumMergedRegions(); j++) {
               if (targetSheet.getMergedRegion(j).equals(newMergedRegion)) {
                  exists = true;
                  break;
               }
            }
            
            if (!exists) {
               targetSheet.addMergedRegion(newMergedRegion);
            }
         }
      }
   }
   
   private void copyPageLayout(Sheet sourceSheet, Sheet targetSheet) {
      // Copy print setup
      PrintSetup sourcePrintSetup = sourceSheet.getPrintSetup();
      PrintSetup targetPrintSetup = targetSheet.getPrintSetup();
      
      targetPrintSetup.setLandscape(sourcePrintSetup.getLandscape());
      targetPrintSetup.setPaperSize(sourcePrintSetup.getPaperSize());
      targetPrintSetup.setScale(sourcePrintSetup.getScale());
      targetPrintSetup.setFooterMargin(sourcePrintSetup.getFooterMargin());
      targetPrintSetup.setHeaderMargin(sourcePrintSetup.getHeaderMargin());
      targetPrintSetup.setFitHeight(sourcePrintSetup.getFitHeight());
      targetPrintSetup.setFitWidth(sourcePrintSetup.getFitWidth());
      
      // Set centering horizontally and vertically
      targetSheet.setHorizontallyCenter(true);
      
      // Copy margins
      targetSheet.setMargin(Sheet.LeftMargin, sourceSheet.getMargin(Sheet.LeftMargin));
      targetSheet.setMargin(Sheet.RightMargin, sourceSheet.getMargin(Sheet.RightMargin));
      targetSheet.setMargin(Sheet.TopMargin, sourceSheet.getMargin(Sheet.TopMargin));
      targetSheet.setMargin(Sheet.BottomMargin, sourceSheet.getMargin(Sheet.BottomMargin));
      
      // Set the sheet to fit to one page width
      targetSheet.setFitToPage(true);
      
      // Copy column widths for all columns
      for (int i = 0; i <= sourceSheet.getRow(0).getLastCellNum(); i++) {
         targetSheet.setColumnWidth(i, sourceSheet.getColumnWidth(i));
      }
   }
   
   // Template for summary
   private List<LotSummary_Model> templateLotRows() {
      // Initialize the list if it's not already initialized
      List<LotSummary_Model> lotRowsModel = new ArrayList<>();
      
      // Add LotSummary_Model objects to the list
      lotRowsModel.add(new LotSummary_Model(6, 3, 25));   // For lot 6
      lotRowsModel.add(new LotSummary_Model(7, 28, 52));  // For lot 7
      lotRowsModel.add(new LotSummary_Model(8, 55, 86));  // For lot 8
      lotRowsModel.add(new LotSummary_Model(9, 89, 105));  // For lot 9
      lotRowsModel.add(new LotSummary_Model(10, 108, 119)); // For lot 10
      lotRowsModel.add(new LotSummary_Model(11, 122, 132)); // For lot 11
      lotRowsModel.add(new LotSummary_Model(13, 135, 144)); // For lot 13
      lotRowsModel.add(new LotSummary_Model(14, 147, 168)); // For lot 14
      lotRowsModel.add(new LotSummary_Model(14, 171, 209)); // For lot 14 (2021)
      
      // Return the populated list
      return lotRowsModel;
   }
}
