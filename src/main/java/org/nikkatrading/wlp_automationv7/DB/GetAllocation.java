package org.nikkatrading.wlp_automationv7.DB;

import org.nikkatrading.wlp_automationv7.Models.CBM.CBMGradeLevel;
import org.nikkatrading.wlp_automationv7.Models.CBM.CBMLot;
import org.nikkatrading.wlp_automationv7.Models.DeliveredSchoolModel;
import org.nikkatrading.wlp_automationv7.Models.GeneratedSchoolModel;
import org.nikkatrading.wlp_automationv7.Models.SPI.SPIGradeLevel;
import org.nikkatrading.wlp_automationv7.Models.SPI.SPIItems;
import org.nikkatrading.wlp_automationv7.Models.SPI.SPILot;
import org.nikkatrading.wlp_automationv7.Models.Table.SchoolGradeLevel_Model;
import org.nikkatrading.wlp_automationv7.Models.Table.SchoolList_TableModel;
import org.nikkatrading.wlp_automationv7.Models.Table.SchoolLot_Model;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GetAllocation {
   // Array List for storing the Region, Division, School, etc..
//   public static List<AllocRegion> allocRegionList = new ArrayList<>();
   public static Set<String> gatheredLots = new HashSet<>();
   
   public static List<SchoolList_TableModel> schoolListTable = new ArrayList<>();
   public static List<DeliveredSchoolModel> deliveredSchoolList = new ArrayList<>();
   public static List<GeneratedSchoolModel> generatedSchoolList = new ArrayList<>();
   
   // Fetch allocation from the epa_allocation database
   public static void fetchAllocation(List<SPIGradeLevel> spiGradeLevelList, List<CBMGradeLevel> cbmGradeLevelList) {
      getAllocation(spiGradeLevelList, cbmGradeLevelList);
      getDeliveredSchool();
      getGeneratedSchool();
   }
   
   /// Fetch the Allocation in epa_allocation Database
   private static void getAllocation(List<SPIGradeLevel> spiGradeLevelList, List<CBMGradeLevel> cbmGradeLevelList) {
      String sql =
              "WITH RankedItems AS (\n" +
                      "    SELECT \n" +
                      "        r.region_name, \n" +
                      "        d.division_name, \n" +
                      "        s.school_id, \n" +
                      "        s.school_name, \n" +
                      "        gl.grade_level, \n" +
                      "        l.lot_name, \n" +
                      "        i.item_name, \n" +
                      "        sa.quantity,\n" +
                      "        ROW_NUMBER() OVER (\n" +
                      "            PARTITION BY s.school_id, gl.grade_level, l.lot_name \n" +
                      "            ORDER BY i.item_id ASC\n" +
                      "        ) AS rn\n" +
                      "    FROM school_allocations sa\n" +
                      "    JOIN schools s ON sa.school_id = s.school_id\n" +
                      "    JOIN grade_levels gl ON sa.grade_level_id = gl.grade_level_id\n" +
                      "    JOIN items i ON sa.item_id = i.item_id\n" +
                      "    JOIN lots l ON i.lot_id = l.lot_id\n" +
                      "    JOIN divisions d ON s.division_id = d.division_id\n" +
                      "    JOIN regions r ON d.region_id = r.region_id\n" +
                      ")\n" +
                      "SELECT \n" +
                      "    region_name, \n" +
                      "    division_name, \n" +
                      "    school_id, \n" +
                      "    school_name, \n" +
                      "    grade_level, \n" +
                      "    lot_name, \n" +
                      "    item_name, \n" +
                      "    quantity\n" +
                      "FROM RankedItems\n" +
                      "WHERE rn = 1  -- Only pick the first item per lot per grade level\n" +
                      "ORDER BY \n" +
                      "    region_name, \n" +
                      "    division_name, \n" +
                      "    school_id;\n";
      
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
   }
   
   /// Fetch the Delivered School in docdesk_db Database
   private static void getDeliveredSchool() {
      String sql = "SELECT * FROM summary;";
      
      try (Connection connection = GetConnection.getDocdeskConnection();
           PreparedStatement preparedStatement = connection.prepareStatement(sql);
           ResultSet resultSet = preparedStatement.executeQuery()) {
         
         while (resultSet.next()) {
            String regionName = resultSet.getString("region_name");
            String divisionName = resultSet.getString("division_name");
            int schoolId = resultSet.getInt("school_id");
            String schoolName = resultSet.getString("school_name");
            String lot6 = resultSet.getString("lot_6_value");
            String lot7 = resultSet.getString("lot_7_value");
            String lot8 = resultSet.getString("lot_8_value");
            String lot9 = resultSet.getString("lot_9_value");
            String lot10 = resultSet.getString("lot_10_value");
            String lot11 = resultSet.getString("lot_11_value");
            String lot13 = resultSet.getString("lot_13_value");
            String lot14 = resultSet.getString("lot_14_value");
            
            deliveredSchoolList.add(new DeliveredSchoolModel(regionName, divisionName, schoolId, schoolName, lot6, lot7, lot8, lot9, lot10, lot11, lot13, lot14));
            
            /*System.out.println("--------------------------------------------------");
            System.out.println("Region: " + regionName);
            System.out.println("Division: " + divisionName);
            System.out.println("School ID: " + schoolId);
            System.out.println("School Name: " + schoolName);
            System.out.println("Lots Delivered:");
            System.out.println("  Lot 6: " + lot6);
            System.out.println("  Lot 7: " + lot7);
            System.out.println("  Lot 8: " + lot8);
            System.out.println("  Lot 9: " + lot9);
            System.out.println("  Lot 10: " + lot10);
            System.out.println("  Lot 11: " + lot11);
            System.out.println("  Lot 13: " + lot13);
            System.out.println("  Lot 14: " + lot14);*/
         }
      } catch (SQLException e) {
         e.printStackTrace();
      }
   }
   
   /// Fetch the Generated School in helpdesk_db Database
   private static void getGeneratedSchool() {
      String sql = "SELECT * FROM helpdesk_db.batch_info hb JOIN helpdesk_db.workload_info hw ON hw.batch_id = hb.batch_id;";
      
      try (Connection connection = GetConnection.getHelpDeskConnection();
           PreparedStatement preparedStatement = connection.prepareStatement(sql);
           ResultSet resultSet = preparedStatement.executeQuery()) {
         
         // Use "MMMM dd, yyyy" for full month name format
         SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy");
         
         while (resultSet.next()) {
            int schoolId = resultSet.getInt("school_id");
            String lot6 = resultSet.getString("lot_6");
            String lot7 = resultSet.getString("lot_7");
            String lot8 = resultSet.getString("lot_8");
            String lot9 = resultSet.getString("lot_9");
            String lot10 = resultSet.getString("lot_10");
            String lot11 = resultSet.getString("lot_11");
            String lot13 = resultSet.getString("lot_13");
            String lot14 = resultSet.getString("lot_14");
            
            // Check if schoolId already exists in generatedSchoolList
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
            }
         }
      } catch (SQLException e) {
         e.printStackTrace();
      }
   }
   
