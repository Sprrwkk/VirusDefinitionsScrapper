package SophosPackage;
import java.sql.Connection;

import com.healthmarketscience.jackcess.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.*;
import java.sql.*;
import java.util.Date;


public class SophosMain  {

    static int ruleID;
    static int auditID;
    static int packageID;

    static Connection connect = null;
    static Statement statement = null;
    static ResultSet resultSet = null;

    // STATICS
    static Date actuallDate = new Date();


    static Elements fileName;
    static Elements fileSize;
    static Elements datePublished;


    static String select = "SELECT Version FROM T_PACKAGES WHERE SWProduct_ID = 36325";

    static String sRuleIDSelect = "select MIN(AUTONUMBER+1) from T_PAR_LUT t1 where AUTONUMBER > 5500000 and AUTONUMBER < 5999999 "
            + "and not exists (select 1 from T_PAR_LUT t2 where t2.AUTONUMBER=t1.AUTONUMBER+1)";
    static String sAuditIDSelect = "select MIN(AUDITID+1) from T_PA_LUT t1 where AUDITID > 500000 and AUDITID < 999999 "
            + "and not exists (select 1 from T_PA_LUT t2 where t2.AUDITID=t1.AUDITID+1)";
    // IMPORTANT: we cannot re-use PackageIDs, so do not use select min or select top
    static String sPackageIDSelect = "select MAX(PACKAGE_ID+1) from T_PACKAGES t1 where PACKAGE_ID > 500000 and PACKAGE_ID < 1000000 and not exists (select 1 from T_PACKAGES t2 where t2.PACKAGE_ID=t1.PACKAGE_ID+1)";



    static String insertT_PAR_LUT = "INSERT INTO T_PAR_LUT"
            + "(AUTONUMBER, FILENAME, SIZE, CRC, TRANSLATIONKEY, VERSIONSTRING, LICENSEDEFINITIONFILE, AUDITID, AUTHOR, FILEPATH, FILEDATE) VALUES"
            + "(?,?,?,?,?,?,?,?,?,?,?)";
    static String insertT_UPAR_LUT = "INSERT INTO T_UPAR_LUT"
            + "(AUTONUMBER, AUDITID, FILENAME, VERSIONSTRING, CHECKSUM) VALUES"
            + "(?,?,?,?,?)";
    static String insertT_PA_LUT = "INSERT INTO T_PA_LUT"
            + "(AUDITID, PACKAGEID, AUTHOR, SOURCE) VALUES"
            + "(?,?,?,?)";
    static String insertT_Packages = "INSERT INTO T_PACKAGES"
            + "(PACKAGE_ID, SWPRODUCT_ID, VERSION, MAINVER, SUBVER1, SUBVER2, SUBVER3, SP_SR, SP_SRONLY, OS, LANGUAGE, VARIATION, EDITION, GENERICVER, CREATIONDATE, AUTHOR) VALUES"
            + "(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
    static String insertT_Packages_Log = "INSERT INTO T_PACKAGES_LOG"
            + "(PACKAGE_ID, SWPRODUCT_ID, VERSION, MAINVER, SUBVER1, SUBVER2, SUBVER3, SP_SR, SP_SRONLY, OS, LANGUAGE, VARIATION, EDITION, GENERICVER, USERNAME) VALUES"
            + "(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
    static String insertT_NFSoftwareAudits_LUT = "INSERT INTO T_NFSoftwareAudits_LUT"
            + "(AUTONUMBER, AUDITID, RULETYPE, PACKAGENAME, RULE1, RULE2) VALUES"
            + "(?,?,?,?,?,?)";


    public static void runningScript() throws IOException, SQLException, ClassNotFoundException {
        //writeToFileSophos();
        //writeToFileSophos();
        connectToDB();

    }




    public static void writeToFileSophos() throws IOException {
        Document doc;

        // Create SB object to store file element's
        StringBuilder fileInfo = new StringBuilder();
        String filePath = "Files\\";

        // Connect with Sophos latest IDE xml
        doc = Jsoup.connect("https://downloads.sophos.com/downloads/info/latest_IDE.xml").get();

        // get filename
        fileName = doc.select("name");
        //get filesize
        fileSize = doc.select("size");
        // get published date
        datePublished = doc.select("published");

        // append file elements to fileInfo
        for (Element file : fileName
        ) {
            fileInfo.append(file.text() + "\n");
            filePath += file.text() + ".txt";
        }

        for (Element size : fileSize) {
            fileInfo.append(size.text() + "\n");
        }

        for (Element date : datePublished) {
            // append only 9 chars of date
            fileInfo.append(date.text().substring(0, 10).replace("-", ""));
        }

        // write to file
        FileWriter fw = new FileWriter(filePath, false);
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(fileInfo.toString());
        bw.close();
        System.out.println("Write:" + "\n" + fileInfo.toString());
    }


