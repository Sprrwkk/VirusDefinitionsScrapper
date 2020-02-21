package SophosPackage;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by lwrobel on 20.02.20.
 */



public class SophosMain {


    // date settings
    static Date date;
    static SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a");

    // database settings
    static String database = "jdbc:odbc:Driver={Microsoft Access Driver (*.mdb)};DBQ=E:\\access\\##" + " Windows" + " Updates\\AMTablesWINDOWS UPDATES.mdb";
    static Connection connect = null;
    static java.sql.Statement statement = null;

    // element var's
    static Elements fileNameWeb;
    static Elements fileSizeWeb;
    static Elements datePublishedWeb;

    // file info var's
    static String fileName;
    static String fileSize;
    static String datePublished;




    // create var's for insert's
    static PreparedStatement psInsertT_PA_LUT = null;
    static PreparedStatement psInsertT_PAR_LUT = null;
    static PreparedStatement psInsertT_UPAR_LUT = null;
    static PreparedStatement psInsertT_Packages = null;
    static PreparedStatement psInsertT_Packages_Log = null;

    // select queries (get correct id's)
    static String sRuleIDSelect = "select MIN(AUTONUMBER+1) from T_PAR_LUT t1 where AUTONUMBER > 5500000 and AUTONUMBER < 5999999 and not exists (select 1 from T_PAR_LUT t2 where t2.AUTONUMBER=t1.AUTONUMBER+1)";
    static String sAuditIDSelect = "select MIN(AUDITID+1) from T_PA_LUT t1 where AUDITID > 500000 and AUDITID < 999999 and not exists (select 1 from T_PA_LUT t2 where t2.AUDITID=t1.AUDITID+1)";
    // IMPORTANT: we cannot re-use PackageIDs, so do not use select min or select top
    static String sPackageIDSelect = "select MAX(PACKAGE_ID+1) from T_PACKAGES t1 where PACKAGE_ID > 500000 and PACKAGE_ID < 1000000 and not exists (select 1 from T_PACKAGES t2 where t2.PACKAGE_ID=t1.PACKAGE_ID+1)";


    // insert string's
    static String insertT_PAR_LUT = "INSERT INTO T_PAR_LUT"
            + "(AUTONUMBER, FILENAME, SIZE, CRC, TRANSLATIONKEY, VERSIONSTRING, LICENSEDEFINITIONFILE, AUDITID, AUTHOR, FILEPATH, FILEDATE) VALUES"
            + "(?,?,?,?,?,?,?,?,?,?,?)";
    static String insertT_UPAR_LUT = "INSERT INTO T_UPAR_LUT (AUTONUMBER, AUDITID, FILENAME, VERSIONSTRING, CHECKSUM) VALUES"
            + "(?,?,?,?,?)";
    static String insertT_PA_LUT = "INSERT INTO T_PA_LUT (AUDITID, PACKAGEID, AUTHOR, SOURCE) VALUES"
            + "(?,?,?,?)";
    static String insertT_Packages = "INSERT INTO T_PACKAGES (PACKAGE_ID, SWPRODUCT_ID, VERSION, MAINVER, SUBVER1, SUBVER2, SUBVER3, SP_SR, SP_SRONLY, OS, LANGUAGE, VARIATION, EDITION, GENERICVER, CREATIONDATE, AUTHOR) VALUES"
            + "(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
    static String insertT_Packages_Log = "INSERT INTO T_PACKAGES_LOG (PACKAGE_ID, SWPRODUCT_ID, VERSION, MAINVER, SUBVER1, SUBVER2, SUBVER3, SP_SR, SP_SRONLY, OS, LANGUAGE, VARIATION, EDITION, GENERICVER, USERNAME) VALUES"
            + "(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";


    public static void main(String[] args) throws Exception {


        writeToFileSophos();

    }

    /**
     * Function that get filename, filesize and fileversion (date published) from sophos IDE (Read and write to file)
     *
     * @throws Exception
     */

