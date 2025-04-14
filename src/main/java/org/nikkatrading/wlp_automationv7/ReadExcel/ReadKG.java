package org.nikkatrading.wlp_automationv7.ReadExcel;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.nikkatrading.wlp_automationv7.Models.KG.KGGradeLevel;
import org.nikkatrading.wlp_automationv7.Models.KG.KGItem;
import org.nikkatrading.wlp_automationv7.Models.KG.KGLot;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ReadKG {
   public List<KGGradeLevel> kgGradeLevelList = new ArrayList<>();
   
   public ReadKG() {
      String kg_excel_file_path = "res/excel/kg_bases/kg_data.xlsx";
      
      /*try (FileInputStream fis = new FileInputStream(kg_excel_file_path);
           Workbook workbook = new XSSFWorkbook(fis)) {
         
         Sheet kgSheet = workbook.getSheetAt(0);
         
         Row lotRow = kgSheet.getRow(0);
         Row itemRow = kgSheet.getRow(1);
         int lastCol = lotRow.getLastCellNum(); // Get last column index (1-based)
         
         for (int row = 3; row <= kgSheet.getLastRowNum(); row++) {
            Row gradeLvlRow = kgSheet.getRow(row);
            if (gradeLvlRow != null) {
               
               String gradeLevel = (gradeLvlRow.getCell(0) != null)
                       ? gradeLvlRow.getCell(0).toString().trim()
                       : ""; // Grade Level
               
               System.out.println(gradeLevel);
               
               for (int col = 1; col <= lastCol; col++) {
                  Cell lotCell = lotRow.getCell(col);
                  Cell itemCell = itemRow.getCell(col);
                  Cell kgCell = gradeLvlRow.getCell(col);
                  
                  if (lotCell != null) {
                     String lot = lotCell.toString().trim(); // Lot Name
                     
                  }
               }
            
            }
         }
         
      } catch (IOException e) {
         e.printStackTrace();
      }*/
      
      try (FileInputStream fis = new FileInputStream(kg_excel_file_path);
           Workbook workbook = new XSSFWorkbook(fis)) {
         
         Sheet spiSheet = workbook.getSheetAt(0);
         
         // Fetch the header row (row 1) for lot names
         Row lotRow = spiSheet.getRow(0);
         Row itemRow = spiSheet.getRow(1);
         
         // Loop through rows 3 to 7 (index 2 to 6)
         for (int rowIndex = 2; rowIndex <= spiSheet.getLastRowNum(); rowIndex++) {
            Row gradeLvlRow = spiSheet.getRow(rowIndex);
            if (gradeLvlRow != null) {
               Cell gradeLvlCell = gradeLvlRow.getCell(0);
               if (gradeLvlCell != null) {
                  String gradeLvlCellValue = gradeLvlCell.getStringCellValue();
                  
                  List<KGLot> lotList = new ArrayList<>();  // Reset for each grade level
                  
                  for (int i = 0; i < spiSheet.getNumMergedRegions(); i++) {
                     CellRangeAddress mergedLotRegion = spiSheet.getMergedRegion(i);
                     
                     if (mergedLotRegion.getFirstRow() == 0 && mergedLotRegion.getLastRow() == 0) {
                        Cell primaryCell = lotRow.getCell(mergedLotRegion.getFirstColumn());
                        if (primaryCell != null) {
                           String lotCellValue = primaryCell.getStringCellValue();
                           
                           List<KGItem> itemsList = new ArrayList<>();
                           for (int colIndex = mergedLotRegion.getFirstColumn(); colIndex <= mergedLotRegion.getLastColumn(); colIndex++) {
                              Cell itemCell = itemRow.getCell(colIndex);
                              Cell kgCell = gradeLvlRow.getCell(colIndex);
                              
                              if (itemCell != null && kgCell != null) {
                                 String itemCellValue = itemCell.getStringCellValue();
                                 double qtyCellValue = kgCell.getNumericCellValue();
                                 
                                 if (qtyCellValue != 0) {  // Only add items with quantity > 0
                                    itemsList.add(new KGItem(itemCellValue, qtyCellValue));
                                 }
                              }
                           }
                           
                           if (!itemsList.isEmpty()) {  // Ensure the lot has items before adding
                              lotList.add(new KGLot(lotCellValue, itemsList));
                           }
                        }
                     }
                  }
                  
                  if (!lotList.isEmpty()) {  // Only add grade level if it has lots with items
                     kgGradeLevelList.add(new KGGradeLevel(gradeLvlCellValue, lotList));
                  }
               }
            }
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
}
