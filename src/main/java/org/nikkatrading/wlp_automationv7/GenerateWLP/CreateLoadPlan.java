package org.nikkatrading.wlp_automationv7.GenerateWLP;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.util.Units;
import org.nikkatrading.wlp_automationv7.Models.Table.SchoolGradeLevel_Model;
import org.nikkatrading.wlp_automationv7.Models.Table.SchoolList_TableModel;
import org.nikkatrading.wlp_automationv7.Models.Table.SchoolLot_Model;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class CreateLoadPlan {
   
   public CreateLoadPlan(String version, String formattedTime, String formattedDate, int batchCounter, Sheet templateSheet, Workbook templateWorkbook, List<SchoolList_TableModel> selectedSchool) {
      setRow1To7Value(templateSheet, templateWorkbook, version, formattedTime, formattedDate, batchCounter, selectedSchool);
   }
   
   private void setRow1To7Value(Sheet templateSheet, Workbook templateWorkbook, String version, String formattedTime, String formattedDate, int batchCounter, List<SchoolList_TableModel> selectedSchool) {
      // Gather unique lots, region, and division names
      Set<String> regionNames = new LinkedHashSet<>();
      Set<String> divisionNames = new LinkedHashSet<>();
      Set<Integer> lots = new TreeSet<>(); // Use TreeSet for automatic sorting

      // Populate region and division names
      for (SchoolList_TableModel schoolModel : selectedSchool) {
         regionNames.add(schoolModel.getRegion());
         divisionNames.add(schoolModel.getDivision());
         
         for (SchoolGradeLevel_Model gradeLevelModel : schoolModel.getTableSchoolGradeLevelList()) {
            
            // Populate lots
            for (SchoolLot_Model lotModel : gradeLevelModel.getTableSchoolLotList()) {
               String lotString = lotModel.getLotName().split(":")[0].replace("LOT", "").trim();
               try {
                  int lotNumber = Integer.parseInt(lotString);
                  lots.add(lotNumber); // Store as an Integer for correct sorting
               } catch (NumberFormatException e) {
                  System.err.println("Invalid lot number format: " + lotString);
               }
            }
         }
      }

      // Format names
      String regionNamesString = formatNames(regionNames);
      String divisionNamesString = formatNames(divisionNames);
      String lotString = lots.stream().map(String::valueOf).collect(Collectors.joining(", "));
      
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
      
      setCellValue(templateSheet, templateWorkbook, 0, 6, version); // set the version in row 1 col 7;
      setCellValue(templateSheet, templateWorkbook, 0, 9, formattedTime); // set the time in row 1 col 10;
      setCellValue(templateSheet, templateWorkbook, 0, 11, "Batch No. " + formattedDate + "-" + batchCounter); // set the batch no in row 1 col 12;
      setCellValue(templateSheet, templateWorkbook, 3, 12, "R - " + regionNamesString); // set the region in the row 4 col 13;
      setCellValue(templateSheet, templateWorkbook, 5, 1, "# " + divisionNamesString); // set the division in row 6 col 2;
      setCellValue(templateSheet, templateWorkbook, 6, 4, "Lot " + lotString); // set the selected lot in row 7 col 5;
      setCellValue(templateSheet, templateWorkbook, 6, 5, "Control No: R" + regionNamesString + " - " + formattedDate + "-" + batchCounter); // set the control number in row 7 col 6;
      setCellValue(templateSheet, templateWorkbook, 6, 14, selectedSchool.size()); // set the number of selected school in row 7 col 15;
   }
   
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
      } else if (value instanceof java.util.Date) {
         CreationHelper createHelper = workbook.getCreationHelper();
         cell.setCellValue((java.util.Date) value);
         cell.setCellStyle(originalStyle); // Retain date formatting
      } else {
         cell.setCellValue(value.toString()); // Fallback to string
      }
      
      // Restore the original cell style
      cell.setCellStyle(originalStyle);
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