    public static void writeToFileSophos() throws Exception {

        Document doc;

        // Create SB object to store file element's
        StringBuilder fileInfo = new StringBuilder();
        String filePath = "Files\\";

        // Connect with Sophos latest IDE xml
        doc = Jsoup.connect("https://downloads.sophos.com/downloads/info/latest_IDE.xml").get();

        // get filename
        fileNameWeb = doc.select("name");
        //get filesize
        fileSizeWeb = doc.select("size");
        // get published date
        datePublishedWeb = doc.select("published");

        // append file elements to fileInfo
        for (Element file : fileNameWeb) {
            filePath += file.text() + ".txt";
            fileName = file.text();
        }

        for (Element size : fileSizeWeb) {
            fileSize = size.text();
        }

        for (Element date : datePublishedWeb) {
            // append only 9 chars of date
            fileInfo.append(date.text().substring(0, 10).replace("-", ""));
            datePublished = date.text().substring(0, 10).replace("-", "");
        }

        // write to file
        FileWriter fw = new FileWriter(filePath, false);
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(fileInfo.toString());
        bw.close();
        System.out.println("Write:" + "\n" + fileInfo.toString());

        addNewDefinition();
        // addMacOSVersion(); -- do not use, need correct id queries

    }


    /**
     * Get free ID from select's queries
     * @param select
     * @return
     * @throws SQLException
     */

    public static int selectFirstFree(String select) throws SQLException {

        int nFreeID = -1;
        if (connect == null || connect.isClosed()) {
            connect = DriverManager.getConnection(database, "", "");
        }
        if (connect != null && !connect.isClosed()) {
            statement = connect.createStatement();
            ResultSet rs = statement.executeQuery(select);
            if (rs.next()) {
                nFreeID = rs.getInt(1);
            }
        } else {
            System.out.println("selectFirstFree: Could not connect to the datbase!");
        }
        return nFreeID;

    }


    /**
     * Adding Windos versions of IDE
     * @throws Exception
     */


