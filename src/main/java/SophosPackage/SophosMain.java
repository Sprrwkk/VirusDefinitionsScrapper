package SophosPackage;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.*;



public class SophosMain {


    public static void main(String[] args) throws IOException {
        writeToFileSophos();
    }


    public static void writeToFileSophos() throws IOException {
        Document doc;

        // Create SB object to store file element's
        StringBuilder fileInfo = new StringBuilder();
        String filePath = "Files\\";

        // Connect with Sophos latest IDE xml
        doc = Jsoup.connect("https://downloads.sophos.com/downloads/info/latest_IDE.xml").get();

        // get filename
        Elements fileName = doc.select("name");
        //get filesize
        Elements fileSize = doc.select("size");
        // get published date
        Elements datePublished = doc.select("published");

        // append file elements to fileInfo
        for (Element file: fileName
        ) {
            fileInfo.append(file.text() + "\n");
            filePath += file.text() + ".txt";
        }

        for (Element size: fileSize) {
            fileInfo.append(size.text() + "\n");
        }

        for (Element date: datePublished) {
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

}