    public static void connectToDB() throws IOException {
        try {
            Class.forName("net.ucanaccess.jdbc.UcanaccessDriver");
        } catch (ClassNotFoundException cnfex) {
            System.out.println("Problem in loading or registering MS Access JDBC driver");
            cnfex.printStackTrace();
        }

        try {
            String msAccDB = "C:\\Users\\lwrobel\\Desktop\\AMTables.mdb;memory=false";
            String dbURL = "jdbc:ucanaccess://" + msAccDB;
            connect = DriverManager.getConnection(dbURL);
            statement = connect.createStatement();
            resultSet = statement.executeQuery("select MAX(PACKAGE_ID+1) from T_PACKAGES t1 where PACKAGE_ID > 500000 and PACKAGE_ID < 1000000 and not exists (select 1 from T_PACKAGES t2 where t2.PACKAGE_ID=t1.PACKAGE_ID+1)");
            System.out.println("========================Result=====================");
            while (resultSet.next()) {
                System.out.println(resultSet.getInt(1));
            }
        } catch (SQLException sqlex) {
            sqlex.printStackTrace();
        }
        finally {
            try {
                if (null != connect) {
                    resultSet.close();
                    statement.close();
                    connect.close();
                }
            } catch (SQLException sqlex) {
                sqlex.printStackTrace();
            }
        }




    }


//    static int selectFirstFree(String select) throws SQLException {
//        int nFreeID = -1;
//        if (connect == null || connect.isClosed()) {
//            connect = DriverManager.getConnection("jdbc:ucanaccess://C:\\Users\\lwrobel\\Desktop\\AMTables.mdb;memory=false");
//        }
//        if (connect != null && !connect.isClosed()) {
//            statement = connect.createStatement();
//            ResultSet rs = statement.executeQuery(select);
//            if (rs.next()) {
//                nFreeID = rs.getInt(1);
//            }
//        } else {
//            System.out.println("selectFirstFree: Could not connect to the datbase!");
//        }
//        return nFreeID;
//    }

//    public static void getsRuleIDSelectStatement(String statementQuery) {
//
//        try {
//            String msAccessDBName = "C:\\Users\\lwrobel\\Desktop\\AMTables.mdb;memory=false";
//            String dbURL = "jdbc:ucanaccess://" + msAccessDBName;
//            connect = DriverManager.getConnection(dbURL);
//            statement = connect.createStatement();
//            resultSet = statement.executeQuery(statementQuery);
//
//            while (resultSet.next()) {
//                ruleID = resultSet.getInt(1);
//            }
//            System.out.println(ruleID);
//
//        } catch (SQLException sqlex) {
//            sqlex.printStackTrace();
//        } finally {
//
//            try {
//                if (null != connect) {
//                    resultSet.close();
//                    statement.close();
//                    connect.close();
//
//                }
//            } catch (SQLException sqlex) {
//                sqlex.printStackTrace();
//            }
//        }
//
//    }
//
//    public static void getsPackageIDSelectStatement(String statementQuery) {
//        try {
//            String msAccessDBName = "C:\\Users\\lwrobel\\Desktop\\AMTables.mdb;memory=false";
//            String dbURL = "jdbc:ucanaccess://" + msAccessDBName;
//            connect = DriverManager.getConnection(dbURL);
//            statement = connect.createStatement();
//            resultSet = statement.executeQuery(statementQuery);
//
//            while (resultSet.next()) {
//                packageID = resultSet.getInt(1);
//            }
//
//            System.out.println(packageID);
//
//        } catch (SQLException sqlex) {
//            sqlex.printStackTrace();
//        } finally {
//
//            try {
//                if (null != connect) {
//                    resultSet.close();
//                    statement.close();
//                    connect.close();
//
//                }
//            } catch (SQLException sqlex) {
//                sqlex.printStackTrace();
//            }
//        }
//    }
//
//
//    public static void getConnect() throws ClassNotFoundException, IOException, SQLException {
//
//        try {
//            Class.forName("net.ucanaccess.jdbc.UcanaccessDriver");
//        } catch (ClassNotFoundException cnfex) {
//            System.out.println("Problem in loading"
//                    + " MS Access JDBC driver");
//            cnfex.printStackTrace();
//        }
//
//            getsRuleIDSelectStatement(sRuleIDSelect);
//            getsPackageIDSelectStatement(sPackageIDSelect);
//        }


//        ruleID = selectFirstFree(sRuleIDSelect);
//        auditID = selectFirstFree(sAuditIDSelect);
//        packageID = selectFirstFree(sPackageIDSelect);

//        if (ruleID <= 0 || auditID <= 0 || packageID <= 0) {
//            System.out.println("Failed to retrieve free ID values from database! Cannot proceed with adding definitions!!!");
//            System.out.println("ruleID = " + ruleID + ", auditID = " + auditID + ", packageID = " + packageID);
//            System.exit(1);
//        }



//        System.out.println("===================");
//        System.out.println("Results ");

//        System.out.println(ruleID + " " + auditID + " " + packageID);

//        // inserting to T_PAR_LUT
//        PreparedStatement statementT_PAR_LUT = connect.prepareStatement(insertT_PAR_LUT);
//        statementT_PAR_LUT.setInt(1, ruleID);
//        statementT_PAR_LUT.setString(2, fileName.toString());
//        statementT_PAR_LUT.setInt(3, Integer.parseInt(fileSize.toString()));
//        statementT_PAR_LUT.setInt(4, -1);
//        statementT_PAR_LUT.setString(5, "-1");
//        statementT_PAR_LUT.setString(6, datePublished.toString());
//        statementT_PAR_LUT.setInt(7, freeAudit);
//        statementT_PAR_LUT.setString(8, "lwrobel");
//        statementT_PAR_LUT.setString(9, null);
//        statementT_PAR_LUT.setDate(10, (java.sql.Date) actuallDate);
//
//        // inserting to T_PA_LUT
//        PreparedStatement statementT_PA_LUT = connect.prepareStatement(insertT_PA_LUT);
//        statementT_PA_LUT.setInt(1, auditID);
//        statementT_PA_LUT.setInt(2, packageID);
//        statementT_PA_LUT.setString(3, "lwrobel");
//        statementT_PA_LUT.setString(4, null);
//
//        // inserting to T_PACKAGES
//        PreparedStatement statementT_PACKAGES = connect.prepareStatement(insertT_Packages);
//        statementT_PACKAGES.setInt(1, packageID);
//        statementT_PACKAGES.setInt(2,36325);
//        statementT_PACKAGES.setString(3, datePublished.toString());
//        statementT_PACKAGES.setString(4, datePublished.toString());
//        statementT_PACKAGES.setString(5, null);
//        statementT_PACKAGES.setString(6, null);
//        statementT_PACKAGES.setString(7,null);
//        statementT_PACKAGES.setString(8,null);
//        statementT_PACKAGES.setString(9, null);
//        statementT_PACKAGES.setString(10, "Win");
//        statementT_PACKAGES.setString(11, null);
//        statementT_PACKAGES.setString(12, null);
//        statementT_PACKAGES.setString(13, null);
//        statementT_PACKAGES.setString(14, datePublished.toString());
//        statementT_PACKAGES.setString(15, String.valueOf(actuallDate));
//        statementT_PACKAGES.setString(16, "lwrobel0");
//
//        // T_PACKAGES_LOG
//        PreparedStatement statementT_PACKAGES_LOG = connect.prepareStatement(insertT_Packages_Log);
//        statementT_PACKAGES_LOG.setInt(1, packageID);
//        statementT_PACKAGES_LOG.setInt(2,  36325);
//        statementT_PACKAGES_LOG.setString(3, datePublished.toString());
//        statementT_PACKAGES_LOG.setString(4, datePublished.toString());
//        statementT_PACKAGES_LOG.setString(5, null);
//        statementT_PACKAGES_LOG.setString(6, null);
//        statementT_PACKAGES_LOG.setString(7, "");
//        statementT_PACKAGES_LOG.setString(8, "");
//        statementT_PACKAGES_LOG.setString(9, "");
//        statementT_PACKAGES_LOG.setString(10, "Win");
//        statementT_PACKAGES_LOG.setString(11, " ");
//        statementT_PACKAGES_LOG.setString(12, datePublished.toString());
//        statementT_PACKAGES_LOG.setString(13, "");
//        statementT_PACKAGES_LOG.setString(14, datePublished.toString());
//        statementT_PACKAGES_LOG.setString(15, "lwrobel");
//
//
//        statementT_PAR_LUT.executeUpdate();
//        statementT_PA_LUT.executeQuery();
//        statementT_PACKAGES.executeQuery();
//
//
//        statementT_PAR_LUT.close();
//        statementT_PA_LUT.close();
//        statementT_PACKAGES.close();

    }






