package org.nikkatrading.wlp_automationv7.GenerateWLP;

import org.apache.poi.ss.usermodel.Footer;
import org.apache.poi.ss.usermodel.Header;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Sheet;

public class CopyPageLayout {
   
   public static void copyPageLayout(Sheet sourceSheet, Sheet targetSheet) {
      PrintSetup sourcePrintSetup = sourceSheet.getPrintSetup();
      PrintSetup targetPrintSetup = targetSheet.getPrintSetup();
      
      // Copying print setup settings
      targetPrintSetup.setPaperSize(sourcePrintSetup.getPaperSize());
      targetPrintSetup.setLandscape(sourcePrintSetup.getLandscape());
      targetPrintSetup.setScale(sourcePrintSetup.getScale());
      targetPrintSetup.setFitWidth(sourcePrintSetup.getFitWidth());
      targetPrintSetup.setFitHeight(sourcePrintSetup.getFitHeight());
      targetPrintSetup.setFooterMargin(sourcePrintSetup.getFooterMargin());
      targetPrintSetup.setHeaderMargin(sourcePrintSetup.getHeaderMargin());
      
      // Copying margins
      targetSheet.setMargin(Sheet.LeftMargin, sourceSheet.getMargin(Sheet.LeftMargin));
      targetSheet.setMargin(Sheet.RightMargin, sourceSheet.getMargin(Sheet.RightMargin));
      targetSheet.setMargin(Sheet.TopMargin, sourceSheet.getMargin(Sheet.TopMargin));
      targetSheet.setMargin(Sheet.BottomMargin, sourceSheet.getMargin(Sheet.BottomMargin));
      
      // Copying header and footer
      copyHeaderFooter(sourceSheet, targetSheet);
   }
   
   private static void copyHeaderFooter(Sheet sourceSheet, Sheet targetSheet) {
      Header sourceHeader = sourceSheet.getHeader();
      Footer sourceFooter = sourceSheet.getFooter();
      
      Header targetHeader = targetSheet.getHeader();
      Footer targetFooter = targetSheet.getFooter();
      
      // Copy headers
      targetHeader.setLeft(sourceHeader.getLeft());
      targetHeader.setCenter(sourceHeader.getCenter());
      targetHeader.setRight(sourceHeader.getRight());
      
      // Copy footers
      targetFooter.setLeft(sourceFooter.getLeft());
      targetFooter.setCenter(sourceFooter.getCenter());
      targetFooter.setRight(sourceFooter.getRight());
   }
}
