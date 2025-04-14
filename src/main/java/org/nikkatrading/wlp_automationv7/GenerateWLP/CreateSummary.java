package org.nikkatrading.wlp_automationv7.GenerateWLP;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.nikkatrading.wlp_automationv7.Models.KG.KGGradeLevel;
import org.nikkatrading.wlp_automationv7.Models.KG.KGItem;
import org.nikkatrading.wlp_automationv7.Models.KG.KGLot;
import org.nikkatrading.wlp_automationv7.Models.SPI.SPIGradeLevel;
import org.nikkatrading.wlp_automationv7.Models.SPI.SPIItems;
import org.nikkatrading.wlp_automationv7.Models.SPI.SPILot;
import org.nikkatrading.wlp_automationv7.Models.Table.SchoolGradeLevel_Model;
import org.nikkatrading.wlp_automationv7.Models.Table.SchoolList_TableModel;
import org.nikkatrading.wlp_automationv7.Models.Table.SchoolLot_Model;
import org.nikkatrading.wlp_automationv7.Utility.SessionData;
import org.nikkatrading.wlp_automationv7.Utility.WLPContext;

import java.util.*;

public class CreateSummary {
   
   private boolean insideMotherbox = false;
   private boolean insideLot8MB1 = false;
   private boolean insideLot8MB2 = false;
   private boolean insideLot8MBJHS = false;
   
   private int overAllTotalQty = 0;
   private double overallTotalWeight = 0;
   
   private int totalStandAlone = 0;
   private int totalMotherbox = 0;
   
   private int footerRow = 0;
   
   public CreateSummary(WLPContext context, String formattedTime, String batchNo, Workbook templateWorkbook, Workbook summaryTemplateWorkbook, List<SchoolList_TableModel> selectedSchool) {
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
      Sheet summarySheet = createSummarySheet(context, templateWorkbook, summaryTemplateSheet, summaryTemplateWorkbook, formattedTime, batchNo, availableLot, selectedSchool);
      
      // Create "Packing List" and copy data from "Summary"
      Sheet packingListSheet = createPackingListSheet(summaryTemplateSheet, templateWorkbook, summarySheet);
      
      // Delete column 8 of summarySheet
      removeCol8OfSummarySheet(summarySheet);
      
      // Delete Motherbox items of packing list
      deleteMotherboxItems(packingListSheet);
      
      // Get Stand Alone and Motherbox total count
      getTotalCount(summarySheet);
      
      // put header in summary sheet
      putPageHeader(summarySheet);
      
      // put header in packing list sheet
      putPageHeader(packingListSheet);
      
      SessionData.getInstance().setTotal_standalone(totalStandAlone);
      SessionData.getInstance().setTotal_motherbox(totalMotherbox);
   }
   
   private Sheet createSummarySheet(WLPContext context, Workbook templateWorkbook, Sheet summaryTemplateSheet, Workbook summaryTemplateWorkbook, String formattedTime, String batchNo, Set<Integer> availableLot, List<SchoolList_TableModel> selectedSchool) {
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
      
      setCellValue(summarySheet, templateWorkbook, 0, 1, context.version); // set the version in row 1 col C;
      setCellValue(summarySheet, templateWorkbook, 0, 5, formattedTime); // set the time in row 1 col F;
      setCellValue(summarySheet, templateWorkbook, 0, 6, batchNo); // set the batch no in row 1 col H;
      setCellValue(summarySheet, templateWorkbook, 2, 0, "Summary"); // set the batch no in row 3 col A;
      
      setValueInSummarySheet(context, summaryTemplateWorkbook, summaryTemplateSheet, summarySheet, availableLot, selectedSchool);
      setWeightInSummarySheet(context, summaryTemplateWorkbook, summarySheet);
      
      // Copy the footer part of the summary template to new summary sheet
      Row sourceFooterRow = summaryTemplateSheet.getRow(211);
      Row newFooterRow = summarySheet.createRow(footerRow);
      
      copyRow(summaryTemplateSheet, summarySheet, sourceFooterRow, newFooterRow, 0, summarySheet.getLastRowNum());
      setCellValue(summarySheet, summaryTemplateWorkbook, footerRow, 6, overAllTotalQty); // over all total
      setCellValue(summarySheet, summaryTemplateWorkbook, footerRow, 8, Math.round(overallTotalWeight * 100.0) / 100.0); // total weight
      
      remove0TotalInSummarySheet(summarySheet);
      
      return summarySheet;
   }
   