//   private static void getSpecific_Allocation(List<String> selectedLot, List<SPIGradeLevel> spiGradeLevelList, List<CBMGradeLevel> cbmGradeLevelList) {
//      schoolListTable.clear();
//
//      // Generate the lot column dynamically
//      String lotColCondition = selectedLot.stream()
//              .map(lot -> "ds.lot_" + lot + "_value")
//              .collect(Collectors.joining(","));
//
//      // Generate the lot conditions dynamically
//      String lotCondition = selectedLot.stream()
//              .map(lot -> "(l.lot_name LIKE 'LOT " + lot + "%' AND ds.lot_" + lot + "_value IS NULL)")
//              .collect(Collectors.joining(" OR "));
//
//      // This query will check if the school already delivered
//      // Get 1 item per lot only to minimize the data
//      String sql =    "WITH RankedItems AS (\n" +
//                      "    SELECT \n" +
//                      "        r.region_name, \n" +
//                      "        d.division_name, \n" +
//                      "        s.school_id, \n" +
//                      "        s.school_name, \n" +
//                      "        gl.grade_level, \n" +
//                      "        l.lot_name, \n" +
//                      "        i.item_name, \n" +
//                      "        sa.quantity,\n" +
//                      "        " + lotColCondition + "," +
//                      "        ROW_NUMBER() OVER (\n" +
//                      "            PARTITION BY s.school_id, gl.grade_level, l.lot_name \n" +
//                      "            ORDER BY i.item_id ASC\n" +
//                      "        ) AS rn\n" +
//                      "    FROM epa_allocation.school_allocations sa\n" +
//                      "    JOIN epa_allocation.schools s ON sa.school_id = s.school_id\n" +
//                      "    JOIN epa_allocation.grade_levels gl ON sa.grade_level_id = gl.grade_level_id\n" +
//                      "    JOIN epa_allocation.items i ON sa.item_id = i.item_id\n" +
//                      "    JOIN epa_allocation.lots l ON i.lot_id = l.lot_id\n" +
//                      "    JOIN epa_allocation.divisions d ON s.division_id = d.division_id\n" +
//                      "    JOIN epa_allocation.regions r ON d.region_id = r.region_id\n" +
//                      "    JOIN docdesk_db.summary ds ON s.school_id = ds.school_id\n" +
//                      "    WHERE \n" +
//                      "        (" + lotCondition + ")\n" +
//                      ")\n" +
//                      "SELECT \n" +
//                      "    region_name, \n" +
//                      "    division_name, \n" +
//                      "    school_id, \n" +
//                      "    school_name, \n" +
//                      "    grade_level, \n" +
//                      "    lot_name, \n" +
//                      "    item_name, \n" +
//                      "    quantity\n" +
//                      "FROM RankedItems\n" +
//                      "WHERE rn = 1  -- Only pick the first item per lot per grade level\n" +
//                      "ORDER BY \n" +
//                      "    region_name, \n" +
//                      "    division_name, \n" +
//                      "    school_id;\n";
//
//
//         try (Connection connection = GetConnection.getEpaAllocationConnection();
//              PreparedStatement preparedStatement = connection.prepareStatement(sql);
//              ResultSet resultSet = preparedStatement.executeQuery()) {
//
//            while (resultSet.next()) {
//               String regionName = resultSet.getString("region_name");
//               String divisionName = resultSet.getString("division_name");
//               int schoolId = resultSet.getInt("school_id");
//               String schoolName = resultSet.getString("school_name");
//               String gradeLevelName = resultSet.getString("grade_level");
//               String lotName = resultSet.getString("lot_name");
//               String itemName = resultSet.getString("item_name");
//               int quantity = resultSet.getInt("quantity");
//
//               System.out.println("Region: " + regionName);
//               System.out.println(" - Division: " + divisionName);
//               System.out.println("   - School ID: " + schoolId);
//               System.out.println("   - School Name: " + schoolName);
//               System.out.println("     - Grade Level: " + gradeLevelName);
//               System.out.println("       - Lot: " + lotName);
//               System.out.println("         - Item: " + itemName);
//               System.out.println("         - Quantity: " + quantity);
//               System.out.println("-----------------------------"); // Separator for each row
//
//               int schoolIndex = insertAllocation(regionName, divisionName, schoolId, schoolName, gradeLevelName);
//               int gradeLevelIndex = insertSchoolGradeLevel(schoolIndex, gradeLevelName);
//               insertGradeLevelLot(schoolIndex, gradeLevelIndex, gradeLevelName, lotName, itemName, quantity, spiGradeLevelList, cbmGradeLevelList);
//            }
//      } catch (SQLException e) {
//         e.printStackTrace();
//      }
//   }
   
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
   
   private static void insertGradeLevelLot(int schoolIndex, int gradeLevelIndex, String gradeLevelName, String lotName, String itemName, int quantity, List<SPIGradeLevel> spiGradeLevelList, List<CBMGradeLevel> cbmGradeLevelList) {
      // Get the school to add the total CBM
      
      // Get the list of lot list for the specified school grade level
      List<SchoolLot_Model> schoolLotlList = schoolListTable.get(schoolIndex).getTableSchoolGradeLevelList().get(gradeLevelIndex).getTableSchoolLotList();
      
      int spi = getSPI(spiGradeLevelList, gradeLevelName, lotName, itemName, quantity);
      double cbm = getCBM(cbmGradeLevelList, gradeLevelName, lotName, spi);
      
      schoolLotlList.add(new SchoolLot_Model(lotName, spi, cbm));
      
//      System.out.println(gradeLevelName + " - SPI: " + spi + " CBM: " + cbm);
      
     /* SchoolList_TableModel school = schoolListTable.get(schoolIndex);
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
