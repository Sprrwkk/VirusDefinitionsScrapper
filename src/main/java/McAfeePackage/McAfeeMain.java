package McAfeePackage;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by lwrobel on 21.02.20.
 */
public class McAfeeMain {

    static String mcAfeeFile;
    static String currentFilePath;
    static String fileSize;




    // date settings
    static Date date;

    static SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a");

    static String pattern =  "yyyy-MM-dd";
    static SimpleDateFormat filePathDate = new SimpleDateFormat(pattern);
    static String filePathFormattedDate = filePathDate.format(new Date());
    static int packageID;



    // database settings
    static String database = "jdbc:odbc:Driver={Microsoft Access Driver (*.mdb)};DBQ=E:\\access\\##" + " Windows" + " Updates\\AMTablesWINDOWS UPDATES.mdb";
    static Connection connect = null;
    static java.sql.Statement statement = null;


    // create var's for insert's
    static PreparedStatement psInsertT_PA_LUT = null;
    static PreparedStatement psInsertT_PAR_LUT = null;
    static PreparedStatement psInsertT_UPAR_LUT = null;
    static PreparedStatement psInsertT_Packages = null;
    static PreparedStatement psInsertT_Packages_Log = null;


    static PreparedStatement psInsertT_PA_LUTWithoutSize = null;
    static PreparedStatement psInsertT_PAR_LUTWithoutSize = null;
    static PreparedStatement psInsertT_UPAR_LUTWithoutSize = null;
    static PreparedStatement psInsertT_PackagesWithoutSize = null;
    static PreparedStatement psInsertT_Packages_LogWithoutSize = null;

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


    static String insertT_PAR_LUTWithoutSize = "INSERT INTO T_PAR_LUT"
            + "(AUTONUMBER, FILENAME, SIZE, CRC, TRANSLATIONKEY, VERSIONSTRING, LICENSEDEFINITIONFILE, AUDITID, AUTHOR, FILEPATH, FILEDATE) VALUES"
            + "(?,?,?,?,?,?,?,?,?,?,?)";
    static String insertT_UPAR_LUTWithoutSize = "INSERT INTO T_UPAR_LUT (AUTONUMBER, AUDITID, FILENAME, VERSIONSTRING, CHECKSUM) VALUES"
            + "(?,?,?,?,?)";
    static String insertT_PA_LUTWithoutSize = "INSERT INTO T_PA_LUT (AUDITID, PACKAGEID, AUTHOR, SOURCE) VALUES"
            + "(?,?,?,?)";
    static String insertT_PackagesWithoutSize = "INSERT INTO T_PACKAGES (PACKAGE_ID, SWPRODUCT_ID, VERSION, MAINVER, SUBVER1, SUBVER2, SUBVER3, SP_SR, SP_SRONLY, OS, LANGUAGE, VARIATION, EDITION, GENERICVER, CREATIONDATE, AUTHOR) VALUES"
            + "(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
    static String insertT_Packages_LogWithoutSize = "INSERT INTO T_PACKAGES_LOG (PACKAGE_ID, SWPRODUCT_ID, VERSION, MAINVER, SUBVER1, SUBVER2, SUBVER3, SP_SR, SP_SRONLY, OS, LANGUAGE, VARIATION, EDITION, GENERICVER, USERNAME) VALUES"
            + "(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";



    public static void main(String[] args) throws Exception {

        runningScript();

    }


    public static void runningScript() throws Exception {
//
        System.out.println("Running script...");
        System.out.println("Creating filepath...");
        getMcAfeeFilePath();
        System.out.println("Filepath creating finished!");
        System.out.println("Downloading McAfee Zip file...");
        downloadMcAfeeZipFile();
        System.out.println("Downloading success!" + "\n" + mcAfeeFile);
        System.out.println("Reading McAfee Zip file...");
        readMcAfeeFileInfo();
        System.out.println("Reading success!");
        System.out.println("Saving McAfee file...");
        saveMcAfeeInfoToFile();
        System.out.println("Saving success!");
        addNewDefinition();

    }



