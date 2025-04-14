package org.nikkatrading.wlp_automationv7.DB;

import org.nikkatrading.wlp_automationv7.Models.Table.SchoolGradeLevel_Model;
import org.nikkatrading.wlp_automationv7.Models.Table.SchoolList_TableModel;
import org.nikkatrading.wlp_automationv7.Models.Table.SchoolLot_Model;
import org.nikkatrading.wlp_automationv7.Utility.SessionData;

import java.sql.*;
import java.util.*;

public class PutHelpDesk {
   
   private final StringBuilder workloadValues = new StringBuilder();
   private boolean success = false;
   
   public PutHelpDesk() {
      int batch_id = insertBatch();
      if (batch_id != -1) {
         success = insertSchools(batch_id);
      }
   }
   
   public boolean isSuccess() {
      return success;
   }
   
   private int insertBatch() {
      String sqlBatch = "INSERT INTO batch_info (batch_no, school_count, created_by, lots, total_standalone, total_motherbox, total_cbm) "
              + "VALUES (?, ?, ?, ?, ?, ?, ?)";
      
      int generatedId = -1;
      
      try (Connection connection = GetConnection.getHelpDeskConnection();
           PreparedStatement pstmt = connection.prepareStatement(sqlBatch, Statement.RETURN_GENERATED_KEYS)) {
         
         String batch_no = SessionData.getInstance().getBatch_no();
         int school_count = SessionData.getInstance().getSchool_count();
         String created_by = System.getProperty("user.name");
         String lots = SessionData.getInstance().getLots();
         int total_standalone = SessionData.getInstance().getTotal_standalone();
         int total_motherbox = SessionData.getInstance().getTotal_motherbox();
         double total_cbm = SessionData.getInstance().getTotal_cbm();
         
         pstmt.setString(1, batch_no.replace("BATCH NO.", "").trim());
         pstmt.setInt(2, school_count);
         pstmt.setString(3, created_by);
         pstmt.setString(4, lots);
         pstmt.setInt(5, total_standalone);
         pstmt.setInt(6, total_motherbox);
         pstmt.setDouble(7, total_cbm);
         
         int affectedRows = pstmt.executeUpdate();
         
         if (affectedRows == 0) {
            throw new SQLException("Inserting batch failed, no rows affected.");
         }
         
         try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
            if (generatedKeys.next()) {
               generatedId = generatedKeys.getInt(1);
            } else {
               throw new SQLException("Inserting batch failed, no ID obtained.");
            }
         }
         
      } catch (SQLException e) {
         e.printStackTrace(); // Replace with proper logging
      }
      
      return generatedId;
   }
   
   
   private boolean insertSchools(int batch_id) {
      Connection connection = null;
      PreparedStatement pstmt = null;
      
      try {
         connection = GetConnection.getHelpDeskConnection();
         connection.setAutoCommit(false);
         
         List<String> lots = new ArrayList<>();
         
         String lotsString = SessionData.getInstance().getLots().trim();

         // Remove the square brackets if present
         if (lotsString.startsWith("[") && lotsString.endsWith("]")) {
            lotsString = lotsString.substring(1, lotsString.length() - 1).trim(); // Remove the brackets
         }

         // If the string is not empty (i.e., the list isn't truly empty), split by commas
         if (!lotsString.isEmpty()) {
            lots.addAll(Arrays.asList(lotsString.split(",\\s*")));
         }
         
         
         for (SchoolList_TableModel schoolModel : SessionData.getInstance().getSchoolList()) {
            Set<String> uniqueLots = new HashSet<>();
            
            for (SchoolGradeLevel_Model gradeLevel : schoolModel.getTableSchoolGradeLevelList()) {
               for (SchoolLot_Model lot : gradeLevel.getTableSchoolLotList()) {
                  String lotName = lot.getLotName();
                  if (lotName != null && !lotName.isEmpty()) {
                     String cleanedLot = lotName.split(":")[0].replace("LOT", "").trim();
                     uniqueLots.add(cleanedLot);
                  }
               }
            }
            
            if (!uniqueLots.isEmpty()) {
               validLots(lots, schoolModel.getSchoolID(), uniqueLots, batch_id);
            }
         }
         
         StringBuilder lotHeader = new StringBuilder();
         for (int i = 0; i < lots.size(); i++) {
            lotHeader.append("lot_").append(lots.get(i));
            if (i < lots.size() - 1) {
               lotHeader.append(", ");
            }
         }
         
         String sqlWorkload = "INSERT INTO workload_info (\n"
                 + "    school_id, " + lotHeader + ", batch_id\n"
                 + ") VALUES \n"
                 + workloadValues.toString() + ";";
         
         System.out.println("Executing SQL:\n" + sqlWorkload);
         
         pstmt = connection.prepareStatement(sqlWorkload);
         pstmt.executeUpdate();
         
         connection.commit();
         return true;
         
      } catch (Exception e) {
         e.printStackTrace();
         try {
            if (connection != null) connection.rollback();
         } catch (SQLException se) {
            se.printStackTrace();
         }
         return false;
      } finally {
         try {
            if (pstmt != null) pstmt.close();
            if (connection != null) connection.setAutoCommit(true);
            if (connection != null) connection.close();
         } catch (SQLException se) {
            se.printStackTrace();
         }
      }
   }
   
   private void validLots(List<String> selectedLot, int schoolID, Set<String> uniqueLots, int batch_id) {
      StringBuilder result = new StringBuilder();
      result.append("'").append(schoolID).append("'");
      
      for (String lot : selectedLot) {
         if (uniqueLots.contains(lot)) {
            result.append(", 'Y'");
         } else {
            result.append(", NULL");
         }
      }
      
      result.append(", ").append(batch_id); // Add batch_id as a numeric value, or quote if needed
      
      String value = "(" + result + ")";
      
      if (workloadValues.isEmpty()) {
         workloadValues.append("\n    ").append(value); // First row, indented
      } else {
         workloadValues.append(",\n    ").append(value); // Subsequent rows, comma-prefixed
      }
   }
}
