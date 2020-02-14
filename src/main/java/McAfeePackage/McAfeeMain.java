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
import java.util.zip.ZipFile;

public class McAfeeMain {

    static String mcAfeeFile;
    static String currentFilePath;
    static String fileSize;
    
    public static void main(String[] args) throws IOException {
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

        long size = mcAfeeZipFile.entries().nextElement().getSize();
        fileSize = Long.toString(size);

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
}
