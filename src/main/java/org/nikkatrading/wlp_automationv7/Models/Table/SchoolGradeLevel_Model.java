package org.nikkatrading.wlp_automationv7.Models.Table;

import java.util.List;

public class SchoolGradeLevel_Model {
   private String gradeLevel;
   private List<SchoolLot_Model> schoolLotModelList;
   
   public SchoolGradeLevel_Model(String gradeLevel, List<SchoolLot_Model> schoolLotModelList) {
      this.gradeLevel = gradeLevel;
      this.schoolLotModelList = schoolLotModelList;
   }
   
   public String getGradeLevel() {
      return gradeLevel;
   }
   
   public void setGradeLevel(String gradeLevel) {
      this.gradeLevel = gradeLevel;
   }
   
   public List<SchoolLot_Model> getTableSchoolLotList() {
      return schoolLotModelList;
   }
   
   public void setTableSchoolLotList(List<SchoolLot_Model> schoolLotModelList) {
      this.schoolLotModelList = schoolLotModelList;
   }
}