    /**
     * Function that connect to download center and create file path to zip file.
     * @return currentFilePath
     * @throws IOException
     */

    public static String getMcAfeeFilePath() throws IOException {

        Document doc;

        // Filepath to main download center
        String downloadCenterPath = "http://downloadcenter.mcafee.com/products/commonupdater2/current/vscandat1000/dat/0000/";
        // connect to main download center
        doc = Jsoup.connect(downloadCenterPath).get();

        // query <h2> - this tag store name of current file
        Elements fileName = doc.select("a[href$=\".zip\"]");

        for (Element x: fileName
        ) {
            mcAfeeFile = x.text();
            currentFilePath = "http://downloadcenter.mcafee.com/products/commonupdater2/current/vscandat1000/dat/0000/" + x.text();
        }
        return currentFilePath;
    }



    /**
     *Function that download a zip file by getting url from getMcAfeeFilePath function.
     * @throws IOException
     */

    public static void downloadMcAfeeZipFile() throws IOException {

        String saveFilePath = "Files\\";
        // Create InputSteam object for download file
        InputStream in = URI.create(getMcAfeeFilePath()).toURL().openStream();

        // Saved file to Files dir path with replace existing option
        Files.copy(in, Paths.get(saveFilePath + mcAfeeFile),
                StandardCopyOption.REPLACE_EXISTING);
        in.close();

    }



    /**
     * Function that read file information and save it (file name and file size) to static's variables
     * @throws IOException
     */

    public static void readMcAfeeFileInfo() throws IOException {


        ZipFile mcAfeeZipFile = new ZipFile("Files\\" + mcAfeeFile);
        Enumeration e = mcAfeeZipFile.entries();

        while(e.hasMoreElements())
        {
            ZipEntry entry = (ZipEntry)e.nextElement();

            /*
             * To get compressed size of zip entry, use
             *
             * long getCompressedSize()
             * method of ZipEntry class.
             *
             * This method returns the size of compressed entry or -1
             * if the size is not known.
             *
             * PLEASE NOTE that uncompressed size and compressed size
             * of the zip entries created using ZipEntry.STORED method
             * will be same.
             *
             * However, It differes for zip entries created using
             * ZipEntry.DEFLATED entries.
             */
            if (entry.toString().equalsIgnoreCase("avvscan.dat")) {
                Long size = entry.getCompressedSize();
                fileSize = size.toString();
            }
        }


        mcAfeeZipFile.close();
    }



    /**
     * Function that save information stored in static's variables to filename.txt in Files dir
     * @throws IOException
     */

    public static void saveMcAfeeInfoToFile() throws IOException {

        File filePath = new File("Files\\" + mcAfeeFile.substring(0,15).replace(".zip", "") + ".txt");

        // write to file
        FileWriter fw = new FileWriter(filePath, false);
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(mcAfeeFile.substring(0,15).replace(".zip", ""));
        bw.write("\n");
        bw.write(fileSize);
        bw.close();
        System.out.println("Write:" + mcAfeeFile);
        System.out.println(fileSize);

    }

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

