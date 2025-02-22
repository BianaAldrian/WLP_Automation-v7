package org.nikkatrading.wlp_automationv7.Models.Table;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.util.ArrayList;
import java.util.List;

public class SchoolList_TableModel {
   private BooleanProperty select = new SimpleBooleanProperty();
   private String region;
   private String division;
   private int schoolID;
   private String schoolName;
   private List<String> gradeLevels;
   private double cbm;
   
   private List<SchoolGradeLevel_Model> schoolGradeLevelModelList;
   
   public SchoolList_TableModel(String region, String division, int schoolID, String schoolName, String gradeLevelName, double cbm, List<SchoolGradeLevel_Model> schoolGradeLevelModelList) {
      this.region = region;
      this.division = division;
      this.schoolID = schoolID;
      this.schoolName = schoolName;
      this.gradeLevels = new ArrayList<>();  // Initialize the list
      this.gradeLevels.add(gradeLevelName);  // Add the first grade level
      this.cbm = cbm;
      this.schoolGradeLevelModelList = schoolGradeLevelModelList;
   }
   
   public boolean isSelect() {
      return select.get();
   }
   
   public BooleanProperty selectProperty() {
      return select;
   }
   
   public void setSelect(boolean select) {
      this.select.set(select);
   }
   
   public String getRegion() {
      return region;
   }
   
   public void setRegion(String region) {
      this.region = region;
   }
   
   public String getDivision() {
      return division;
   }
   
   public void setDivision(String division) {
      this.division = division;
   }
   
   public int getSchoolID() {
      return schoolID;
   }
   
   public void setSchoolID(int schoolID) {
      this.schoolID = schoolID;
   }
   
   public String getSchoolName() {
      return schoolName;
   }
   
   public void setSchoolName(String schoolName) {
      this.schoolName = schoolName;
   }
   
   // Getter for grade levels
   public List<String> getGradeLevels() {
      return gradeLevels;
   }
   
   // Method to add a new grade level
   public void addGradeLevel(String gradeLevelName) {
      if (!gradeLevels.contains(gradeLevelName)) { // Prevent duplicates
         gradeLevels.add(gradeLevelName);
      }
   }
   
   public void addCbm(double additionalCbm) {
      this.cbm += additionalCbm;
   }
   
   public double getCbm() {
      return cbm;
   }
   
   public void setCbm(double cbm) {
      this.cbm = cbm;
   }
   
   public List<SchoolGradeLevel_Model> getTableSchoolGradeLevelList() {
      return schoolGradeLevelModelList;
   }
   
   public void setTableSchoolGradeLevelList(List<SchoolGradeLevel_Model> schoolGradeLevelModelList) {
      this.schoolGradeLevelModelList = schoolGradeLevelModelList;
   }
}