    public static void addNewDefinition() throws Exception {

        String defFileName;
        int defSize;
        String fileVersion;

        int ruleID;
        int auditID;
        int packageID;


        try {
            defFileName = fileName;
            defSize = Integer.parseInt(fileSize);
            fileVersion = datePublished;
            date = new Date();
            connect = DriverManager.getConnection(database, "", "");
            connect.setAutoCommit(false);


            psInsertT_PAR_LUT = connect.prepareStatement(insertT_PAR_LUT);
            psInsertT_PA_LUT = connect.prepareStatement(insertT_PA_LUT);
            psInsertT_Packages = connect.prepareStatement(insertT_Packages);
            psInsertT_Packages_Log = connect.prepareStatement(insertT_Packages_Log);

            ruleID = selectFirstFree(sRuleIDSelect);
            auditID = selectFirstFree(sAuditIDSelect);
            packageID = selectFirstFree(sPackageIDSelect);

            if (ruleID <= 0 || auditID <= 0 || packageID <= 0) {
                System.out.println("Failed to retrieve free ID values from database! Cannot proceed with adding definitions!!!");
                System.out.println("ruleID = " + ruleID + ", auditID = " + auditID + ", packageID = " + packageID);
                System.exit(1);
            }
            //System.out.println("ruleID = " + ruleID + ", auditID = " + auditID + ", packageID = " + packageID);


            //T_PAR_LUT
            psInsertT_PAR_LUT.setInt(1, (Integer) ruleID);
            psInsertT_PAR_LUT.setString(2, defFileName);
            psInsertT_PAR_LUT.setInt(3, defSize);
            psInsertT_PAR_LUT.setInt(4, -1);
            psInsertT_PAR_LUT.setString(5, "-1");
            psInsertT_PAR_LUT.setString(6, " ");
            psInsertT_PAR_LUT.setInt(7, -1);
            psInsertT_PAR_LUT.setInt(8, (Integer) auditID);
            psInsertT_PAR_LUT.setString(9, "lwrobel");
            psInsertT_PAR_LUT.setString(10, null);
            psInsertT_PAR_LUT.setString(11, null);

            //T_PA_LUT
            psInsertT_PA_LUT.setInt(1, (Integer) auditID);
            psInsertT_PA_LUT.setInt(2, (Integer) packageID);
            psInsertT_PA_LUT.setString(3, "lwrobel");
            psInsertT_PA_LUT.setString(4, null);

            //T_PACKAGES
            psInsertT_Packages.setInt(1, (Integer) packageID);
            psInsertT_Packages.setInt(2, (Integer) 36325);
            psInsertT_Packages.setString(3, fileVersion);
            psInsertT_Packages.setString(4, fileVersion);
            psInsertT_Packages.setString(5, "");
            psInsertT_Packages.setString(6, "");
            psInsertT_Packages.setString(7, "");
            psInsertT_Packages.setString(8, "");
            psInsertT_Packages.setString(9, "");
            psInsertT_Packages.setString(10, "Win");
            psInsertT_Packages.setString(11, "");
            psInsertT_Packages.setString(12, "");
            psInsertT_Packages.setString(13, "");
            psInsertT_Packages.setString(14, fileVersion);
            psInsertT_Packages.setString(15, df.format(date));
            psInsertT_Packages.setString(16, "lwrobel");

            //T_PACKAGES_LOG
            psInsertT_Packages_Log.setInt(1, (Integer) packageID);
            psInsertT_Packages_Log.setInt(2, (Integer) 36325);
            psInsertT_Packages_Log.setString(3, fileVersion);
            psInsertT_Packages_Log.setString(4, fileVersion);
            psInsertT_Packages_Log.setString(5, "");
            psInsertT_Packages_Log.setString(6, "");
            psInsertT_Packages_Log.setString(7, "");
            psInsertT_Packages_Log.setString(8, "");
            psInsertT_Packages_Log.setString(9, "");
            psInsertT_Packages_Log.setString(10, "Win");
            psInsertT_Packages_Log.setString(11, " ");
            psInsertT_Packages_Log.setString(12, "");
            psInsertT_Packages_Log.setString(13, "");
            psInsertT_Packages_Log.setString(14, fileVersion);
            psInsertT_Packages_Log.setString(15, "lwrobel");


            psInsertT_Packages.execute();
            psInsertT_PA_LUT.execute();
            psInsertT_PAR_LUT.execute();
            psInsertT_Packages_Log.execute();

            connect.commit();

        } catch (SQLException e) {
            System.out.println("Exception has been thrown while adding new versions: '" + e.getMessage() + "'");
            System.out.println("SQL State: " + e.getSQLState() + ", Error Code: " + e.getErrorCode());
            if (connect != null) {
                connect.rollback();
                System.out.println("Connection rollback...");
            }
            e.printStackTrace();
        } finally {
            if (connect != null && !connect.isClosed()) {
                connect.close();
            }
        }
    }


