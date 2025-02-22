package org.nikkatrading.wlp_automationv7.Models.SPI;

import java.util.List;

public class SPIGradeLevel {
   private String gradeLevel;
   private List<SPILot> spiLotList;
   
   public SPIGradeLevel(String gradeLevel, List<SPILot> spiLotList) {
      this.gradeLevel = gradeLevel;
      this.spiLotList = spiLotList;
   }
   
   public String getGradeLevel() {
      return gradeLevel;
   }
   
   public void setGradeLevel(String gradeLevel) {
      this.gradeLevel = gradeLevel;
   }
   
   public List<SPILot> getSpiLotList() {
      return spiLotList;
   }
   
   public void setSpiLotList(List<SPILot> spiLotList) {
      this.spiLotList = spiLotList;
   }
}
