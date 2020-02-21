import McAfeePackage.McAfeeMain;
import SophosPackage.SophosMain;
import TrendMicroPackage.TrendMicroMain;

import java.io.IOException;
import java.sql.SQLException;

public class MainApp {

    public static void main(String[] args) throws IOException, SQLException, ClassNotFoundException {
//
//        //McAfee Script
       //McAfeeMain.runningScript();
//
//        //TrendMicro Script
       TrendMicroMain.runningScript();
//
//        //Sophos Script
//        //SophosMain.runningScript();


    }

}
