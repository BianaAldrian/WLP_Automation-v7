package org.nikkatrading.wlp_automationv7.Models.CBM;

import java.util.List;

public class CBMGradeLevel {
   private String gradeLevel;
   private List<CBMLot> cbmLotList;
   
   public CBMGradeLevel(String gradeLevel, List<CBMLot> cbmLotList) {
      this.gradeLevel = gradeLevel;
      this.cbmLotList = cbmLotList;
   }
   
   public String getGradeLevel() {
      return gradeLevel;
   }
   
   public void setGradeLevel(String gradeLevel) {
      this.gradeLevel = gradeLevel;
   }
   
   public List<CBMLot> getCbmLotList() {
      return cbmLotList;
   }
   
   public void setCbmLotList(List<CBMLot> cbmLotList) {
      this.cbmLotList = cbmLotList;
   }
}
