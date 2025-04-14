package org.nikkatrading.wlp_automationv7.Models;

import java.util.List;

public class DeliveredSchoolModel {
   String regionName;
   String divisionName;
   int schoolId;
   String schoolName;
   List<LotsModel> lotsValueList;
   
   public DeliveredSchoolModel(String regionName, String divisionName, int schoolId, String schoolName, List<LotsModel> lotsValueList) {
      this.regionName = regionName;
      this.divisionName = divisionName;
      this.schoolId = schoolId;
      this.schoolName = schoolName;
      this.lotsValueList = lotsValueList;
   }
   
   public String getRegionName() {
      return regionName;
   }
   
   public void setRegionName(String regionName) {
      this.regionName = regionName;
   }
   
   public String getDivisionName() {
      return divisionName;
   }
   
   public void setDivisionName(String divisionName) {
      this.divisionName = divisionName;
   }
   
   public int getSchoolId() {
      return schoolId;
   }
   
   public void setSchoolId(int schoolId) {
      this.schoolId = schoolId;
   }
   
   public String getSchoolName() {
      return schoolName;
   }
   
   public void setSchoolName(String schoolName) {
      this.schoolName = schoolName;
   }
   
   public List<LotsModel> getLotsValueList() {
      return lotsValueList;
   }
   
   public void setLotsValueList(List<LotsModel> lotsValueList) {
      this.lotsValueList = lotsValueList;
   }
}