   private Sheet createPackingListSheet(Sheet summaryTemplateSheet, Workbook templateWorkbook, Sheet summarySheet) {
      Sheet packingListSheet = templateWorkbook.createSheet("Packing List");
      copyPageLayout(summaryTemplateSheet, packingListSheet);
      
      for (int i = 0; i <= summarySheet.getLastRowNum(); i++) {
         Row sourceRow = summarySheet.getRow(i);
         if (sourceRow != null) {
            Row newRow = packingListSheet.createRow(i);
            copyRow(summarySheet, packingListSheet, sourceRow, newRow, 0, sourceRow.getLastCellNum() - 1);
         }
      }
      setCellValue(packingListSheet, templateWorkbook, 2, 0, "Packing List"); // set the batch no in row 3 col A;
      
      return packingListSheet;
   }
   
   private void setValueInSummarySheet(WLPContext context, Workbook summaryTemplateWorkbook, Sheet summaryTemplateSheet, Sheet summarySheet, Set<Integer> availableLot, List<SchoolList_TableModel> selectedSchool) {
      // Get the list of lot summary data
      List<LotSummary_Model> lotSummaryList = templateLotRows();
      
      // Initialize row index in the summarySheet (starts from row 3)
      int currentRowIndex = 3;
      
      // Iterate through the lot summary list
      for (LotSummary_Model lotSummaryModel : lotSummaryList) {
         // Check if the lot is in the availableLot set before processing
         if (availableLot.contains(lotSummaryModel.getLot())) {
            insideMotherbox = false;
            insideLot8MB1 = false;
            insideLot8MB2 = false;
            insideLot8MBJHS = false;
            
            int i = lotSummaryModel.getStartRow(); // Start processing from the lot's start row
            
            Row lotHeaderRow = summaryTemplateSheet.getRow(i);
            
            // Iterate through all rows within the lot's defined range
            while (i <= lotSummaryModel.getEndRow()) {
               Row summarySourceRow = summaryTemplateSheet.getRow(i); // Get the row from the template
               
               if (summarySourceRow != null) {
                  // Determine if the current row is part of a merged region
                  int mergedStart = i, mergedEnd = i;
                  for (int j = 0; j < summaryTemplateSheet.getNumMergedRegions(); j++) {
                     CellRangeAddress mergedRegion = summaryTemplateSheet.getMergedRegion(j);
                     if (mergedRegion.getFirstRow() == i) {
                        mergedEnd = mergedRegion.getLastRow();
                        break; // Exit loop once the merged region is found
                     }
                  }
                  
                  // Copy the merged rows into the summary sheet
                  int blockStartRowIndex = currentRowIndex; // Remember where this merged block starts
                  for (int k = mergedStart; k <= mergedEnd; k++) {
                     Row mergedRow = summaryTemplateSheet.getRow(k);
                     if (mergedRow != null) {
                        Row newRow = summarySheet.createRow(currentRowIndex);
                        copyRow(summaryTemplateSheet, summarySheet, mergedRow, newRow, 0, mergedRow.getLastCellNum() - 1);
                        currentRowIndex++;
                     }
                  }

                  // ✅ Set values once for the whole merged block using the first row of the merged group
                  setItemValues(summarySheet, summaryTemplateWorkbook, lotHeaderRow, summarySourceRow, blockStartRowIndex, selectedSchool, context.spiGradeLevelList);
                  
                  
                  // Skip over rows that were part of the merged region
                  i = mergedEnd + 1;
               } else {
                  i++; // If no row is found, simply move to the next row
               }
            }
            
            // Add an extra space after each lot for better readability
            currentRowIndex++;
         }
      }
      
      footerRow = currentRowIndex;
   }
   
