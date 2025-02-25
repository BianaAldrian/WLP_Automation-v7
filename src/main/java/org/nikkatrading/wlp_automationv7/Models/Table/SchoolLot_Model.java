package org.nikkatrading.wlp_automationv7.Models.Table;

public class SchoolLot_Model {
   private String lotName;
   private int setPerItem;
   private double cbm;
   
   public SchoolLot_Model(String lotName, int setPerItem, double cbm) {
      this.lotName = lotName;
      this.setPerItem = setPerItem;
      this.cbm = cbm;
   }
   
   public String getLotName() {
      return lotName;
   }
   
   public void setLotName(String lotName) {
      this.lotName = lotName;
   }
   
   public int getSetPerItem() {
      return setPerItem;
   }
   
   public void setSetPerItem(int setPerItem) {
      this.setPerItem = setPerItem;
   }
   
   public double getCbm() {
      return cbm;
   }
   
   public void setCbm(double cbm) {
      this.cbm = cbm;
   }
}
