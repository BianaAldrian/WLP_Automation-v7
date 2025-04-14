package org.nikkatrading.wlp_automationv7.Models.KG;

public class KGItem {
   private final String itemName;
   private final double kg;
   
   public KGItem(String itemName, double kg) {
      this.itemName = itemName;
      this.kg = kg;
   }
   
   public String getItemName() {
      return itemName;
   }
   
   public double getKg() {
      return kg;
   }
}
