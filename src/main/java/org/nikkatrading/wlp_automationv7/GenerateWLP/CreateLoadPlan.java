package org.nikkatrading.wlp_automationv7.GenerateWLP;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.util.Units;
import org.nikkatrading.wlp_automationv7.Models.Table.SchoolGradeLevel_Model;
import org.nikkatrading.wlp_automationv7.Models.Table.SchoolList_TableModel;
import org.nikkatrading.wlp_automationv7.Models.Table.SchoolLot_Model;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class CreateLoadPlan {
   
   public CreateLoadPlan(String version, String formattedTime, String formattedDate, int batchCounter, Workbook templateWorkbook, List<SchoolList_TableModel> selectedSchool) {
      Sheet loadplanSheet = templateWorkbook.getSheetAt(0);
      Sheet sheet2 = templateWorkbook.getSheetAt(1);
      
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
                  System.err.println( "Invalid lot number format: " + lotString);
               }
            }
         }
      }
      
      // set value in the sheet 2 first before copying it in the lower part of the load plan
      setValueInSheet2(sheet2, templateWorkbook, availableLot, selectedSchool);
      
      // set value in the header part of the load plan
      setValueInRow1To7(loadplanSheet, templateWorkbook, version, formattedTime, formattedDate, batchCounter, availableLot, selectedSchool);
      
      // set value in the load plan based on the selected schools
      setValueInRow12Onwards(loadplanSheet, templateWorkbook, availableLot, selectedSchool);
   }
   
   /**
    * Returns a custom sorting order index for grade levels.
    */
   private int getGradeLevelOrder(String gradeLevel) {
      List<String> customOrder = Arrays.asList(
              "G1toG3", "G1toG3_2021",
              "G4toG6", "G4toG6_2021",
              "JHS", "JHS_2021",
              "SHSCore", "SHSCore_2021"
      );
      return customOrder.indexOf(gradeLevel);
   }
   
   ///  Method to set a value in the sheet 2 of the load plan before copying it in the first sheet of the load plan
   private void setValueInSheet2(Sheet sheet2, Workbook templateWorkbook, Set<Integer> availableLot, List<SchoolList_TableModel> selectedSchool) {
      int gradeLvlRowIndex = 0;
      int borderRowIndex = 1; // Row to copy from (zero-based index)
      int startColumn = 6; // Starting column to copy
      int endColumn = 11; // Ending column to copy
      int currentRowIndex = borderRowIndex + 1; // Start inserting after the source row
      
      Set<String> gradeLevelSet = new HashSet<>();
      
      Row gradeLvlRow = sheet2.getRow(gradeLvlRowIndex);
      if (gradeLvlRow == null) {
         System.out.println("Grade Level row is null. Check the template sheet.");
         return;
      }
      
      Row borderRow = sheet2.getRow(borderRowIndex);
      if (borderRow == null) {
         System.out.println("Border row is null. Check the template sheet.");
         return;
      }
      
      // Create rows based on available lots
      for (Integer lot : availableLot) {
         Row newRow = sheet2.getRow(currentRowIndex);
         if (newRow == null) {
            newRow = sheet2.createRow(currentRowIndex); // Only create if it doesn't exist
         }
         
         copyRow(sheet2, borderRow, newRow, startColumn, endColumn);
         currentRowIndex++; // Increment row index for the next lot
         
         // If the lot is 14, add three additional rows
         if (lot == 14) {
            Row newGradeLvlRow = sheet2.getRow(currentRowIndex);
            if (newGradeLvlRow == null) {
               newGradeLvlRow = sheet2.createRow(currentRowIndex); // Only create if it doesn't exist
            }
            
            copyRow(sheet2, gradeLvlRow, newGradeLvlRow, startColumn, endColumn);
            currentRowIndex++;
            
            for (int i = 0; i < 2; i++) {
               Row extraRow = sheet2.getRow(currentRowIndex);
               if (extraRow == null) {
                  extraRow = sheet2.createRow(currentRowIndex); // Only create if it doesn't exist
               }
               
               copyRow(sheet2, borderRow, extraRow, startColumn, endColumn);
               currentRowIndex++; // Increment for each extra row
            }
         }
      }
      
      // Set values in the created rows
      // Read header from columns H to L
      Row gradeLevelRow = sheet2.getRow(0);
      if (gradeLevelRow != null) {
         for (int gradeLvlCol = 7; gradeLvlCol <= 11; gradeLvlCol++) {
            Cell gradeLevelCell = gradeLevelRow.getCell(gradeLvlCol);
            if (gradeLevelCell == null) continue; // Skip null cells
            
            String gradeLevel = gradeLevelCell.getStringCellValue().trim();
            if (gradeLevel.isEmpty()) continue; // Skip empty grade levels
            
            double cbm = 0, cbm2021 = 0;
            
            List<Integer> lotList = new ArrayList<>(availableLot);
            for (int i = 0; i < lotList.size(); i++) {
               String strLot = String.valueOf(lotList.get(i));
               setCellValue(sheet2, templateWorkbook, i + 1, 6, "LOT " + strLot); // Set lots in column G
               
               int spi = 0, spi2021 = 0;
               
               // Loop through selected schools to match grade levels and lots
               for (SchoolList_TableModel schoolModel : selectedSchool) {
                  for (SchoolGradeLevel_Model gradeLevelModel : schoolModel.getTableSchoolGradeLevelList()) {
                     gradeLevelSet.add(gradeLevelModel.getGradeLevel());
                     
                     if (gradeLevelModel.getGradeLevel().equals(gradeLevel)) {
                        // Compare if the lot number matches
                        for (SchoolLot_Model lotModel : gradeLevelModel.getTableSchoolLotList()) {
                           String lotName = lotModel.getLotName();
                           if (lotName == null || !lotName.contains(":")) continue; // Avoid null or invalid format
                           
                           String extractedLotNumber = lotName.split(":")[0]
                                   .replace("LOT ", "").trim();
                           
                           if (extractedLotNumber.equals(strLot)) {
                              spi += lotModel.getSetPerItem();
                              cbm += lotModel.getCbm();
                           }
                        }
                     }
                     
                     if (lotList.contains(14)) {
                        if (gradeLevelModel.getGradeLevel().contains("2021")) {
                           if (gradeLevelModel.getGradeLevel().replace("_2021", "").trim().equals(gradeLevel)) {
                              for (SchoolLot_Model lotModel : gradeLevelModel.getTableSchoolLotList()) {
                                 String lotName = lotModel.getLotName();
                                 if (lotName == null || !lotName.contains(":")) continue; // Avoid null or invalid format
                                 
                                 String extractedLotNumber = lotName.split(":")[0]
                                         .replace("LOT ", "").trim();
                                 
                                 if (extractedLotNumber.equals(strLot)) {
                                    spi2021 += lotModel.getSetPerItem();
                                    cbm2021 += lotModel.getCbm();
                                 }
                              }
                           }
                        }
                     }
                  }
               }
               
               // Set the SPI in the cell per lot and per grade level
               if (spi != 0) {
                  setCellValue(sheet2, templateWorkbook, i + 1, gradeLvlCol, spi);
               }
               if (spi2021 != 0) {
                  setCellValue(sheet2, templateWorkbook, currentRowIndex - 2, 6, "LOT 14");
                  setCellValue(sheet2, templateWorkbook, currentRowIndex - 2, gradeLvlCol, spi2021);
               }
            }
            
            // Set the cbm per grade level in all selected lots
            if (cbm != 0) {
               setCellValue(sheet2, templateWorkbook, lotList.size() + 1, gradeLvlCol, Math.round(cbm * 100.0) / 100.0);
            }
            if (cbm2021 != 0) {
               setCellValue(sheet2, templateWorkbook, currentRowIndex - 1, gradeLvlCol, Math.round(cbm2021 * 100.0) / 100.0);
            }
         }
      }
      
      // Convert gradeLevelSet to a sorted list and store in sheet
      List<String> sortedGradeLevels = new ArrayList<>(gradeLevelSet);
      sortedGradeLevels.sort(Comparator.comparingInt(this::getGradeLevelOrder)); // Sort by custom order
      
      String gradeLevelsString = String.join(", ", sortedGradeLevels);
      setCellValue(sheet2, templateWorkbook, 4, 2, gradeLevelsString);
      // Set grade levels in row 5 col C
      
      /// For 2021, remove this if the project do not have this specific condition
      if (availableLot.contains(14)) {
         // Set the SPI in the cell per lot and per grade level
         setCellValue(sheet2, templateWorkbook, availableLot.size() + 2, 6, 2021);
      }
   }
   
   /// Method to set a value in row 1 to row 7
   private void setValueInRow1To7(Sheet templateSheet, Workbook templateWorkbook, String version, String formattedTime, String formattedDate, int batchCounter, Set<Integer> availableLot, List<SchoolList_TableModel> selectedSchool) {
      // Gather unique lots, region, and division names
      Set<String> regionNames = new LinkedHashSet<>();
      Set<String> divisionNames = new LinkedHashSet<>();

      // Populate region and division names
      for (SchoolList_TableModel schoolModel : selectedSchool) {
         regionNames.add(schoolModel.getRegion());
         divisionNames.add(schoolModel.getDivision());
      }

      // Format names
      String regionNamesString = formatNames(regionNames);
      String divisionNamesString = formatNames(divisionNames);
      String lotString = availableLot.stream()
              .map(String::valueOf) // Convert each Integer to String
              .collect(Collectors.joining(", "));
      
      // Set the image in header
      Drawing<?> drawing = templateSheet.createDrawingPatriarch();
      ClientAnchor anchor = getClientAnchor(templateWorkbook);
      
      try (FileInputStream fis = new FileInputStream("res/image/nikka_logo.png")) {
         byte[] bytes = fis.readAllBytes();
         int pictureIndex = templateWorkbook.addPicture(bytes, Workbook.PICTURE_TYPE_PNG);
         drawing.createPicture(anchor, pictureIndex);
      } catch (IOException e) {
         e.printStackTrace();
      }
      
      setCellValue(templateSheet, templateWorkbook, 0, 6, version); // set the version in row 1 col G;
      setCellValue(templateSheet, templateWorkbook, 0, 9, formattedTime); // set the time in row 1 col J;
      setCellValue(templateSheet, templateWorkbook, 0, 11, "Batch No. " + formattedDate + "-" + batchCounter); // set the batch no in row 1 col L;
      setCellValue(templateSheet, templateWorkbook, 3, 12, "R - " + regionNamesString); // set the region in the row 4 col M;
      setCellValue(templateSheet, templateWorkbook, 5, 1, "# " + divisionNamesString); // set the division in row 6 col B;
      setCellValue(templateSheet, templateWorkbook, 6, 4, "Lot " + lotString); // set the selected lot in row 7 col E;
      setCellValue(templateSheet, templateWorkbook, 6, 5, "Control No: R" + regionNamesString + " - " + formattedDate + "-" + batchCounter); // set the control number in row 7 col F;
      setCellValue(templateSheet, templateWorkbook, 6, 14, selectedSchool.size()); // set the number of selected school in row 7 col O;
   }
   
   /// Method to set the lots in row 12 and set the school sets in row 14 onwards
   private void setValueInRow12Onwards(Sheet templateSheet, Workbook templateWorkbook, Set<Integer> availableLot, List<SchoolList_TableModel> selectedSchool) {
      List<Integer> lotList = new ArrayList<>(availableLot); // Convert Set to List
      
      // Put the Lots in row 12 col 7
      for (int i = 0; i < lotList.size(); i++) {
         setCellValue(templateSheet, templateWorkbook, 11, 6 + i, "LOT "+lotList.get(i)); // Set numbering
      }
      
      // First, create the necessary rows before populating them with values
      createRows(templateSheet, templateWorkbook, selectedSchool);
      
      int currentRowIndex = 13; // Start filling data from row 13 (0-based index)
      int no = 1; // Counter for numbering schools
      
      int rowCount = 0; // Track the number of rows added
      int maxRowCount = 22; // Maximum rows allowed in the first page before a new page starts
      int nextPageRowCount = 36; // Maximum rows allowed in the following pages
      
      // Loop through each selected school to populate the rows
      for (SchoolList_TableModel schoolModel : selectedSchool) {
         List<SchoolGradeLevel_Model> gradeLevels = schoolModel.getTableSchoolGradeLevelList(); // Get grade levels for the school
         
         // If adding this school's grade levels exceeds the max row limit, move to a new page
         if (rowCount + gradeLevels.size() > maxRowCount) {
            currentRowIndex += maxRowCount - rowCount; // Skip to the next available row for the new page
            rowCount = maxRowCount; // Reset the row count to trigger new page logic
         }
         
         int firstRow = currentRowIndex; // Store the starting row index for this school (used for merging)
         
         // Iterate through each grade level and set its value
         for (SchoolGradeLevel_Model gradeLevelModel : gradeLevels) {
            
            // If rowCount reaches maxRowCount, move to a new page
            if (rowCount == maxRowCount) {
               currentRowIndex++; // Move to the next row for a new page
               firstRow = currentRowIndex; // Update the firstRow index for this new page
               rowCount = 0; // Reset row count for the new page
               maxRowCount = nextPageRowCount; // New row count for the following page
            }
            
            // Set the grade level in column 5 for the current row
            setCellValue(templateSheet, templateWorkbook, currentRowIndex, 5, gradeLevelModel.getGradeLevel());
            
            for (int i = 0; i < lotList.size(); i++) {
               String lotNum = lotList.get(i).toString();
               
               // Set the SPI in per column of lots in its current row
               for (SchoolLot_Model lotModel : gradeLevelModel.getTableSchoolLotList()) {
                  
                  if (lotModel.getLotName().split(":")[0].replace("LOT", "").trim().equals(lotNum)) {
                     setCellValue(templateSheet, templateWorkbook, currentRowIndex, 6 + i, lotModel.getSetPerItem());
                  }
               }
            }
            
            currentRowIndex++; // Move to the next row
            rowCount++; // Increase row count
         }
         
         int lastRow = currentRowIndex - 1; // Determine the last row for merging
         
         // If the school has multiple grade levels, merge the relevant columns
         if (gradeLevels.size() > 1) {
            mergeCells(templateSheet, firstRow, lastRow, 0, 0); // Merge column 1 (numbering)
            mergeCells(templateSheet, firstRow, lastRow, 1, 1); // Merge column 2 (division)
            mergeCells(templateSheet, firstRow, lastRow, 2, 2); // Merge column 3 (municipality)
            mergeCells(templateSheet, firstRow, lastRow, 3, 3); // Merge column 4 (school ID)
            mergeCells(templateSheet, firstRow, lastRow, 4, 4); // Merge column 5 (school name)
            mergeCells(templateSheet, firstRow, lastRow, 15, 15); // Merge column 16 (distance)
            mergeCells(templateSheet, firstRow, lastRow, 16, 16); // Merge column 17 (time)
         }
         
         // Set school details in the first row (one-time per school)
         setCellValue(templateSheet, templateWorkbook, firstRow, 0, no); // Set numbering
         setCellValue(templateSheet, templateWorkbook, firstRow, 1, schoolModel.getDivision()); // Set division
         setCellValue(templateSheet, templateWorkbook, firstRow, 3, schoolModel.getSchoolID()); // Set school ID
         setCellValue(templateSheet, templateWorkbook, firstRow, 4, schoolModel.getSchoolName()); // Set school name
         
         no++; // Increment the school counter
      }
   }
   
   /// Helper to create rows based on the grade levels of the school
   private void createRows(Sheet templateSheet, Workbook templateWorkbook, List<SchoolList_TableModel> selectedSchool) {
      int sourceRowIndex = 13; // The row index of the template row that will be copied
      int startColumn = 0;
      int endColumn = 16;
      int currentRowIndex = sourceRowIndex + 1; // Start inserting new rows just after the template row
      int rowCount = 0; // Keeps track of the number of rows added
      int maxRowCount = 22; // Maximum rows allowed in the first page before a new page starts
      int nextPageRowCount = 36; // Maximum rows allowed in the following pages
      
      Row row1 = templateSheet.getRow(0); // First row, assumed to be the page header
      Row sourceRow = templateSheet.getRow(sourceRowIndex); // Get the template row to copy
      
      // Loop through the list of selected schools
      for (SchoolList_TableModel schoolModel : selectedSchool) {
         int schoolGLvlCount = schoolModel.getTableSchoolGradeLevelList().size(); // Get the number of grade levels for the school
         
         // If adding all grade levels exceeds the max row limit, move to the next page
         if (rowCount + schoolGLvlCount > maxRowCount) {
            currentRowIndex += maxRowCount - rowCount; // Skip to the next available row for the next page
            rowCount = maxRowCount; // Reset the count to max to trigger new page logic
         }
         
         // Loop through the grade levels of the school
         for (SchoolGradeLevel_Model gradeLevelModel : schoolModel.getTableSchoolGradeLevelList()) {
            
            // If we reach maxRowCount, insert a new page header row and reset row count
            if (rowCount == maxRowCount) {
               Row newRow = templateSheet.createRow(currentRowIndex); // Create a new row for the page header
               copyRow(templateSheet, row1, newRow, startColumn, endColumn); // Copy the header row (assumed to be row 0)
               currentRowIndex++; // Move to the next row
               rowCount = 0; // Reset row count for the new page
               maxRowCount = nextPageRowCount; // New row count for the following page
            }
            
            // Create a new row and copy the structure from the template row
            Row newRow = templateSheet.createRow(currentRowIndex);
            copyRow(templateSheet, sourceRow, newRow, startColumn, endColumn);
            currentRowIndex++; // Move to the next row
            rowCount++; // Increase row count
         }
      }
      
      // Remove the original template row after copying is complete
      if (sourceRow != null) {
         templateSheet.removeRow(sourceRow); // Deletes the original template row
         
         // Shift the rows up to fill the empty space left by the removed row
         if (sourceRowIndex + 1 <= templateSheet.getLastRowNum()) {
            templateSheet.shiftRows(sourceRowIndex + 1, templateSheet.getLastRowNum(), -1);
         }
      }
      
      currentRowIndex += (maxRowCount - rowCount) - 1;
      
      Row newRow = templateSheet.createRow(currentRowIndex); // Create a new row for the page header
      copyRow(templateSheet, row1, newRow, startColumn, endColumn); // Copy the header row (assumed to be row 0)
      
      // Copy the sheet2 and remove it
      copySheet2(templateSheet, templateWorkbook, currentRowIndex + 2);
   }
   
   /// Method to copy sheet 2 to sheet 1 and remove the sheet 2
   private void copySheet2(Sheet templateSheet, Workbook templateWorkbook, int currentRowIndex) {
      // Get the second sheet (Sheet2) from the workbook
      Sheet sheet2 = templateWorkbook.getSheetAt(1);
      if (sheet2 == null) return; // Exit if sheet2 doesn't exist
      
      // Copy merged regions from Sheet2 to templateSheet
      for (int i = 0; i < sheet2.getNumMergedRegions(); i++) {
         CellRangeAddress mergedRegion = sheet2.getMergedRegion(i);
         CellRangeAddress newMergedRegion = new CellRangeAddress(
                 mergedRegion.getFirstRow() + currentRowIndex, // Adjust row position
                 mergedRegion.getLastRow() + currentRowIndex,
                 mergedRegion.getFirstColumn(),
                 mergedRegion.getLastColumn()
         );
         templateSheet.addMergedRegion(newMergedRegion); // Apply merged regions to templateSheet
      }
      
      // Copy rows from Sheet2 to templateSheet
      for (int i = 0; i <= sheet2.getLastRowNum(); i++) {
         Row sourceRow = sheet2.getRow(i);
         if (sourceRow == null) continue; // Skip empty rows
         
         // Create a new row in templateSheet at the appropriate index
         Row newRow = templateSheet.createRow(currentRowIndex + i);
         newRow.setHeight(sourceRow.getHeight()); // Copy row height
         
         // Copy each cell from the source row to the new row
         for (int j = 0; j < sourceRow.getLastCellNum(); j++) {
            Cell sourceCell = sourceRow.getCell(j);
            if (sourceCell == null) continue; // Skip empty cells
            
            // Create a new cell and copy the content type from the source cell
            Cell newCell = newRow.createCell(j, sourceCell.getCellType());
            newCell.setCellStyle(sourceCell.getCellStyle()); // Copy cell style (font, color, borders, etc.)
            
            // Copy the value based on the cell type
            switch (sourceCell.getCellType()) {
               case STRING:
                  newCell.setCellValue(sourceCell.getStringCellValue()); // Copy text values
                  break;
               case NUMERIC:
                  newCell.setCellValue(sourceCell.getNumericCellValue()); // Copy numeric values
                  break;
               case BOOLEAN:
                  newCell.setCellValue(sourceCell.getBooleanCellValue()); // Copy boolean values
                  break;
               case FORMULA:
                  newCell.setCellFormula(sourceCell.getCellFormula()); // Copy formulas
                  break;
               default:
                  newCell.setCellValue(sourceCell.toString()); // Copy any other content
            }
         }
      }
      
      // Delete the second sheet after copying its content
      int sheetIndex = templateWorkbook.getSheetIndex(sheet2);
      if (sheetIndex != -1) {
         templateWorkbook.removeSheetAt(sheetIndex);
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
   
   private void mergeCells(Sheet sheet, int firstRow, int lastRow, int firstCol, int lastCol) {
      for (int col = firstCol; col <= lastCol; col++) {
         sheet.addMergedRegion(new CellRangeAddress(firstRow, lastRow, col, col));
      }
   }
   
   public void copyRow(Sheet sheet, Row sourceRow, Row newRow, int startColumn, int endColumn) {
      newRow.setHeight(sourceRow.getHeight()); // Copy the row height
      
      for (int i = startColumn; i <= endColumn; i++) {
         Cell sourceCell = sourceRow.getCell(i);
         if (sourceCell == null) continue; // Skip empty cells in the source
         
         Cell newCell = newRow.getCell(i);
         if (newCell == null) {
            newCell = newRow.createCell(i); // Only create if it doesn't exist
         }
         
         newCell.setCellStyle(sourceCell.getCellStyle()); // Copy the cell style
         
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
      
      // Handle merged regions within the column range
      copyMergedRegions(sheet, sourceRow, newRow, startColumn, endColumn);
   }
   
   private void copyMergedRegions(Sheet sheet, Row sourceRow, Row newRow, int startColumn, int endColumn) {
      int sourceRowNum = sourceRow.getRowNum();
      int newRowNum = newRow.getRowNum();
      
      for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
         CellRangeAddress mergedRegion = sheet.getMergedRegion(i);
         
         // Check if the merged region is within the source row and within the column range
         if (mergedRegion.getFirstRow() == sourceRowNum &&
                 mergedRegion.getFirstColumn() >= startColumn &&
                 mergedRegion.getLastColumn() <= endColumn) {
            
            CellRangeAddress newMergedRegion = new CellRangeAddress(
                    newRowNum,
                    newRowNum + (mergedRegion.getLastRow() - mergedRegion.getFirstRow()),
                    mergedRegion.getFirstColumn(),
                    mergedRegion.getLastColumn()
            );
            sheet.addMergedRegion(newMergedRegion);
         }
      }
   }
   
   public static String formatNames(Set<String> regionNames) {
      List<String> regionList = new ArrayList<>(regionNames);
      if (regionList.size() == 1) {
         return regionList.get(0);
      } else if (regionList.size() == 2) {
         return regionList.get(0) + " & " + regionList.get(1);
      } else {
         String lastRegion = regionList.remove(regionList.size() - 1);
         return String.join(", ", regionList) + ", & " + lastRegion;
      }
   }
   
   private static ClientAnchor getClientAnchor(Workbook workbook) {
      CreationHelper helper = workbook.getCreationHelper();
      ClientAnchor anchor = helper.createClientAnchor();
      anchor.setCol1(1); // Column 1 (Column B)
      anchor.setCol2(2); // Column 2 (Column C)
      
      // Calculate the X coordinate within column D for the fractional value (for example, 0.9 for 3.9)
      int dx2 = Units.toEMU(10 * Units.DEFAULT_CHARACTER_WIDTH); // Assuming 0.9 is the fractional part
      anchor.setDx2(dx2);
      
      anchor.setRow1(1); // Adjusted for the new row start
      anchor.setRow2(1 + 4); // Adjusted for the new row end (adjust as needed)
      return anchor;
   }
}
