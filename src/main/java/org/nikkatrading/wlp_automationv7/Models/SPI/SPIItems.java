package org.nikkatrading.wlp_automationv7.Models.SPI;

public class SPIItems {
   private String item;
   private int qty;
   
   public SPIItems(String item, int qty) {
      this.item = item;
      this.qty = qty;
   }
   
   public String getItem() {
      return item;
   }
   
   public void setItem(String item) {
      this.item = item;
   }
   
   public int getQty() {
      return qty;
   }
   
   public void setQty(int qty) {
      this.qty = qty;
   }
}
