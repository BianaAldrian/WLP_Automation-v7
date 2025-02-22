package org.nikkatrading.wlp_automationv7.ReadExcel;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.nikkatrading.wlp_automationv7.Models.CBM.CBMGradeLevel;
import org.nikkatrading.wlp_automationv7.Models.CBM.CBMLot;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ReadCBM {
   public List<CBMGradeLevel> cbmGradeLevelList = new ArrayList<>();
   
   public ReadCBM() {
      String cbm_excel_file_path = "res/excel/cbm_bases/cbm_data.xlsx";
      
      try (FileInputStream fis = new FileInputStream(cbm_excel_file_path);
           Workbook workbook = new XSSFWorkbook(fis)) {
         
         Sheet cbmSheet = workbook.getSheetAt(0);
         
         int gradeLevelCol = 0;
         
         Row lotRow = cbmSheet.getRow(0);
         int lastCol = lotRow.getLastCellNum(); // Get last column index (1-based)
         
         for (int row = 1; row <= cbmSheet.getLastRowNum(); row++) { // Loop through all rows
            Row gradeLevelRow = cbmSheet.getRow(row);
            if (gradeLevelRow != null) {
               
               String gradeLevel = (gradeLevelRow.getCell(gradeLevelCol) != null)
                       ? gradeLevelRow.getCell(gradeLevelCol).toString().trim()
                       : ""; // Grade Level
               
               List<CBMLot> cbmLotList = new ArrayList<>();
               for (int col = 1; col < lastCol; col++) {  // Iterate through actual columns
                  Cell lotCell = lotRow.getCell(col);
                  Cell cbmCell = gradeLevelRow.getCell(col);
                  
                  if (lotCell != null && cbmCell != null) {
                     String lot = lotCell.toString().trim(); // Lot Name
                     double cbm = 0.0;
                     
                     // Ensure CBM value is read correctly
                     if (cbmCell.getCellType() == CellType.NUMERIC) {
                        cbm = cbmCell.getNumericCellValue();
                     } else if (cbmCell.getCellType() == CellType.STRING) {
                        try {
                           cbm = Double.parseDouble(cbmCell.getStringCellValue().trim());
                        } catch (NumberFormatException e) {
                           cbm = 0.0; // If parsing fails, assume 0
                        }
                     }
                     
                     if (cbm != 0.0) {
                        cbmLotList.add(new CBMLot(lot, cbm));
                     }
                  }
               }
               
               if (!cbmLotList.isEmpty()) {
                  cbmGradeLevelList.add(new CBMGradeLevel(gradeLevel, cbmLotList));
               }
            }
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
}
