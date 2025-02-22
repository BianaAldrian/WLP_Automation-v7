package org.nikkatrading.wlp_automationv7.Models.SPI;

import java.util.List;

public class SPILot {
   private String lot;
   private List<SPIItems> itemsList;
   
   public SPILot(String lot, List<SPIItems> itemsList) {
      this.lot = lot;
      this.itemsList = itemsList;
   }
   
   public String getLot() {
      return lot;
   }
   
   public void setLot(String lot) {
      this.lot = lot;
   }
   
   public List<SPIItems> getItemsList() {
      return itemsList;
   }
   
   public void setItemsList(List<SPIItems> itemsList) {
      this.itemsList = itemsList;
   }
}
