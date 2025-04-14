package org.nikkatrading.wlp_automationv7.DB;

import org.nikkatrading.wlp_automationv7.Models.CBM.CBMGradeLevel;
import org.nikkatrading.wlp_automationv7.Models.CBM.CBMLot;
import org.nikkatrading.wlp_automationv7.Models.GeneratedSchoolModel;
import org.nikkatrading.wlp_automationv7.Models.SPI.SPIGradeLevel;
import org.nikkatrading.wlp_automationv7.Models.SPI.SPIItems;
import org.nikkatrading.wlp_automationv7.Models.SPI.SPILot;
import org.nikkatrading.wlp_automationv7.Models.LotsModel;
import org.nikkatrading.wlp_automationv7.Models.Table.SchoolGradeLevel_Model;
import org.nikkatrading.wlp_automationv7.Models.Table.SchoolList_TableModel;
import org.nikkatrading.wlp_automationv7.Models.Table.SchoolLot_Model;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GetAllocation {
   // Array List for storing the Region, Division, School, etc..
//   public static List<AllocRegion> allocRegionList = new ArrayList<>();
   public static Set<String> gatheredLots = new HashSet<>();
   
   public static List<SchoolList_TableModel> schoolListTable = new ArrayList<>();
//   public static List<DeliveredSchoolModel> deliveredSchoolList = new ArrayList<>();
//   public static List<GeneratedSchoolModel> generatedSchoolList = new ArrayList<>();
   
   // Fetch allocation from the epa_allocation database
   /*public static void fetchAllocation() {
      getAllocation();
      *//*getDeliveredSchool();
      getGeneratedSchool();*//*
   }*/
   
   /// Fetch the Allocation in epa_allocation Database
   public static List<SchoolList_TableModel> getAllocation(List<SPIGradeLevel> spiGradeLevelList, List<CBMGradeLevel> cbmGradeLevelList) {
      String sql = 
             """
             WITH RankedItems AS (
                 SELECT
                     r.region_name,
                     d.division_name,
                     s.school_id,
                     s.school_name,
                     gl.grade_level,
                     l.lot_name,
                     i.item_name,
                     sa.quantity,
                     ROW_NUMBER() OVER (
                         PARTITION BY s.school_id, gl.grade_level, l.lot_name
                         ORDER BY i.item_id ASC
                     ) AS rn
                 FROM school_allocations sa
                 JOIN schools s ON sa.school_id = s.school_id
                 JOIN grade_levels gl ON sa.grade_level_id = gl.grade_level_id
                 JOIN items i ON sa.item_id = i.item_id
                 JOIN lots l ON i.lot_id = l.lot_id
                 JOIN divisions d ON s.division_id = d.division_id
                 JOIN regions r ON d.region_id = r.region_id
             )
             SELECT
                 region_name,
                 division_name,
                 school_id,
                 school_name,
                 grade_level,
                 lot_name,
                 item_name,
                 quantity
             FROM RankedItems
             WHERE rn = 1  -- Only pick the first item per lot per grade level
             ORDER BY
                 region_name,
                 division_name,
                 school_id;
             """;
      
      try (Connection connection = GetConnection.getEpaAllocationConnection();
           PreparedStatement preparedStatement = connection.prepareStatement(sql);
           ResultSet resultSet = preparedStatement.executeQuery()) {
         
         while (resultSet.next()) {
            String regionName = resultSet.getString("region_name");
            String divisionName = resultSet.getString("division_name");
            int schoolId = resultSet.getInt("school_id");
            String schoolName = resultSet.getString("school_name");
            String gradeLevelName = resultSet.getString("grade_level");
            String lotName = resultSet.getString("lot_name");
            String itemName = resultSet.getString("item_name");
            int quantity = resultSet.getInt("quantity");
            
//            int schoolIndex = insertAll_Allocation(regionName, divisionName, schoolId, schoolName, gradeLevelName);
            int schoolIndex = insertAllocation(regionName, divisionName, schoolId, schoolName, gradeLevelName);
            int gradeLevelIndex = insertSchoolGradeLevel(schoolIndex, gradeLevelName);
            insertGradeLevelLot(schoolIndex, gradeLevelIndex, gradeLevelName, lotName, itemName, quantity, spiGradeLevelList, cbmGradeLevelList);
            
            gatheredLots.add(lotName);
         }
      } catch (SQLException e) {
         e.printStackTrace();
      }
      
      return schoolListTable;
   }
   
   
   
   /// Fetch the Delivered School in docdesk_db Database
   public static List<GeneratedSchoolModel> getDeliveredSchool() {
      List<GeneratedSchoolModel> generatedSchoolList = new ArrayList<>();
      String sql = "SELECT * FROM summary;";

      try (Connection connection = GetConnection.getDocdeskConnection();
           PreparedStatement preparedStatement = connection.prepareStatement(sql);
           ResultSet resultSet = preparedStatement.executeQuery()) {

         while (resultSet.next()) {
            
            List<LotsModel> lotsValueList = new ArrayList<>();
            
            int schoolId = resultSet.getInt("school_id");
//            String lot6 = resultSet.getString("lot_6_value");
//            String lot7 = resultSet.getString("lot_7_value");
//            String lot8 = resultSet.getString("lot_8_value");
//            String lot9 = resultSet.getString("lot_9_value");
//            String lot10 = resultSet.getString("lot_10_value");
//            String lot11 = resultSet.getString("lot_11_value");
//            String lot13 = resultSet.getString("lot_13_value");
//            String lot14 = resultSet.getString("lot_14_value");
            
            for (String lots : gatheredLots) {
               String strLotNum = lots.split(":")[0].replace("LOT ", "").trim();
               String lotColValue = resultSet.getString("lot_"+strLotNum+"_value");
               
               lotsValueList.add(new LotsModel(strLotNum, lotColValue));
            }
            
            generatedSchoolList.add(new GeneratedSchoolModel(schoolId,lotsValueList));

//            System.out.println("--------------------------------------------------");
//            System.out.println("Region: " + regionName);
//            System.out.println("Division: " + divisionName);
//            System.out.println("School ID: " + schoolId);
//            System.out.println("School Name: " + schoolName);
//            System.out.println("Lots Delivered:");
//            for (LotsModel lotsModel : lotsValueList) {
//               System.out.println("  Lot "+ lotsModel.getLotCol() +": " + lotsModel.getValue());
//            }
         }
      } catch (SQLException e) {
         e.printStackTrace();
      }
      return generatedSchoolList;
   }
   
   /// Fetch the Generated School in helpdesk_db Database
   public static List<GeneratedSchoolModel> getGeneratedSchool(List<GeneratedSchoolModel> deliveredSchool) {
      List<GeneratedSchoolModel> generatedSchoolList =
              deliveredSchool != null ? deliveredSchool : new ArrayList<>();
      
      String sql = "SELECT * FROM helpdesk_db.batch_info hb " +
              "JOIN helpdesk_db.workload_info hw ON hw.batch_id = hb.batch_id;";
      
      try (Connection connection = GetConnection.getHelpDeskConnection();
           PreparedStatement preparedStatement = connection.prepareStatement(sql);
           ResultSet resultSet = preparedStatement.executeQuery()) {
         
         while (resultSet.next()) {
            int schoolId = resultSet.getInt("school_id");
            
            List<LotsModel> newLotsValueList = new ArrayList<>();
            for (String lots : gatheredLots) {
               String strLotNum = lots.split(":")[0].replace("LOT ", "").trim();
               String lotColValue = resultSet.getString("lot_" + strLotNum);
               newLotsValueList.add(new LotsModel(strLotNum, lotColValue));
            }
            
            // Check if schoolId already exists in the list
            GeneratedSchoolModel existing = generatedSchoolList.stream()
                    .filter(s -> s.getSchoolId() == schoolId)
                    .findFirst()
                    .orElse(null);
            
            if (existing != null) {
               // Merge lot values: overwrite only if existing lot is NOT null
               for (LotsModel newLot : newLotsValueList) {
                  for (LotsModel existingLot : existing.getLotsValueList()) {
                     if (existingLot.getLotCol().equals(newLot.getLotCol())) {
                        if (existingLot.getValue() == null && newLot.getValue() != null) {
                           existingLot.setValue(newLot.getValue());
                        }
                     }
                  }
               }
            } else {
               // SchoolId not found, add new
               generatedSchoolList.add(new GeneratedSchoolModel(schoolId, newLotsValueList));
            }
         }
         
      } catch (SQLException e) {
         e.printStackTrace();
      }
      
     /* for (GeneratedSchoolModel school : generatedSchoolList) {
         System.out.println("School ID: " + school.getSchoolId());
         for (LotsModel lot : school.getLotsValueList()) {
            System.out.println("  Lot: " + lot.getLotCol() + " => Value: " + lot.getValue());
         }
      }*/
      
      return generatedSchoolList;
   }

   
   /*// Check if schoolId already exists in generatedSchoolList
            GeneratedSchoolModel existingModel = null;
            for (GeneratedSchoolModel model : generatedSchoolList) {
               if (model.getSchoolId() == schoolId) {
                  existingModel = model;
                  break;
               }
            }

            if (existingModel != null) {
               // Update only null values
               if (existingModel.getLot6() == null) existingModel.setLot6(lot6);
               if (existingModel.getLot7() == null) existingModel.setLot7(lot7);
               if (existingModel.getLot8() == null) existingModel.setLot8(lot8);
               if (existingModel.getLot9() == null) existingModel.setLot9(lot9);
               if (existingModel.getLot10() == null) existingModel.setLot10(lot10);
               if (existingModel.getLot11() == null) existingModel.setLot11(lot11);
               if (existingModel.getLot13() == null) existingModel.setLot13(lot13);
               if (existingModel.getLot14() == null) existingModel.setLot14(lot14);
            } else {
               // Add new entry if schoolId is not found
               generatedSchoolList.add(new GeneratedSchoolModel(schoolId, lot6, lot7, lot8, lot9, lot10, lot11, lot13, lot14));
            }*/
   
   /*public static List<SchoolList_TableModel> getSpecific_Allocation(List<String> selectedLot, List<SPIGradeLevel> spiGradeLevelList, List<CBMGradeLevel> cbmGradeLevelList) {
      schoolListTable.clear();
      
      String selectCondition = selectedLot.stream()
              .map(lot -> "MAX(NULLIF(TRIM(lot_" + lot + "), '')) AS lot_" + lot)
              .collect(Collectors.joining(",\n                  ")); // Indentation for readability
      
      String whereCondition = selectedLot.stream()
              .map(lot -> "(l.lot_name LIKE 'LOT " + lot + "%' AND (ds.lot_" + lot + "_value IS NULL) AND (hw.lot_" + lot + " IS NULL))")
              .collect(Collectors.joining("\n                  OR ")); // Indentation for readability
      
      String sql = 
            """
            WITH AggregatedWorkload AS (
                SELECT
                    school_id,
                    %s
                FROM helpdesk_db.workload_info
                GROUP BY school_id
            ),
            RankedItems AS (
                SELECT
                    r.region_name,
                    d.division_name,
                    s.school_id,
                    s.school_name,
                    gl.grade_level,
                    l.lot_name,
                    i.item_name,
                    sa.quantity,
                    ROW_NUMBER() OVER (
                        PARTITION BY s.school_id, gl.grade_level, l.lot_name
                        ORDER BY i.item_id ASC
                    ) AS rn
                FROM epa_allocation.school_allocations sa
                JOIN epa_allocation.schools s ON sa.school_id = s.school_id
                JOIN epa_allocation.grade_levels gl ON sa.grade_level_id = gl.grade_level_id
                JOIN epa_allocation.items i ON sa.item_id = i.item_id
                JOIN epa_allocation.lots l ON i.lot_id = l.lot_id
                JOIN epa_allocation.divisions d ON s.division_id = d.division_id
                JOIN epa_allocation.regions r ON d.region_id = r.region_id
                JOIN docdesk_db.delivered_schools ds ON s.school_id = ds.school_id
                LEFT JOIN AggregatedWorkload hw ON s.school_id = hw.school_id
                WHERE
                    (%s)
            )
            SELECT
                region_name,
                division_name,
                school_id,
                school_name,
                grade_level,
                lot_name,
                item_name,
                quantity
            FROM RankedItems
            WHERE rn = 1
            ORDER BY
                region_name,
                division_name,
                school_id;
            """.formatted(selectCondition, whereCondition);
      
         try (Connection connection = GetConnection.getEpaAllocationConnection();
              PreparedStatement preparedStatement = connection.prepareStatement(sql);
              ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
               String regionName = resultSet.getString("region_name");
               String divisionName = resultSet.getString("division_name");
               int schoolId = resultSet.getInt("school_id");
               String schoolName = resultSet.getString("school_name");
               String gradeLevelName = resultSet.getString("grade_level");
               String lotName = resultSet.getString("lot_name");
               String itemName = resultSet.getString("item_name");
               int quantity = resultSet.getInt("quantity");

               int schoolIndex = insertAllocation(regionName, divisionName, schoolId, schoolName, gradeLevelName);
               int gradeLevelIndex = insertSchoolGradeLevel(schoolIndex, gradeLevelName);
               insertGradeLevelLot(schoolIndex, gradeLevelIndex, gradeLevelName, lotName, itemName, quantity, spiGradeLevelList, cbmGradeLevelList);
            }
      } catch (SQLException e) {
         e.printStackTrace();
      }
         return schoolListTable;
   }*/
   
  /* private static int insertAll_Allocation(String regionName, String divisionName, int schoolId, String schoolName, String gradeLevelName) {
      // Check if the school exists in schoolListTable
      for (int i = 0; i < schoolListTable.size(); i++) {
         if (!schoolListTable.get(i).getRegion().equals(regionName)) continue;
         if (!schoolListTable.get(i).getDivision().equals(divisionName)) continue;
         if (schoolListTable.get(i).getSchoolID() == schoolId) {
            // School exists, update its grade levels
            schoolListTable.get(i).addGradeLevel(gradeLevelName);
            return i;
         }
      }
      
      // If the school doesn't exist, add a new one with the first grade level
      schoolListTable.add(new SchoolList_TableModel(regionName, divisionName, schoolId, schoolName, gradeLevelName, 0, null));
      return schoolListTable.size() - 1;
   }*/
   
   private static int insertAllocation(String regionName, String divisionName, int schoolId, String schoolName, String gradeLevelName) {
      // Check if the school exists in schoolListTable
      for (int i = 0; i < schoolListTable.size(); i++) {
         SchoolList_TableModel school = schoolListTable.get(i);
         
         if (!school.getRegion().equals(regionName)) continue;
         if (!school.getDivision().equals(divisionName)) continue;
         if (school.getSchoolID() == schoolId) {
            // School exists, update its grade levels
            school.addGradeLevel(gradeLevelName);
            return i; // Return the index if found
         }
      }
      
      // If the school doesn't exist, add a new grade level list per school
      List<SchoolGradeLevel_Model> newSchoolGradeLevelModelList = new ArrayList<>();
      schoolListTable.add(new SchoolList_TableModel(regionName, divisionName, schoolId, schoolName, gradeLevelName, 0, newSchoolGradeLevelModelList));
      return schoolListTable.size() - 1;
   }
   
   private static int insertSchoolGradeLevel(int schoolIndex, String gradeLevelName) {
      // Get the list of grade levels for the specified school
      List<SchoolGradeLevel_Model> schoolGradeLevelList = schoolListTable.get(schoolIndex).getTableSchoolGradeLevelList();
      
      // check if the grade level already exist in the school grade level list
      for (int i = 0; i < schoolGradeLevelList.size(); i++) {
         if (schoolGradeLevelList.get(i).getGradeLevel().equals(gradeLevelName)) {
            return i; // Return index if found
         }
      }
      
      // If grade level doesn't exist, add new SchoolGradeLevel list
      List<SchoolLot_Model> newSchoolLotlList = new ArrayList<>();
      schoolGradeLevelList.add(new SchoolGradeLevel_Model(gradeLevelName, newSchoolLotlList));
      return schoolGradeLevelList.size() - 1;
   }
   
   /*private static void insertGradeLevelLot(int schoolIndex, int gradeLevelIndex, String gradeLevelName, String lotName, String itemName, int quantity, List<SPIGradeLevel> spiGradeLevelList, List<CBMGradeLevel> cbmGradeLevelList) {
      // Get the school to add the total CBM
      
      // Get the list of lot list for the specified school grade level
      List<SchoolLot_Model> schoolLotlList = schoolListTable.get(schoolIndex).getTableSchoolGradeLevelList().get(gradeLevelIndex).getTableSchoolLotList();
      
      int spi = getSPI(spiGradeLevelList, gradeLevelName, lotName, itemName, quantity);
      double cbm = getCBM(cbmGradeLevelList, gradeLevelName, lotName, spi);
      
      schoolLotlList.add(new SchoolLot_Model(lotName, spi, cbm));
      
//      System.out.println(gradeLevelName + " - SPI: " + spi + " CBM: " + cbm);
      
      SchoolList_TableModel school = schoolListTable.get(schoolIndex);
      school.addCbm(cbm);
   }*/
   
   private static void insertGradeLevelLot(int schoolIndex, int gradeLevelIndex, String gradeLevelName, String lotName, String itemName, int quantity, List<SPIGradeLevel> spiGradeLevelList, List<CBMGradeLevel> cbmGradeLevelList) {
      // Get the school to add the total CBM
      
      // Get the list of lot list for the specified school grade level
      List<SchoolLot_Model> schoolLotlList = schoolListTable.get(schoolIndex).getTableSchoolGradeLevelList().get(gradeLevelIndex).getTableSchoolLotList();
      
      int spi = getSPI(spiGradeLevelList, gradeLevelName, lotName, itemName, quantity);
      double cbm = getCBM(cbmGradeLevelList, gradeLevelName, lotName, spi);
      
      schoolLotlList.add(new SchoolLot_Model(lotName, spi, cbm));

//      System.out.println(gradeLevelName + " - SPI: " + spi + " CBM: " + cbm);
      
      /*SchoolList_TableModel school = schoolListTable.get(schoolIndex);
      school.addCbm(cbm);*/
   }
   
   private static int getSPI(List<SPIGradeLevel> spiGradeLevelList, String gradeLevelName, String lotName, String itemName, int quantity) {
      int spi = 0;
      
      for (SPIGradeLevel spiGradeLevel : spiGradeLevelList) {
         // Loop through each SPIGradeLevel in spiGradeLevelList
         
         if (spiGradeLevel.getGradeLevel().equalsIgnoreCase(gradeLevelName.split("_")[0])) {
            // Check if the grade level matches the first part of gradeLevelName (before "_")
            
            for (SPILot spiLot : spiGradeLevel.getSpiLotList()) {
               // Loop through each SPILot in the matched SPIGradeLevel
               
               if (gradeLevelName.contains("2021")) {
                  // If gradeLevelName contains "2021", filter lots related to 2021
                  
                  if (spiLot.getLot().contains("2021")) {
                     // If the SPILot name also contains "2021", proceed to check items
                     
                     for (SPIItems spiItems : spiLot.getItemsList()) {
                        // Loop through items in the matched SPILot
                        
                        if (spiItems.getItem().equalsIgnoreCase(itemName)) {
                           // If the item name matches, calculate SPI
                           spi = quantity / spiItems.getQty();
                           return spi; // Return early once found
                        }
                     }
                  }
                  
               } else {
                  // If gradeLevelName does not contain "2021", compare the lot name directly
                  
                  if (spiLot.getLot().equalsIgnoreCase(lotName)) {
                     // If the SPILot name matches lotName, proceed to check items
                     
                     for (SPIItems spiItems : spiLot.getItemsList()) {
                        // Loop through items in the matched SPILot
                        
                        if (spiItems.getItem().equalsIgnoreCase(itemName)) {
                           // If the item name matches, calculate SPI
                           spi = quantity / spiItems.getQty();
                           return spi; // Return early once found
                        }
                     }
                  }
               }
            }
         }
      }

      // If no matching item is found, the method will return whatever default value is set for 'spi' (if any)
      
      return spi;
   }
   
   private static double getCBM(List<CBMGradeLevel> cbmGradeLevelList, String gradeLevelName, String lotName, int spi) {
      double cbm = 0;
      
      for (CBMGradeLevel cbmGradeLevel : cbmGradeLevelList) {
         // Loop through each CBMGradeLevel in cbmGradeLevelList
         
         if (cbmGradeLevel.getGradeLevel().equalsIgnoreCase(gradeLevelName.split("_")[0])) {
            // Check if the grade level matches the first part of gradeLevelName (before "_")
            
            for (CBMLot cbmLot : cbmGradeLevel.getCbmLotList()) {
               // Loop through each CBMLot in the matched CBMGradeLevel
               
               if (gradeLevelName.contains("2021")) {
                  // If gradeLevelName contains "2021", filter lots related to 2021
                  
                  if (cbmLot.getLot().contains("2021")) {
                     // If the CBMLot name also contains "2021", calculate cbm
                     cbm = cbmLot.getCbm() * spi;
                     return cbm; // Return early once found
                  }
                  
               } else {
                  // If gradeLevelName does not contain "2021", use the regular lot name comparison
                  
                  if (cbmLot.getLot().equalsIgnoreCase(lotName.split(":")[0].trim())) {
                     // Check if CBMLot name matches the first part of lotName (before ":"), ignoring case
                     cbm = cbmLot.getCbm() * spi;
                     return cbm; // Return early once found
                  }
               }
            }
         }
      }
      
      // If no matching lot is found, the method will return whatever default value is set for 'cbm' (if any)
      
      return cbm;
   }
}
