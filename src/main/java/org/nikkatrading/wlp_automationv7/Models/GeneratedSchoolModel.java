package org.nikkatrading.wlp_automationv7.Models;

import java.sql.Timestamp;
import java.util.List;

public class GeneratedSchoolModel {
   int schoolId;
   List<LotsModel> lotsValueList;
   
   public GeneratedSchoolModel(int schoolId, List<LotsModel> lotsValueList) {
      this.schoolId = schoolId;
      this.lotsValueList = lotsValueList;
   }
   
   public int getSchoolId() {
      return schoolId;
   }
   
   public void setSchoolId(int schoolId) {
      this.schoolId = schoolId;
   }
   
   public List<LotsModel> getLotsValueList() {
      return lotsValueList;
   }
   
   public void setLotsValueList(List<LotsModel> lotsValueList) {
      this.lotsValueList = lotsValueList;
   }
}
