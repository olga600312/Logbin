package util;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.sql.*;

public class Logger {
    public static final String INFO =    "INFO       ";
    public static final String ERROR =   "ERR        ";
    public static final String WARNING = "WARN       ";
    public static final String FATAL =   "FATAL      ";

    private static SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss      ");
    public static void log(String str) {
        log(INFO, str);
    }



    public static void log(Throwable ex) {
        error("==================== ERROR =======================");
        ex.printStackTrace();
    }

    public static void log(Exception ex) {
        error("==================== ERROR =======================");
        ex.printStackTrace();
    }


    public static void log(Object str) {
        if (str != null) {
            log(str.toString());
        }
    }

    public static void log(String type, String str) {
      //  System.err.println((format.format(new Date())) + type + str);
        System.err.println(Thread.currentThread().getName()+"\t" + type + str);
    }

    public static void logWithoutNewLine(String str) {
        System.err.print(str);
    }


    public static void warning(Object str) {
        if (str != null) {
            log(WARNING, str.toString());
        }
    }

    public static void error(Object str) {
        if (str != null) {
            log(ERROR, str.toString());
        }
    }


}
