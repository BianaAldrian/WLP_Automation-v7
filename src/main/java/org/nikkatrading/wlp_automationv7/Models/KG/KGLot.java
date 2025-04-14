package org.nikkatrading.wlp_automationv7.Models.KG;

import java.util.List;

public class KGLot {
   private final String lotName;
   private final List<KGItem> itemList;
   
   public KGLot(String lotName, List<KGItem> itemList) {
      this.lotName = lotName;
      this.itemList = itemList;
   }
   
   public String getLotName() {
      return lotName;
   }
   
   public List<KGItem> getItemList() {
      return itemList;
   }
}
