package org.nikkatrading.wlp_automationv7.Models;

public class LotsModel {
   String lotCol;
   String value;
   
   public LotsModel(String lotCol, String value) {
      this.lotCol = lotCol;
      this.value = value;
   }
   
   public String getLotCol() {
      return lotCol;
   }
   
   public void setLotCol(String lotCol) {
      this.lotCol = lotCol;
   }
   
   public String getValue() {
      return value;
   }
   
   public void setValue(String value) {
      this.value = value;
   }
}