   /// Update this method for the specific conditions per lot
   // Method to set a value in the created empty summary per lot
   private void setItemValues(Sheet summarySheet, Workbook summaryTemplateWorkbook, Row lotHeaderRow, Row summarySourceRow, int row, List<SchoolList_TableModel> selectedSchool, List<SPIGradeLevel> spiGradeLevelList) {
      Cell lotCell = lotHeaderRow.getCell(0);
      String lotName = getCellValue(lotCell).trim();
      
      Cell itemCell = summarySourceRow.getCell(0);
      String itemName = getCellValue(itemCell).toLowerCase().replace("• ","").trim();
      
      int totalQty = 0;
      
      if (!itemName.contains("lot")) {
         
         // Loop through the grade level header
         for (int i = 1; i <= 6; i++) {
            Cell headerCell = lotHeaderRow.getCell(i); // grade level and total cells
            String headerColValue = getCellValue(headerCell).trim();
            
            int set = 0;
            int qty = 0;
            
            //** Condition to calculate the quantity of item per lot and grade level **//
            // Loop through the selected school list
            for (SchoolList_TableModel schoolModel : selectedSchool) {
               // Loop through the school grade level list
               for (SchoolGradeLevel_Model gradeLevelModel : schoolModel.getTableSchoolGradeLevelList()) {
                  
                  if (lotName.contains("2021")) {
                     // Check if the school grade level match the grade level cell in summary
                     if (gradeLevelModel.getGradeLevel().contains(headerColValue+"_2021")) {
                        // Loop through the lots of the school grade level
                        for (SchoolLot_Model lotModel : gradeLevelModel.getTableSchoolLotList()) {
                           // Check if the lot of the summary matches the grade level lot
                           
                           if (lotName.contains(lotModel.getLotName().split(":")[0].trim())) {
                              
                              // Loop through the grade level of the set per item bases
                              for (SPIGradeLevel spiGradeLevel : spiGradeLevelList) {
                                 // Check if the spi grade level matches the school grade level
                                 if (spiGradeLevel.getGradeLevel().equals(headerColValue)) {
                                    // Loop through the lot of the set per item bases
                                    for (SPILot spiLot : spiGradeLevel.getSpiLotList()) {
                                       if (spiLot.getLot().contains("2021")) {
                                          // Loop through the items of the set per item bases
                                          for (SPIItems spiItems : spiLot.getItemsList()) {
                                             // Check if the summary item match the item in the set per item
                                             if (itemName.contains(spiItems.getItem().toLowerCase().trim())) {
                                                if (lotModel.getSetPerItem() != 0) {
                                                   qty += spiItems.getQty() * lotModel.getSetPerItem();
                                                }
                                                break;
                                             }
                                          }
                                          break;
                                       }
                                    }
                                    break;
                                 }
                              }
                              set += lotModel.getSetPerItem();
                              break;
                           }
                        }
                        break;
                     }
                  } else {
                     // Check if the school grade level match the grade level cell in summary
                     if (gradeLevelModel.getGradeLevel().contains(headerColValue)) {
                        // Loop through the lots of the school grade level
                        for (SchoolLot_Model lotModel : gradeLevelModel.getTableSchoolLotList()) {
                           // Check if the lot of the summary matches the grade level lot
                           if (lotName.contains(lotModel.getLotName().split(":")[0].trim())) {
                              
                              // Loop through the grade level of the set per item bases
                              for (SPIGradeLevel spiGradeLevel : spiGradeLevelList) {
                                 // Check if the spi grade level matches the school grade level
                                 if (spiGradeLevel.getGradeLevel().equals(headerColValue)) {
                                    // Loop through the lot of the set per item bases
                                    for (SPILot spiLot : spiGradeLevel.getSpiLotList()) {
                                       // Check if the lot summary has 2021
                                       if (spiLot.getLot().equals(lotModel.getLotName())) {
                                          for (SPIItems spiItems : spiLot.getItemsList()) {
                                             if (itemName.contains(spiItems.getItem().toLowerCase().trim())) {
                                                if (lotModel.getSetPerItem() != 0) {
                                                   qty += spiItems.getQty() * lotModel.getSetPerItem();
                                                }
                                                break;
                                             }
                                          }
                                          break;
                                       }
                                    }
                                    break;
                                 }
                              }
                              set += lotModel.getSetPerItem();
                              break;
                           }
                        }
                        break;
                     }
                  }
               }
            }
            
            //** Modify this function based on the requirements per lot **//
            switch (lotName) {
               case "LOT 6" -> {
                  if (itemName.equalsIgnoreCase("Balance, Toploading, Electronic") || itemName.equalsIgnoreCase("Balance, Triple Beam, with tare, 2610-gram")) {
                     if (insideMotherbox) {
                        if (qty != 0) {
                           setCellValue(summarySheet, summaryTemplateWorkbook, row, i, set);
                           totalQty += set;
                        }
                     } else {
                        if (qty != 0) {
                           int stanAloneCount = qty - set;
                           
                           setCellValue(summarySheet, summaryTemplateWorkbook, row, i, stanAloneCount);
                           totalQty += stanAloneCount;
                        }
                     }
                  } else {
                     if (qty != 0) {
                        setCellValue(summarySheet, summaryTemplateWorkbook, row, i, qty);
                        totalQty += qty;
                     }
                  }
                  
                  if (itemName.contains("motherbox")) {
                     insideMotherbox = true;
                     if (set != 0) {
                        setCellValue(summarySheet, summaryTemplateWorkbook, row, i, set);
                        totalQty += set;
                     }
                  }
                  
                  if (headerColValue.equals("Total")) {
                     setCellValue(summarySheet, summaryTemplateWorkbook, row, i, totalQty);
                  }
               }
               case "LOT 8" -> {
                  if (insideLot8MB1) {
                     if (headerColValue.equals("JHS")) {
                        setCellValue(summarySheet, summaryTemplateWorkbook, row, i, "");
                     }
                     
                     if (itemName.equalsIgnoreCase("Geostrips") || itemName.equalsIgnoreCase("Pattern Blocks, 250 pcs/set")) {
                        if (headerColValue.equals("G1toG3")) {
                           setCellValue(summarySheet, summaryTemplateWorkbook, row, i, "");
                        }
                        else if (headerColValue.equals("G4toG6")) {
                           if (qty != 0) {
                              setCellValue(summarySheet, summaryTemplateWorkbook, row, i, qty);
                              totalQty += qty;
                           }
                        }
                     } else {
                        if (qty != 0) {
                           setCellValue(summarySheet, summaryTemplateWorkbook, row, i, qty);
                           totalQty += qty;
                        }
                     }
                  }
                  else if (insideLot8MB2) {
                     if (headerColValue.equals("JHS")) {
                        setCellValue(summarySheet, summaryTemplateWorkbook, row, i, "");
                     }
                     
                     if (itemName.equalsIgnoreCase("Geostrips") || itemName.equalsIgnoreCase("Pattern Blocks, 250 pcs/set")) {
                        if (headerColValue.equals("G1toG3")) {
                           if (qty != 0) {
                              setCellValue(summarySheet, summaryTemplateWorkbook, row, i, qty);
                              totalQty += qty;
                           }
                        }
                        else if (headerColValue.equals("G4toG6")) {
                           setCellValue(summarySheet, summaryTemplateWorkbook, row, i, "");
                        }
                     } else {
                        if (qty != 0) {
                           setCellValue(summarySheet, summaryTemplateWorkbook, row, i, qty);
                           totalQty += qty;
                        }
                     }
                  }
                  else if (insideLot8MBJHS) {
                     if (headerColValue.equals("JHS")) {
                        if (qty != 0) {
                           setCellValue(summarySheet, summaryTemplateWorkbook, row, i, qty);
                           totalQty += qty;
                        }
                     } else {
                        setCellValue(summarySheet, summaryTemplateWorkbook, row, i, "");
                     }
                  } else {
                     if (qty != 0) {
                        setCellValue(summarySheet, summaryTemplateWorkbook, row, i, qty);
                        totalQty += qty;
                     }
                  }
                  
                  switch (itemName) {
                     case "motherbox 1" -> {
                        insideLot8MB1 = true;
                        insideLot8MB2 = false;
                        insideLot8MBJHS = false;
                        
                        if (!headerColValue.equals("JHS")) {
                           if (set != 0) {
                              setCellValue(summarySheet, summaryTemplateWorkbook, row, i, set);
                              totalQty += set;
                           }
                        }
                        if (headerColValue.equals("SHSCore")) {
                           totalQty -= set;
//                           setCellValue(summarySheet, summaryTemplateWorkbook, row, i, "");
                        }
                     }
                     case "motherbox 2" -> {
                        insideLot8MB1 = false;
                        insideLot8MB2 = true;
                        insideLot8MBJHS = false;
                        if (!headerColValue.equals("JHS")) {
                           if (set != 0) {
                              setCellValue(summarySheet, summaryTemplateWorkbook, row, i, set);
                              totalQty += set;
                           }
                        }
                        if (headerColValue.equals("SHSCore")) {
                           totalQty -= set;
//                           setCellValue(summarySheet, summaryTemplateWorkbook, row, i, "");
                        }
                     }
                     case "motherbox (jhs)" -> {
                        insideLot8MB1 = false;
                        insideLot8MB2 = false;
                        insideLot8MBJHS = true;
                        if (headerColValue.equals("JHS")) {
                           if (set != 0) {
                              setCellValue(summarySheet, summaryTemplateWorkbook, row, i, set);
                              totalQty += set;
                           }
                        }
                     }
                  }
                  if (headerColValue.equals("Total")) {
                     setCellValue(summarySheet, summaryTemplateWorkbook, row, i, totalQty);
                  }
               }
               case "LOT 9" -> {
                  if (itemName.equalsIgnoreCase("Balance, Double-pan, 500-gram")) {
                     if (insideMotherbox) {
                        if (qty != 0) {
                           setCellValue(summarySheet, summaryTemplateWorkbook, row, i, set);
                           totalQty += set;
                        }
                     } else {
                        if (qty != 0) {
                           int stanAloneCount = qty - set;
                           setCellValue(summarySheet, summaryTemplateWorkbook, row, i, stanAloneCount);
                           totalQty += stanAloneCount;
                        }
                     }
                  } else {
                     if (qty != 0) {
                        setCellValue(summarySheet, summaryTemplateWorkbook, row, i, qty);
                        totalQty += qty;
                     }
                  }
                  
                  if (itemName.contains("motherbox")) {
                     insideMotherbox = true;
                     if (set != 0) {
                        setCellValue(summarySheet, summaryTemplateWorkbook, row, i, set);
                        totalQty += set;
                     }
                  }
                  
                  if (headerColValue.equals("Total")) {
                     setCellValue(summarySheet, summaryTemplateWorkbook, row, i, totalQty);
                  }
               }
               case "LOT 14" -> {
                  if (qty != 0) {
                     setCellValue(summarySheet, summaryTemplateWorkbook, row, i, qty);
                     totalQty += qty;
                  }
                  if (itemName.contains("motherbox")) {
                     if (headerColValue.equals("G1toG3") || headerColValue.equals("G4toG6")) {
                        setCellValue(summarySheet, summaryTemplateWorkbook, row, i, "");
                     } else {
                        if (set != 0) {
                           setCellValue(summarySheet, summaryTemplateWorkbook, row, i, set);
                           totalQty += set;
                        }
                     }
                  }
                  
                  if (headerColValue.equals("Total")) {
                     setCellValue(summarySheet, summaryTemplateWorkbook, row, i, totalQty);
                  }
               }
               case "LOT 14 (2021)" -> {
                  if (qty != 0) {
                     setCellValue(summarySheet, summaryTemplateWorkbook, row, i, qty);
                     totalQty += qty;
                  }
                  
                  if (itemName.contains("motherbox")) {
                     if (headerColValue.equals("SHSCore")) {
                        setCellValue(summarySheet, summaryTemplateWorkbook, row, i, "");
                     } else {
                        if (set != 0) {
                           setCellValue(summarySheet, summaryTemplateWorkbook, row, i, set);
                           totalQty += set;
                        }
                     }
                  }
                  
                  if (headerColValue.equals("Total")) {
                     setCellValue(summarySheet, summaryTemplateWorkbook, row, i, totalQty);
                  }
               }
               default -> {
                  if (headerColValue.equals("Total")) {
                     setCellValue(summarySheet, summaryTemplateWorkbook, row, i, totalQty);
                     
                     if (itemName.contains("motherbox")) {
                        setCellValue(summarySheet, summaryTemplateWorkbook, row, i, totalQty);
                     }
                  } else {
                     if (qty != 0) {
                        setCellValue(summarySheet, summaryTemplateWorkbook, row, i, qty);
                        totalQty += qty;
                     }
                     
                     if (itemName.contains("motherbox")) {
                        if (set != 0) {
                           setCellValue(summarySheet, summaryTemplateWorkbook, row, i, set);
                           totalQty += set;
                        }
                     }
                  }
               }
            }
         }
      }
      
      overAllTotalQty += totalQty;
   }
   
