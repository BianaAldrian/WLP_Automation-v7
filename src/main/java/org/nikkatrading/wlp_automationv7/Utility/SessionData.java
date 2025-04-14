package org.nikkatrading.wlp_automationv7.Utility;

import org.nikkatrading.wlp_automationv7.Models.Table.SchoolList_TableModel;

import java.util.List;

public class SessionData {
   private static final SessionData instance = new SessionData();
   
   private List<String> selectedLot;
   private String batch_no;
   private int school_count;
   private String lots;
   private int total_standalone;
   private int total_motherbox;
   private double total_cbm;
   private String created_by;
   private List<SchoolList_TableModel> schoolList;
   
   private SessionData() {}
   
   public static SessionData getInstance() {
      return instance;
   }
   
   public List<String> getSelectedLot() {
      return selectedLot;
   }
   
   public void setSelectedLot(List<String> selectedLot) {
      this.selectedLot = selectedLot;
   }
   
   public String getBatch_no() {
      return batch_no;
   }
   
   public void setBatch_no(String batch_no) {
      this.batch_no = batch_no;
   }
   
   public int getSchool_count() {
      return school_count;
   }
   
   public void setSchool_count(int school_count) {
      this.school_count = school_count;
   }
   
   public String getLots() {
      return lots;
   }
   
   public void setLots(String lots) {
      this.lots = lots;
   }
   
   public int getTotal_standalone() {
      return total_standalone;
   }
   
   public void setTotal_standalone(int total_standalone) {
      this.total_standalone = total_standalone;
   }
   
   public int getTotal_motherbox() {
      return total_motherbox;
   }
   
   public void setTotal_motherbox(int total_motherbox) {
      this.total_motherbox = total_motherbox;
   }
   
   public double getTotal_cbm() {
      return total_cbm;
   }
   
   public void setTotal_cbm(double total_cbm) {
      this.total_cbm = total_cbm;
   }
   
   public String getCreated_by() {
      return created_by;
   }
   
   public void setCreated_by(String created_by) {
      this.created_by = created_by;
   }
   
   public List<SchoolList_TableModel> getSchoolList() {
      return schoolList;
   }
   
   public void setSchoolList(List<SchoolList_TableModel> schoolList) {
      this.schoolList = schoolList;
   }
   
   public void clear() {
      batch_no = null;
      school_count = 0;
      lots = null;
      total_standalone = 0;
      total_motherbox = 0;
      total_cbm = 0;
      created_by = null;
      schoolList = null;
   }
}
