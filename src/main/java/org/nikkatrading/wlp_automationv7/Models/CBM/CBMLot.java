package org.nikkatrading.wlp_automationv7.Models.CBM;

public class CBMLot {
   private String lot;
   private double cbm;
   
   public CBMLot(String lot, double cbm) {
      this.lot = lot;
      this.cbm = cbm;
   }
   
   public String getLot() {
      return lot;
   }
   
   public void setLot(String lot) {
      this.lot = lot;
   }
   
   public double getCbm() {
      return cbm;
   }
   
   public void setCbm(double cbm) {
      this.cbm = cbm;
   }
}
