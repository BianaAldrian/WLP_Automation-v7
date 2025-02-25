package org.nikkatrading.wlp_automationv7.DB;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GetConnection {
   private static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
   private static final String DB_HOST;
   private static final String USERNAME = "root";
   private static final String PASSWORD;
   private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
   private static Connection connection; // Persistent connection for monitoring
   
   static {
      // Localhost Configuration (for testing)
      /*DB_HOST = "jdbc:mysql://localhost:3306/";
      PASSWORD = "Res3cted";*/
      
      // Uncomment below for Main Server Configuration
        PASSWORD = "NikkaT2024!@";
        String defaultGateway = getDefaultGateway();
        if ("192.168.88.1".equals(defaultGateway)) {
            DB_HOST = "jdbc:mysql://192.168.88.3:3306/";
            System.out.println("Connected to the local network.");
        } else {
            DB_HOST = "jdbc:mysql://112.199.100.14:3306/";
            System.out.println("Connected to an outside network.");
        }
      
      // Start Connection Monitoring in Background
      startConnectionMonitoring();
   }
   
   // Generic method to get a database connection
   private static Connection getConnection(String databaseName) throws SQLException {
      String jdbcURL = DB_HOST + databaseName;
      
      try {
         // Load MySQL JDBC Driver
         Class.forName(JDBC_DRIVER);
         
         // Establish and return the connection
         connection = DriverManager.getConnection(jdbcURL, USERNAME, PASSWORD);
         System.out.println("Connected to the database: " + databaseName + " successfully!");
         return connection;
      } catch (ClassNotFoundException e) {
         System.out.println("MySQL JDBC Driver not found.");
         throw new SQLException("Driver not found", e);
      } catch (SQLException e) {
         System.out.println("Failed to connect to the database: " + databaseName);
         throw e;
      }
   }
   
   // Specific methods for each database
   public static Connection getEpaAllocationConnection() throws SQLException {
      return getConnection("epa_allocation");
   }
   
   public static Connection getHelpDeskConnection() throws SQLException {
      return getConnection("helpdesk_db");
   }
   
   public static Connection getDocdeskConnection() throws SQLException {
      return getConnection("docdesk_db");
   }
   
   // Method to get the default gateway IP
   private static String getDefaultGateway() {
      try {
         String os = System.getProperty("os.name").toLowerCase();
         Process process;
         if (os.contains("win")) {
            process = Runtime.getRuntime().exec("ipconfig");
         } else {
            process = Runtime.getRuntime().exec("netstat -rn");
         }
         
         BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
         String line;
         while ((line = reader.readLine()) != null) {
            if (line.contains("Gateway") || line.contains("0.0.0.0") || line.contains("default")) {
               String[] parts = line.split("\\s+");
               return parts[parts.length - 1];
            }
         }
      } catch (IOException e) {
         System.err.println(e);
      }
      return "";
   }
   
   // Background Connection Health Check
   public static void startConnectionMonitoring() {
      scheduler.scheduleAtFixedRate(() -> {
         boolean isConnected = isConnected();
         if (!isConnected) {
            System.out.println("Database Connection Status: Disconnected");
            try {
               // Attempt to reconnect
               connection = getEpaAllocationConnection();
               System.out.println("Reconnected to the database successfully.");
            } catch (Exception e) {
               System.err.println("Reconnection failed: " + e.getMessage());
            }
         }
      }, 0, 5, TimeUnit.SECONDS); // Check every 10 seconds
   }
   
   // Stop monitoring the database connection
   public static void shutdownMonitoring() {
      scheduler.shutdown();
      System.out.println("Database connection monitoring stopped.");
   }
   
   // Check if the connection is still alive
   private static boolean isConnected() {
      try {
         return connection != null && !connection.isClosed() && connection.isValid(2);
      } catch (SQLException e) {
         return false;
      }
   }
}