    /**
     * Adding MacOS versions of IDE
     * @throws Exception
     */
    public static void addMacOSVersion() throws Exception {

        String defFileName;
        int defSize;
        String fileVersion;

        int ruleID;
        int auditID;
        int packageID;


        try {
            defFileName = fileName;
            defSize = Integer.parseInt(fileSize);
            fileVersion = datePublished;
            date = new Date();
            connect = DriverManager.getConnection(database, "", "");
            connect.setAutoCommit(false);

            psInsertT_UPAR_LUT = connect.prepareStatement(insertT_UPAR_LUT);
            psInsertT_PA_LUT = connect.prepareStatement(insertT_PA_LUT);
            psInsertT_Packages = connect.prepareStatement(insertT_Packages);
            psInsertT_Packages_Log = connect.prepareStatement(insertT_Packages_Log);


            ruleID = selectFirstFree(sRuleIDSelect);
            auditID = selectFirstFree(sAuditIDSelect);
            packageID = selectFirstFree(sPackageIDSelect);

            if (ruleID <= 0 || auditID <= 0 || packageID <= 0) {
                System.out.println("Failed to retrieve free ID values from database! Cannot proceed with adding definitions!!!");
                System.out.println("ruleID = " + ruleID + ", auditID = " + auditID + ", packageID = " + packageID);
                System.exit(1);
            }
            //System.out.println("ruleID = " + ruleID + ", auditID = " + auditID + ", packageID = " + packageID);


            //
            //T_UPAR_LUT
            psInsertT_UPAR_LUT.setInt(1, (Integer) ruleID);
            psInsertT_UPAR_LUT.setInt(2, (Integer) auditID);
            psInsertT_UPAR_LUT.setString(3, defFileName);
            psInsertT_UPAR_LUT.setString(4, fileVersion);
            psInsertT_UPAR_LUT.setInt(5, (Integer) 0);


            //T_PA_LUT
            psInsertT_PA_LUT.setInt(1, (Integer) auditID);
            psInsertT_PA_LUT.setInt(2, (Integer) packageID);
            psInsertT_PA_LUT.setString(3, "lwrobel");
            psInsertT_PA_LUT.setString(4, null);

            //T_PACKAGES
            psInsertT_Packages.setInt(1, (Integer) packageID);
            psInsertT_Packages.setInt(2, (Integer) 62222);
            psInsertT_Packages.setString(3, fileVersion + " Mac OS");
            psInsertT_Packages.setString(4, fileVersion);
            psInsertT_Packages.setString(5, "");
            psInsertT_Packages.setString(6, "");
            psInsertT_Packages.setString(7, "");
            psInsertT_Packages.setString(8, "");
            psInsertT_Packages.setString(9, "");
            psInsertT_Packages.setString(10, "Mac OS");
            psInsertT_Packages.setString(11, "");
            psInsertT_Packages.setString(12, "");
            psInsertT_Packages.setString(13, "");
            psInsertT_Packages.setString(14, fileVersion);
            psInsertT_Packages.setString(15, df.format(date));
            psInsertT_Packages.setString(16, "lwrobel");

            //T_PACKAGES_LOG
            psInsertT_Packages_Log.setInt(1, (Integer) packageID);
            psInsertT_Packages_Log.setInt(2, (Integer) 62222);
            psInsertT_Packages_Log.setString(3, fileVersion + " Mac OS");
            psInsertT_Packages_Log.setString(4, fileVersion);
            psInsertT_Packages_Log.setString(5, "");
            psInsertT_Packages_Log.setString(6, "");
            psInsertT_Packages_Log.setString(7, "");
            psInsertT_Packages_Log.setString(8, "");
            psInsertT_Packages_Log.setString(9, "");
            psInsertT_Packages_Log.setString(10, "Mac OS");
            psInsertT_Packages_Log.setString(11, " ");
            psInsertT_Packages_Log.setString(12, "");
            psInsertT_Packages_Log.setString(13, "");
            psInsertT_Packages_Log.setString(14, fileVersion);
            psInsertT_Packages_Log.setString(15, "lwrobel");


            psInsertT_Packages.execute();
            psInsertT_PA_LUT.execute();
            psInsertT_UPAR_LUT.execute();
            psInsertT_Packages_Log.execute();

            connect.commit();


        } catch (SQLException e) {
            System.out.println("Exception has been thrown while adding new versions: '" + e.getMessage() + "'");
            System.out.println("SQL State: " + e.getSQLState() + ", Error Code: " + e.getErrorCode());
            if (connect != null) {
                connect.rollback();
                System.out.println("Connection rollback...");
            }
            e.printStackTrace();
        } finally {
            if (connect != null && !connect.isClosed()) {
                connect.close();
            }
        }
    }
}