    public static void addNewDefinition() throws Exception {

        String defFileName;
        int defSize;
        String defFileVersion;


        int ruleID;
        int auditID;

        int ruleIDWithoutSize;
        int auditIDWithoutSize;

        int packageID;



        try {
            defFileName = mcAfeeFile;
            defSize = Integer.parseInt(fileSize);
            defFileVersion = mcAfeeFile.substring(7, 11);
            date = new Date();
            connect = DriverManager.getConnection(database, "", "");
            connect.setAutoCommit(false);


            psInsertT_PAR_LUT = connect.prepareStatement(insertT_PAR_LUT);
            psInsertT_PA_LUT = connect.prepareStatement(insertT_PA_LUT);
            psInsertT_Packages = connect.prepareStatement(insertT_Packages);
            psInsertT_Packages_Log = connect.prepareStatement(insertT_Packages_Log);


            psInsertT_PAR_LUTWithoutSize = connect.prepareStatement(insertT_PAR_LUTWithoutSize);
            psInsertT_PA_LUTWithoutSize = connect.prepareStatement(insertT_PA_LUTWithoutSize);
            psInsertT_PackagesWithoutSize = connect.prepareStatement(insertT_PackagesWithoutSize);
            psInsertT_Packages_LogWithoutSize = connect.prepareStatement(insertT_Packages_LogWithoutSize);


            ruleID = selectFirstFree(sRuleIDSelect);
            auditID = selectFirstFree(sAuditIDSelect);
            packageID = selectFirstFree(sPackageIDSelect);
            int exsistingPackageID = packageID;



            if (ruleID <= 0 || auditID <= 0 || packageID <= 0) {
                System.out.println("Failed to retrieve free ID values from database! Cannot proceed with adding definitions!!!");
                System.out.println("ruleID = " + ruleID + ", auditID = " + auditID + ", packageID = " + packageID);
                System.exit(1);
            }


            //System.out.println("ruleID = " + ruleID + ", auditID = " + auditID + ", packageID = " + packageID);


            //T_PAR_LUT (file with size)
            psInsertT_PAR_LUT.setInt(1, (Integer) ruleID);
            psInsertT_PAR_LUT.setString(2, "avvscan.dat");
            psInsertT_PAR_LUT.setInt(3, defSize);
            psInsertT_PAR_LUT.setInt(4, -1);
            psInsertT_PAR_LUT.setString(5, "-1");
            psInsertT_PAR_LUT.setString(6, " ");
            psInsertT_PAR_LUT.setInt(7, -1);
            psInsertT_PAR_LUT.setInt(8, (Integer) auditID);
            psInsertT_PAR_LUT.setString(9, "lwrobel");
            psInsertT_PAR_LUT.setString(10, null);
            psInsertT_PAR_LUT.setString(11, null);



            //T_PA_LUT (file with size)
            psInsertT_PA_LUT.setInt(1, (Integer) auditID);
            psInsertT_PA_LUT.setInt(2, (Integer) packageID);
            psInsertT_PA_LUT.setString(3, "lwrobel");
            psInsertT_PA_LUT.setString(4, null);



            //T_PACKAGES (file with size)
            psInsertT_Packages.setInt(1, (Integer) packageID);
            psInsertT_Packages.setInt(2, (Integer) 17044);
            psInsertT_Packages.setString(3, defFileVersion);
            psInsertT_Packages.setString(4, defFileVersion);
            psInsertT_Packages.setString(5, defFileVersion);
            psInsertT_Packages.setString(6, defFileVersion);
            psInsertT_Packages.setString(7, "");
            psInsertT_Packages.setString(8, "");
            psInsertT_Packages.setString(9, "");
            psInsertT_Packages.setString(10, "Win");
            psInsertT_Packages.setString(11, "");
            psInsertT_Packages.setString(12, "");
            psInsertT_Packages.setString(13, "");
            psInsertT_Packages.setString(14, defFileVersion);
            psInsertT_Packages.setString(15, df.format(date));
            psInsertT_Packages.setString(16, "lwrobel");


            //T_PACKAGES_LOG (file with size)
            psInsertT_Packages_Log.setInt(1, (Integer) packageID);
            psInsertT_Packages_Log.setInt(2, (Integer) 17044);
            psInsertT_Packages_Log.setString(3, defFileVersion);
            psInsertT_Packages_Log.setString(4, defFileVersion);
            psInsertT_Packages_Log.setString(5, defFileVersion);
            psInsertT_Packages_Log.setString(6, defFileVersion);
            psInsertT_Packages_Log.setString(7, "");
            psInsertT_Packages_Log.setString(8, "");
            psInsertT_Packages_Log.setString(9, "");
            psInsertT_Packages_Log.setString(10, "Win");
            psInsertT_Packages_Log.setString(11, " ");
            psInsertT_Packages_Log.setString(12, "");
            psInsertT_Packages_Log.setString(13, "");
            psInsertT_Packages_Log.setString(14, defFileVersion);
            psInsertT_Packages_Log.setString(15, "lwrobel");


            psInsertT_Packages.execute();
            psInsertT_PA_LUT.execute();
            psInsertT_PAR_LUT.execute();
            psInsertT_Packages_Log.execute();
            connect.commit();

            ruleIDWithoutSize = selectFirstFree(sRuleIDSelect);
            auditIDWithoutSize = selectFirstFree(sAuditIDSelect);

            if (ruleIDWithoutSize <= 0 || auditIDWithoutSize <= 0 || exsistingPackageID <= 0) {
                System.out.println("Failed to retrieve free ID values from database! Cannot proceed with adding definitions!!!");
                System.out.println("ruleID = " + ruleID + ", auditID = " + auditID + ", packageID = " + packageID);
                System.exit(1);
            }

            //T_PAR_LUT (file without size)
            psInsertT_PAR_LUTWithoutSize.setInt(1, (Integer) ruleIDWithoutSize);
            psInsertT_PAR_LUTWithoutSize.setString(2, "avvscan.dat");
            psInsertT_PAR_LUTWithoutSize.setInt(3, -1);
            psInsertT_PAR_LUTWithoutSize.setInt(4, -1);
            psInsertT_PAR_LUTWithoutSize.setString(5, "-1");
            psInsertT_PAR_LUTWithoutSize.setString(6, " ");
            psInsertT_PAR_LUTWithoutSize.setInt(7, -1);
            psInsertT_PAR_LUTWithoutSize.setInt(8, (Integer) auditIDWithoutSize);
            psInsertT_PAR_LUTWithoutSize.setString(9, "lwrobel");
            psInsertT_PAR_LUTWithoutSize.setString(10, null);
            psInsertT_PAR_LUTWithoutSize.setString(11, filePathFormattedDate+"*");



            //T_PA_LUT (file without size)
            psInsertT_PA_LUTWithoutSize.setInt(1, (Integer) auditIDWithoutSize);
            psInsertT_PA_LUTWithoutSize.setInt(2, (Integer) packageID);
            psInsertT_PA_LUTWithoutSize.setString(3, "lwrobel");
            psInsertT_PA_LUTWithoutSize.setString(4, null);

            //T_PACKAGES_LOG (file without size)
            psInsertT_Packages_LogWithoutSize.setInt(1, (Integer) packageID);
            psInsertT_Packages_LogWithoutSize.setInt(2, (Integer) 17044);
            psInsertT_Packages_LogWithoutSize.setString(3, defFileVersion);
            psInsertT_Packages_LogWithoutSize.setString(4, defFileVersion);
            psInsertT_Packages_LogWithoutSize.setString(5, defFileVersion);
            psInsertT_Packages_LogWithoutSize.setString(6, defFileVersion);
            psInsertT_Packages_LogWithoutSize.setString(7, "");
            psInsertT_Packages_LogWithoutSize.setString(8, "");
            psInsertT_Packages_LogWithoutSize.setString(9, "");
            psInsertT_Packages_LogWithoutSize.setString(10, "Win");
            psInsertT_Packages_LogWithoutSize.setString(11, " ");
            psInsertT_Packages_LogWithoutSize.setString(12, "");
            psInsertT_Packages_LogWithoutSize.setString(13, "");
            psInsertT_Packages_LogWithoutSize.setString(14, defFileVersion);
            psInsertT_Packages_LogWithoutSize.setString(15, "lwrobel");

            psInsertT_PA_LUTWithoutSize.execute();
            psInsertT_PAR_LUTWithoutSize.execute();
            psInsertT_Packages_LogWithoutSize.execute();

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


