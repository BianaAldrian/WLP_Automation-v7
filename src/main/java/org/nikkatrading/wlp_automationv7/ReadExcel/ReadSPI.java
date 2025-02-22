package org.nikkatrading.wlp_automationv7.ReadExcel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.nikkatrading.wlp_automationv7.Models.SPI.SPIGradeLevel;
import org.nikkatrading.wlp_automationv7.Models.SPI.SPIItems;
import org.nikkatrading.wlp_automationv7.Models.SPI.SPILot;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ReadSPI {
   public List<SPIGradeLevel> spiGradeLevelList = new ArrayList<>();
   
   public ReadSPI() {
      String spi_excel_file_path = "res/excel/set_per_item_bases/set_per_item.xlsx";
      
      try (FileInputStream fis = new FileInputStream(spi_excel_file_path);
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
                  
                  List<SPILot> lotList = new ArrayList<>();  // Reset for each grade level
                  
                  for (int i = 0; i < spiSheet.getNumMergedRegions(); i++) {
                     CellRangeAddress mergedLotRegion = spiSheet.getMergedRegion(i);
                     
                     if (mergedLotRegion.getFirstRow() == 0 && mergedLotRegion.getLastRow() == 0) {
                        Cell primaryCell = lotRow.getCell(mergedLotRegion.getFirstColumn());
                        if (primaryCell != null) {
                           String lotCellValue = primaryCell.getStringCellValue();
                           
                           List<SPIItems> itemsList = new ArrayList<>();
                           for (int colIndex = mergedLotRegion.getFirstColumn(); colIndex <= mergedLotRegion.getLastColumn(); colIndex++) {
                              Cell itemCell = itemRow.getCell(colIndex);
                              Cell qtyCell = gradeLvlRow.getCell(colIndex);
                              
                              if (itemCell != null && qtyCell != null) {
                                 String itemCellValue = itemCell.getStringCellValue();
                                 int qtyCellValue = (int) qtyCell.getNumericCellValue();
                                 
                                 if (qtyCellValue != 0) {  // Only add items with quantity > 0
                                    itemsList.add(new SPIItems(itemCellValue, qtyCellValue));
                                 }
                              }
                           }
                           
                           if (!itemsList.isEmpty()) {  // Ensure the lot has items before adding
                              lotList.add(new SPILot(lotCellValue, itemsList));
                           }
                        }
                     }
                  }
                  
                  if (!lotList.isEmpty()) {  // Only add grade level if it has lots with items
                     spiGradeLevelList.add(new SPIGradeLevel(gradeLvlCellValue, lotList));
                  }
               }
            }
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
}