   private void setWeightInSummarySheet(WLPContext context, Workbook summaryTemplateWorkbook, Sheet summarySheet) {
      // Initialize row index in the summarySheet (starts from row 3)
      int currentRowIndex = 3;
      int itemCol = 0;
      int weightCol = 8;
      
      String lotName = "";
      Row finalLotRow = null;
      
      for (int row = currentRowIndex; row < summarySheet.getLastRowNum(); row++) {
         Row lotRow = summarySheet.getRow(row);
         if (lotRow != null) {
            Cell lotCell = lotRow.getCell(0);
            if (lotCell != null) {
               if (getCellValue(lotCell).contains("LOT")) {
                  lotName = getCellValue(lotCell);
                  finalLotRow = lotRow;
                  continue;
               }
            }
            
            Row itemRow = summarySheet.getRow(row);
            if (itemRow != null) {
               Cell itemCell = itemRow.getCell(itemCol);
               
               if (itemCell != null) {
                  String itemName = getCellValue(itemCell);
                  if (!itemName.contains("• ")) {
                     if (finalLotRow != null) {
                        
                        double totalWeight = 0;
                        
                        for (int col = 1; col <= finalLotRow.getLastCellNum(); col++) {
                           Cell headerCell = finalLotRow.getCell(col);
                           if (headerCell != null) {
                              String headerVal = getCellValue(headerCell);
                              
                              if (headerVal.equals("Weight (kg)")) {
                                 setCellValue(summarySheet, summaryTemplateWorkbook, row, weightCol, Math.round(totalWeight * 100.0) / 100.0);
                                 overallTotalWeight += totalWeight;
                              } else {
                                 Cell qtyCell = itemRow.getCell(col);
                                 if (!getCellValue(qtyCell).isEmpty()) {
                                    int qty = Integer.parseInt(getCellValue(qtyCell));
                                    for (KGGradeLevel kgGradeLevel : context.kgGradeLevelList) {
                                       if (kgGradeLevel.getGradeLvlName().equals(headerVal)) {
                                          for (KGLot kgLot : kgGradeLevel.getLotList()) {
                                             if (kgLot.getLotName().equals(lotName)) {
                                                for (KGItem kgItem : kgLot.getItemList()) {
                                                   if (kgItem.getItemName().equalsIgnoreCase(itemName)) {
                                                      totalWeight += qty * kgItem.getKg();
                                                      break;
                                                   }
                                                }
                                                break;
                                             }
                                          }
                                          break;
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }
   
   private void deleteMotherboxItems(Sheet packingListSheet) {
      int lastRow = packingListSheet.getLastRowNum();
      
      for (int i = lastRow; i >= 3; i--) {
         Row row = packingListSheet.getRow(i);
         if (row == null) continue;
         
         Cell cell = row.getCell(0);
         if (cell != null && cell.getCellType() == CellType.STRING && cell.getStringCellValue().contains("• ")) {
            
            // Track merged rows to delete
            List<Integer> rowsToDelete = new ArrayList<>();
            rowsToDelete.add(i); // Always delete the current row
            
            // Remove merged regions in A–I that intersect this row, and collect all involved rows
            for (int j = packingListSheet.getNumMergedRegions() - 1; j >= 0; j--) {
               CellRangeAddress mergedRegion = packingListSheet.getMergedRegion(j);
               
               if (mergedRegion.getFirstRow() <= i && mergedRegion.getLastRow() >= i &&
                       mergedRegion.getFirstColumn() >= 0 && mergedRegion.getLastColumn() <= 8) {
                  
                  // Collect all rows in this merged region
                  for (int r = mergedRegion.getFirstRow(); r <= mergedRegion.getLastRow(); r++) {
                     if (!rowsToDelete.contains(r)) {
                        rowsToDelete.add(r);
                     }
                  }
                  
                  // Remove the merged region
                  packingListSheet.removeMergedRegion(j);
               }
            }
            
            // Sort and delete all affected rows from bottom to top
            Collections.sort(rowsToDelete, Collections.reverseOrder());
            for (int rowIndex : rowsToDelete) {
               Row r = packingListSheet.getRow(rowIndex);
               if (r != null) {
                  packingListSheet.removeRow(r);
               }
            }
            
            // Shift remaining rows up
            int maxRowToDelete = Collections.max(rowsToDelete);
            int minRowToDelete = Collections.min(rowsToDelete);
            int numRowsDeleted = maxRowToDelete - minRowToDelete + 1;
            
            if (maxRowToDelete < lastRow) {
               packingListSheet.shiftRows(maxRowToDelete + 1, lastRow, -numRowsDeleted);
               lastRow -= numRowsDeleted;
               i = maxRowToDelete; // Move loop back to reprocess shifted content
            }
         }
      }
   }
   
   private void getTotalCount(Sheet summarySheet) {
      for (int rowIndex = 3; rowIndex <= summarySheet.getLastRowNum() - 2; rowIndex++) {
         Row row = summarySheet.getRow(rowIndex);
         
         if (row != null) {
            Cell itemCell = row.getCell(0);
            Cell totalCell = row.getCell(6);
            
            String itemName = getCellValue(itemCell);
            
            if (!itemName.contains("•") && !itemName.contains("LOT") && !itemName.contains("Motherbox") && !itemName.isEmpty()) {
               int total = Integer.parseInt(getCellValue(totalCell));
               
               totalStandAlone += total;
            }
            
            if (itemName.contains("Motherbox")) {
               int total = Integer.parseInt(getCellValue(totalCell));
               
               totalMotherbox += total;
            }
         }
         
         /*if (row != null) {
            
            for (int colIndex = 1; colIndex < row.getLastCellNum(); colIndex++) {
               Cell cell = row.getCell(colIndex);
               if (cell != null) {
                  // Process the cell value
                  System.out.println("Row " + rowIndex + ", Col " + colIndex + ": " + cell.toString());
               }
            }
         }*/
      }
   }
   
   
   private void remove0TotalInSummarySheet(Sheet summarySheet) {
      int lastRow = summarySheet.getLastRowNum();
      
      for (int i = lastRow; i >= 3; i--) {
         Row row = summarySheet.getRow(i);
         if (row == null) continue;
         
         Cell cell = row.getCell(6);
         if (cell != null && cell.getCellType() == CellType.NUMERIC && cell.getNumericCellValue() == 0) {
            
            // Track merged rows to delete
            List<Integer> rowsToDelete = new ArrayList<>();
            rowsToDelete.add(i); // Always delete the current row
            
            // Remove merged regions in A–I that intersect this row, and collect all involved rows
            for (int j = summarySheet.getNumMergedRegions() - 1; j >= 0; j--) {
               CellRangeAddress mergedRegion = summarySheet.getMergedRegion(j);
               
               if (mergedRegion.getFirstRow() <= i && mergedRegion.getLastRow() >= i &&
                       mergedRegion.getFirstColumn() >= 0 && mergedRegion.getLastColumn() <= 8) {
                  
                  // Collect all rows in this merged region
                  for (int r = mergedRegion.getFirstRow(); r <= mergedRegion.getLastRow(); r++) {
                     if (!rowsToDelete.contains(r)) {
                        rowsToDelete.add(r);
                     }
                  }
                  
                  // Remove the merged region
                  summarySheet.removeMergedRegion(j);
               }
            }
            
            // Sort and delete all affected rows from bottom to top
            Collections.sort(rowsToDelete, Collections.reverseOrder());
            for (int rowIndex : rowsToDelete) {
               Row r = summarySheet.getRow(rowIndex);
               if (r != null) {
                  summarySheet.removeRow(r);
               }
            }
            
            // Shift remaining rows up
            int maxRowToDelete = Collections.max(rowsToDelete);
            int minRowToDelete = Collections.min(rowsToDelete);
            int numRowsDeleted = maxRowToDelete - minRowToDelete + 1;
            
            if (maxRowToDelete < lastRow) {
               summarySheet.shiftRows(maxRowToDelete + 1, lastRow, -numRowsDeleted);
               lastRow -= numRowsDeleted;
               i = maxRowToDelete; // Move loop back to reprocess shifted content
            }
         }
      }
   }
   
   private void removeCol8OfSummarySheet(Sheet summarySheet) {
      int lastRow = summarySheet.getLastRowNum();
      
      // Iterate through all rows
      for (int i = 0; i <= lastRow; i++) {
         Row row = summarySheet.getRow(i);
         if (row != null) {
            Cell cell = row.getCell(8); // Column 8 (Column I)
            if (cell != null) {
               row.removeCell(cell); // Remove the cell
            }
            
            // Shift remaining columns to the left if necessary
            for (int j = 9; j < row.getLastCellNum(); j++) {
               Cell leftCell = row.getCell(j - 1);
               Cell rightCell = row.getCell(j);
               
               if (rightCell != null) {
                  if (leftCell == null) {
                     leftCell = row.createCell(j - 1);
                  }
                  leftCell.setCellValue(rightCell.getStringCellValue()); // Copy value
                  leftCell.setCellStyle(rightCell.getCellStyle()); // Copy style
                  row.removeCell(rightCell); // Remove old cell
               }
            }
         }
      }
      
      // Handle merged regions that include column 8
      for (int i = summarySheet.getNumMergedRegions() - 1; i >= 0; i--) {
         CellRangeAddress mergedRegion = summarySheet.getMergedRegion(i);
         
         if (mergedRegion.getFirstColumn() <= 8 && mergedRegion.getLastColumn() >= 8) {
            summarySheet.removeMergedRegion(i);
         } else if (mergedRegion.getFirstColumn() > 8) {
            // Shift merged region one column left
            CellRangeAddress newRegion = new CellRangeAddress(
                    mergedRegion.getFirstRow(), mergedRegion.getLastRow(),
                    mergedRegion.getFirstColumn() - 1, mergedRegion.getLastColumn() - 1
            );
            summarySheet.removeMergedRegion(i);
            summarySheet.addMergedRegion(newRegion);
         }
      }
   }
   
   /*private void putPageHeader(Sheet sheet) {
      int lastRowNum = sheet.getLastRowNum();
      System.out.println("=== Row Information Including Merged Rows ===");
      
      int rowIndex = 0;
      while (rowIndex <= lastRowNum) {
         if (rowIndex != 0 && rowIndex % 31 == 0) {
            int insertIndex = rowIndex;
            
            Row previousRow = sheet.getRow(rowIndex - 1);
            Cell firstCell = (previousRow != null) ? previousRow.getCell(0) : null;
            String cellValue = (firstCell != null) ? getCellValue(firstCell) : "";
            
            System.out.printf(">>> Inserting header at row %d%n", insertIndex);
            System.out.println("    Item Name: " + cellValue);
            
            boolean isSpecialRow = cellValue.contains("LOT") || cellValue.contains("Motherbox");
            int shiftCount = isSpecialRow ? 2 : 1;
            int shiftStart = isSpecialRow ? insertIndex - 1 : insertIndex;
            
            if (isSpecialRow) {
               System.out.printf(">>> Row %d contains 'LOT' or 'Motherbox', shifting down.%n", shiftStart);
            }
            
            // Check if the row falls within a merged region
            if (isRowInMergedRegion(sheet, insertIndex)) {
               System.out.printf(">>> Row %d is within a merged region, adjusting insertion%n", insertIndex);
               insertIndex += shiftCount;  // Adjust position to avoid overlap
               shiftStart += shiftCount;
            }
            
            sheet.shiftRows(shiftStart, lastRowNum, shiftCount, true, false);
            lastRowNum += shiftCount;
            
            Row sourceHeader = sheet.getRow(0);
            if (sourceHeader != null) {
               Row newHeader = sheet.createRow(insertIndex);
               copyRow(sheet, sheet, sourceHeader, newHeader, 0, lastRowNum);
            } else {
               System.err.println(">>> ERROR: Header row at index 0 is null.");
            }
            
            rowIndex += shiftCount;
         } else {
            rowIndex++;
         }
      }
   }
   
   // Helper method to check if a row falls within a merged region
   private boolean isRowInMergedRegion(Sheet sheet, int rowIndex) {
      for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
         CellRangeAddress mergedRegion = sheet.getMergedRegion(i);
         if (rowIndex >= mergedRegion.getFirstRow() && rowIndex <= mergedRegion.getLastRow()) {
            return true;
         }
      }
      return false;
   }*/
   
   ///  Backup for putting header
   private void putPageHeader(Sheet sheet) {
      int lastRowNum = sheet.getLastRowNum();
      
      System.out.println("=== Row Information Including Merged Rows ===");
      
      int rowIndex = 0;
      while (rowIndex <= lastRowNum) {
         // Every 31 rows (excluding the first row), insert a header
         if (rowIndex != 0 && rowIndex % 31 == 0) {
            int insertIndex = rowIndex;
            
            // Check if the 31st row is part of a merged region
            boolean isMergedRow = false;
            int mergedRowStart = -1;
            for (CellRangeAddress mergedRegion : sheet.getMergedRegions()) {
               if (mergedRegion.getFirstRow() <= rowIndex && rowIndex <= mergedRegion.getLastRow()) {
                  isMergedRow = true;
                  mergedRowStart = mergedRegion.getFirstRow();
                  break;
               }
            }
            
            // If part of merged rows, use the first row of the merged region
            if (isMergedRow) {
               insertIndex = mergedRowStart;
               System.out.printf(">>> Row %d is part of a merged region, starting at row %d.%n", rowIndex, mergedRowStart);
            }
            
            Row previousRow = sheet.getRow(rowIndex - 1);
            Cell firstCell = (previousRow != null) ? previousRow.getCell(0) : null;
            String cellValue = (firstCell != null) ? getCellValue(firstCell) : "";
            
            System.out.printf(">>> Inserting header at row %d%n", insertIndex);
            System.out.println("    Item Name: " + cellValue);
            
            boolean isSpecialRow = cellValue.contains("LOT") || cellValue.contains("Motherbox");
            int shiftCount = isSpecialRow ? 2 : 1;
            int shiftStart = isSpecialRow ? insertIndex - 1 : insertIndex;
            
            if (isSpecialRow) {
               System.out.printf(">>> Row %d contains 'LOT' or 'Motherbox', shifting down.%n", shiftStart);
            }
            
            sheet.shiftRows(shiftStart, lastRowNum, shiftCount, true, false);
            lastRowNum += shiftCount;
            
            if (insertIndex != 0 && insertIndex % 31 == 0) {
               // Copy header row from index 0 to the new insertion point
               Row sourceHeader = sheet.getRow(0);
               if (sourceHeader != null) {
                  Row newHeader = sheet.createRow(insertIndex);
                  copyRow(sheet, sheet, sourceHeader, newHeader, 0, lastRowNum);
               } else {
                  System.err.println(">>> ERROR: Header row at index 0 is null.");
               }
            }
            
            rowIndex = insertIndex + shiftCount; // Skip over the inserted rows
         } else {
            rowIndex++;
         }
      }
   }
   
   /// Helper Method to get cell value as a string
   private String getCellValue(Cell cell) {
      if (cell != null) {
         return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            case BLANK -> "";
            default -> "Unsupported cell type";
         };
      }
      return "Unknown Cell Type";
   }
   
   /// Helper to set a value in a cell without affecting its current style
   private void setCellValue(Sheet sheet, Workbook workbook, int rowIndex, int colIndex, Object value) {
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
   
   /// Helper method to copy the exactly specific row
   private void copyRow(Sheet sourceSheet, Sheet targetSheet, Row sourceRow, Row newRow, int startColumn, int endColumn) {
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
   
   /// Help method to copy the merged cells
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
   
   /// Helper method to copy the exact style of the page
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
      
      // Copy header and footer
      Header sourceHeader = sourceSheet.getHeader();
      Footer sourceFooter = sourceSheet.getFooter();
      
      Header targetHeader = targetSheet.getHeader();
      Footer targetFooter = targetSheet.getFooter();
      
      targetHeader.setLeft(sourceHeader.getLeft());
      targetHeader.setCenter(sourceHeader.getCenter());
      targetHeader.setRight(sourceHeader.getRight());
      
      targetFooter.setLeft(sourceFooter.getLeft());
      targetFooter.setCenter(sourceFooter.getCenter());
      targetFooter.setRight(sourceFooter.getRight());
   }
   
   // Template for summary
   /** When the summary template has changed, always change this method also **/
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
