package org.nikkatrading.wlp_automationv7.Models.KG;

import java.util.List;

public class KGGradeLevel {
   private final String gradeLvlName;
   private final List<KGLot> lotList;
   
   public KGGradeLevel(String gradeLvlName, List<KGLot> lotList) {
      this.gradeLvlName = gradeLvlName;
      this.lotList = lotList;
   }
   
   public String getGradeLvlName() {
      return gradeLvlName;
   }
   
   public List<KGLot> getLotList() {
      return lotList;
   }
}
