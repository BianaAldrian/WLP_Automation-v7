module org.nikkatrading.wlp_automationv7 {
   requires javafx.controls;
   requires javafx.fxml;
   requires org.controlsfx.controls;
   requires com.dlsc.formsfx;
   requires java.sql;
   requires mysql.connector.java;
   requires org.apache.poi.ooxml;
   opens org.nikkatrading.wlp_automationv7 to javafx.fxml;
   opens org.nikkatrading.wlp_automationv7.UI to javafx.fxml;
   exports org.nikkatrading.wlp_automationv7;
   exports org.nikkatrading.wlp_automationv7.UI;
}
