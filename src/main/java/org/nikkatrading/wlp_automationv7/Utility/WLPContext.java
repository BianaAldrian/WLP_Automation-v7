package org.nikkatrading.wlp_automationv7.Utility;

import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.nikkatrading.wlp_automationv7.Models.KG.KGGradeLevel;
import org.nikkatrading.wlp_automationv7.Models.SPI.SPIGradeLevel;
import org.nikkatrading.wlp_automationv7.Models.Table.SchoolList_TableModel;

import java.util.List;
import java.util.Map;

public class WLPContext {
   public String version;
   public Stage mainStage;
   public CheckBox cbContainer;
   public TextField inp_batchNumber;
   public TextField inp_containerNum;
   public List<SPIGradeLevel> spiGradeLevelList;
   public List<KGGradeLevel> kgGradeLevelList;
   public List<SchoolList_TableModel> selectedSchool;
   public Map<Integer, List<SchoolList_TableModel>> selectedBatchesMap;
   
   // Optionally add constructor or builder methods
}
