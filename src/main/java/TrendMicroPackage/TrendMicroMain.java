package TrendMicroPackage;
import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipFile;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;




public class TrendMicroMain {

    static String fileName;
    static String fileSize;

    public static void main(String[] args) throws IOException{
          downloadZipFile();
          readFileInfo();
          saveInfoToFile();
    }



    /**
     *Function that connect to download center and create file path to zip file.
     * @return currentFilePath
     * @throws IOException
     */

    public static String getTrendMicroFilePath() throws IOException {

        Document doc;

        // Filepath to main download center
        String downloadCenterPath = "https://downloadcenter.trendmicro.com/index.php?clk=tab_pattern&clkval=1&regs=nabu&lang_loc=1";
        // connect to main download center
        doc = Jsoup.connect(downloadCenterPath).get();

        // query <h2> - this tag store name of current file
        fileName = doc.select("h2").text().substring(31, 34);

        // create new downloading path with current file
        String currentFilePath = "https://www.trendmicro.com/ftp/products/aupattern/ent95/lpt" + fileName + ".zip";

        return currentFilePath;
    }



    /**
     *Function that download a zip file by getting url from getFilePathUrl function.
     * @throws IOException
     */

    public static void downloadZipFile() throws IOException {

        String saveFilePath = "Files\\";
        // Create InputSteam object for download file
        InputStream in = URI.create(getTrendMicroFilePath()).toURL().openStream();

        // Saved file to Files dir path with replace existing option
        Files.copy(in, Paths.get(saveFilePath + "lpt$vpn" + fileName + ".zip"),
                StandardCopyOption.REPLACE_EXISTING);
        in.close();
    }



    /**
     * Function that read file information and save it (file name and file size) to static's variables
     * @throws IOException
     */

    public static void readFileInfo() throws IOException {
        ZipFile trendMicroZipFile = new ZipFile("Files\\" +
                "lpt$vpn" + fileName + ".zip");

        long size = trendMicroZipFile.entries().nextElement().getSize();
        fileSize = Long.toString(size);
    }



    /**
     * Function that save information stored in static's variables to filename.txt in Files dir
     * @throws IOException
     */

    public static void saveInfoToFile() throws IOException {
        File filePath = new File("Files\\" + "lpt$vpn" + fileName + ".txt");

        // write to file
        FileWriter fw = new FileWriter(filePath, false);
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write("lpt$vpn." + fileName);
        bw.write("\n");
        bw.write(fileSize);
        bw.close();
        System.out.println("Write:" + "lpt$vpn" + fileName);
        System.out.println(fileSize);

    }
}
