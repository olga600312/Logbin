package logbin;

import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.security.*;
import java.sql.*;
import java.sql.Timestamp;
import java.text.*;
import java.util.*;
import java.util.zip.*;
import javax.xml.ws.*;

import updater.*;
import util.LogbinConstants;
import util.Logger;
import ws.*;

public class LogManager {
    private static LogManager instance;
    private String regId;
    public static final String DUMP = "DUMP";
    public static final String LOGBIN = "TRANSFER";
    public static final String SYSTEM = "SYSTEM";
    public static final String ERROR_MEMO = "Error-> ";
    public static final String LB_VERSION = "1.0.75";

    public static final int COMMAND_SYSDEFINE = 1;
    public static final int COMMAND_PEDIT = 2;
    public static final int COMMAND_CLIENTRES = 3;
    public static final int COMMAND_FILE = 4;
    public static final int COMMAND_COMMAND = 5;
    public static final int COMMAND_BIAGENT = 6;
    public static final int COMMAND_QUERY = 7;

    public static final int STATUS_OK = 0;
    public static final int STATUS_INVALID = 1;
    public static final int STATUS_SQL_ERROR = 2;
    public static final int STATUS_NO_SUCH_FIELD = 3;
    public static final int STATUS_DOWNLOAD_ERROR = 4;
    public static final int STATUS_MD5_ERROR = 5;
    public static final int STATUS_EXE_ERROR = 6;
    public static final int STATUS_CANT_START_SERVICE = 7;
    public static final int STATUS_UPDATE_ERROR = 99;

    public static final String PATH_JAVA = "$JAVA$";
    public static final String PATH_SYSTEM = "$SYS$";
    public static final String PATH_HOME = "$HOME$";
    public static final String PATH_ASH = "$ASH$";

    private byte[] indexMD5;
    private String executable;
    private long timeout = 1200000; // 20 min
    private static final int DEFAULT_LOG_CHECK_INTERVAL = 30;
    private long pingRunLastTime = 0;
    private CommandManager mCommandManager;
    public static String SERVER_PATH = "http://192.168.55.13";
    private Integer monitor = new Integer(1);
    private Synchronizer mSynchronizer = new Synchronizer();
    private java.util.Timer keepAliveTimer;
    private Timer commandTaskTimer;

    private LogManager() {
        util.Logger.log("Current Logbin Version: " + LB_VERSION);
        init();
    }

    public static LogManager getInstance() {
        if (instance == null) {
            instance = new LogManager();
        }
        return instance;
    }

    public String getExecutable() {
        return executable;
    }

    public String getRegId() {
        return regId;
    }

    private void init() {

        try {
            boolean fl = false;
            String typeTable = isMySQL5() ? "ENGINE" : "TYPE";

            if (!SQLAdmin.getInstance().isTableExists("logbin_command")) {
                SQLAdmin.getInstance().execute("CREATE TABLE `logbin_command` (`id` int(10) unsigned NOT NULL," +
                        "`type` int(10) unsigned NOT NULL,`prop_name` varchar(45) NOT NULL,`prop_value` text NOT NULL,`tm_exec` datetime DEFAULT NULL," +
                        "`isinteractive` int(10) unsigned NOT NULL DEFAULT '0',`memo` varchar(45) DEFAULT NULL,`dependency_id` int(10) unsigned DEFAULT NULL," +
                        " status int(10) ,tm_status datetime,ack TINYINT(1) UNSIGNED DEFAULT '0' , PRIMARY KEY (`id`)) " + typeTable + "=MyISAM");
                fl = true;
            }

            if (!SQLAdmin.getInstance().isTableExists("logbin_defines")) {
                SQLAdmin.getInstance().execute("CREATE TABLE `logbin_defines` (" +
                        "`id` CHAR(255) NOT NULL,`name` CHAR(255) NOT NULL," + " PRIMARY KEY (`id`))" + typeTable + " = MyISAM;");
                setLogbinParam("timeout", DEFAULT_LOG_CHECK_INTERVAL);

                fl = true;
            } else {
                String tm = getLogbinParam("timeout");
                if (tm == null) {
                    setLogbinParam("timeout", DEFAULT_LOG_CHECK_INTERVAL);
                } else {
                    try {
                        long r = Long.parseLong(tm);
                        if (r < DEFAULT_LOG_CHECK_INTERVAL) {
                            r = DEFAULT_LOG_CHECK_INTERVAL;
                            setLogbinParam("timeout", DEFAULT_LOG_CHECK_INTERVAL);
                        }
                        timeout = r * 60 * 1000;

                    } catch (Throwable ex1) {
                        util.Logger.log(ex1);
                        setLogbinParam("timeout", DEFAULT_LOG_CHECK_INTERVAL);
                    } finally {
                        util.Logger.log("Default Log Check Interval: " + timeout);
                    }
                }

        /*tm = getLogbinParam("timeout_terminals_info");
                if (tm == null) {
          setLogbinParam("timeout_terminals_info", DEFAULT_LOG_CHECK_INTERVAL);
                }
                else {
          try {
            long r = Long.parseLong(tm);
            if (r < DEFAULT_LOG_CHECK_INTERVAL) {
              r = DEFAULT_LOG_CHECK_INTERVAL;
              setLogbinParam("timeout_terminals_info", DEFAULT_LOG_CHECK_INTERVAL);
            }
            timeoutTerminals = r * 60 * 1000;
          }
          catch (Throwable ex1) {
            util.Logger.log(ex1);
            setLogbinParam("timeout_terminals_info", DEFAULT_LOG_CHECK_INTERVAL);
          }
                }*/


            }
            if (!SQLAdmin.getInstance().isTableExists("logbin_info")) {
                SQLAdmin.getInstance().execute("CREATE TABLE `logbin_info` (`id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT," +
                        "`date` DATETIME NOT NULL, `type` CHAR(100) NOT NULL,`name` CHAR(100) NOT NULL, `ordinal` INT(10) UNSIGNED NOT NULL DEFAULT 0,`err_code` INT(10) UNSIGNED NOT NULL, `memo` CHAR(255) NOT NULL," + " PRIMARY KEY (`id`)) " + typeTable + " = MyISAM;");
                fl = true;
            }

            while (!checkTerminalId()) {
                util.Logger.log("Cant get regId value. Sleep for 3 hours");
                try {
                    Thread.sleep(10800000); // sleep for 3 hours
                } catch (InterruptedException ex2) {
                }
            }

            if (mCommandManager == null) {
                mCommandManager = new CommandManager();
            }
            if (fl) {
                // createSnapshot(); //commented temporary for testing
                ping();
            }

            if (!checkLogBinInstalled()) {
                util.Logger.error("Cant install binlog process....");
                System.exit(6);
            }

        } catch (SQLException ex) {
            util.Logger.log(ex);
            System.exit(3);
        }
        setCommandTaskTimer();
        setStatusTick();
    }

    public void start() {
        util.Logger.log("First check if script lock exists unlock it.");
        ping();
        if ("TRUE".equalsIgnoreCase(LogManager.getInstance().getLogbinParam("STATUS"))) {
            runTerminalAgent();
        }
        unlockScript();
        if (initIndex()) {
            indexMD5 = null;
            process();
        } else {
            util.Logger.error("Cant access index file....");
            System.exit(7);
        }

    }

    private void sleep() {
        int sleepTime = 10800000;
        try {
            long sleep = new Random(System.currentTimeMillis() % Long.parseLong(regId)).nextInt(sleepTime);
            util.Logger.log("Sleep for " + (sleep / 1000 / 60) + " min ");
            Thread.sleep(sleep);
        } catch (InterruptedException ex1) {
        } catch (NumberFormatException ex1) {
            try {
                Thread.sleep(new Random(System.currentTimeMillis()).nextInt(sleepTime));
            } catch (InterruptedException ex) {
            }
        }
    }

    private void process() {
        util.Logger.log("Log bin process started.");
        boolean fixUpdates = false;
        while (true) {
            try {
                if (!fixUpdates) {
                    fixUpdates = fixUpdates();
                }
                if (isFullDump()) {
                    createSnapshot();
                    // sleep to avoid overload on ftp server
                    sleep();
                    runScript();
                } else if (isLogBinInstalled()) {
                    checkLogs();
                } else {
                    util.Logger.log("Logbin process is not defined in the MySQL. Check my.ini file.");
                }
                if (System.currentTimeMillis() - pingRunLastTime > 24 * 60 * 60 * 1000) { //24 hours
                    ping();
                }
            } catch (Throwable ex) {
                util.Logger.log("Exception is throwed in the 'process' function. Todo remove after fix the issue. Timeout is " + timeout);
                util.Logger.log(ex);
            }
            try {
                Thread.sleep(timeout);
            } catch (InterruptedException ex) {
            }
        }
    }

    private void createFullDump() {
        util.Logger.log("Create full dump .....");
        String basedir = this.getBaseDir();
        if (basedir != null) {
            String command = basedir + File.separatorChar + "bin" + File.separatorChar + "mysqldump";
            command = command.replaceAll("\\\\", "/");
            String logbinPath = this.getLogBinPath();
            if (logbinPath != null) {
                String fileName = logbinPath + File.separatorChar + "fulldump_" + regId + "_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new java.util.Date());
                fileName = fileName.replaceAll("\\\\", "/");
                String[] cmdArr = new String[8];
                cmdArr[0] = command;
                cmdArr[1] = SQLAdmin.getInstance().getDbCatalog();
                cmdArr[2] = "-u" + SQLAdmin.getInstance().getUser();
                cmdArr[3] = "-p" + SQLAdmin.getInstance().getPassword();
                cmdArr[4] = "-e";
                cmdArr[5] = "-Q";
                cmdArr[6] = "-r";
                cmdArr[7] = "\"" + fileName + "\"";
                int res = 9999;
                String memo = ERROR_MEMO;

                try {

                    SQLAdmin.getInstance().execute("FLUSH LOGS");
                    lastFlush = System.currentTimeMillis();
                    Process child = Runtime.getRuntime().exec(cmdArr);
                    res = child.waitFor();
          /* InputStream in = child.getInputStream();
           String encoding = SQLAdmin.getInstance().getSystemValue(SQLAdmin.DB_ENCODING);
           BufferedReader xx;

           if (encoding != null) {
             try {
               String arr[] = encoding.split("=");
               if (arr.length > 1) {
                 util.Logger.log("Used encoding is " + arr[1]);
                 xx = new BufferedReader(new InputStreamReader(in, arr[1]));
               }
               else {
                 util.Logger.error("Unknown encoding value " + encoding + " create default encoding");
                 xx = new BufferedReader(new InputStreamReader(in));
               }
             }
             catch (UnsupportedEncodingException ex1) {
               util.Logger.log(ex1);
               util.Logger.error("Create default encoding");
               xx = new BufferedReader(new InputStreamReader(in));
             }
           }
           else {
             xx = new BufferedReader(new InputStreamReader(in));
           }

           char[] chars = new char[1024];
           int ibyte = 0;
           while ( (ibyte = xx.read()) >= 0) {
             fw.write(ibyte);
           }

           in.close();
           fw.close();
           xx.close();
           res = child.exitValue();*/
                    util.Logger.log("Dump file is [" + fileName + "] res=" + res);
                    if (res == 0) {
                        SQLAdmin.getInstance().execute("FLUSH LOGS");
                        lastFlush = System.currentTimeMillis();
                        File outFile = new File(fileName + ".zip");
                        if (outFile.exists() && !outFile.delete()) {
                            memo = "Cannot create zipDb" + outFile.getName() + ". File already exists and cant delete one";
                            util.Logger.error(memo);
                            res = 6;
                        } else {
                            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(outFile));
                            // out.setLevel(Deflater.BEST_COMPRESSION);
                            if (zipFile(fileName, out)) {
                                memo = "Monthly full dump";
                                out.close();
                                File f = new File(fileName);
                                if (f.exists() && !f.delete()) {
                                    util.Logger.error("Cannot delete unzipped source file " + f.getAbsolutePath());
                                    memo += " Cant delete source";
                                }

                            } else {
                                res = 7;
                                memo = "Cannot create zip " + outFile.getName();
                                util.Logger.error(memo);
                            }
                        }
                    } else {
                        util.Logger.error("Res =" + res);
                        String str = "";
                        for (String elem : cmdArr) {
                            str += elem + " ";
                        }
                        util.Logger.error(str);
                    }

                } catch (IOException ex) {
                    util.Logger.log(ex);
                    memo = ERROR_MEMO + ex.getMessage();
                } catch (Exception ex) {
                    /** @todo Handle this exception */
                    memo = ERROR_MEMO + ex.getMessage();
                }
                log(DUMP, fileName, res, memo);
                // sleep while  antivirus is checking this file
                try {
                    Thread.sleep(60000);
                } catch (InterruptedException ex2) {
                }
            }
        }

    }

    private boolean fixUpdates() {
        return true; //migrateToNewServer();
    }

    private boolean isFullDump() {
        boolean fl = true;

    /* if (fixBug109()) {
       util.Logger.log("Create fulldump by fixing bug 1.0.9");
       return fl;
     }

     if (fixBug131()) {
       util.Logger.log("Create fulldump by fixing bug 1.0.31");
       return fl;
     }*/

        String val = getLogbinParam("forceFullDump");
        if (val != null && "TRUE".equalsIgnoreCase(val)) {
            util.Logger.log("Force fulldump ");
            try {
                setLogbinParam("forceFullDump", "FALSE");
            } catch (SQLException ex1) {
                util.Logger.log(ex1);
            }
            return fl;
        }
        ResultSet rs = null;
        try {
            // rs = SQLAdmin.getInstance().executeQuery("SELECT max(ordinal) FROM logbin_info WHERE type='" + DUMP + "' AND EXTRACT(YEAR FROM date)=EXTRACT(YEAR FROM NOW()) AND WEEK(date)>=WEEK(NOW())"); // AND WEEK(date)%2=0");
            rs = SQLAdmin.getInstance().executeQuery("SELECT count(*) FROM logbin_info WHERE type='" + DUMP + "'");
            //  fl = !rs.next() || rs.getInt(1) == 0;
            if (!rs.next() || rs.getInt(1) == 0)
                return true;

        } catch (SQLException ex) {
            util.Logger.log(ex);
        } finally {
            SQLAdmin.closeFullResultSet(rs);
        }

        try {
            // rs = SQLAdmin.getInstance().executeQuery("SELECT max(ordinal) FROM logbin_info WHERE type='" + DUMP + "' AND EXTRACT(YEAR FROM date)=EXTRACT(YEAR FROM NOW()) AND WEEK(date)>=WEEK(NOW())"); // AND WEEK(date)%2=0");
            rs = SQLAdmin.getInstance().executeQuery("SELECT max(ordinal) FROM logbin_info WHERE type='" + DUMP + "' AND EXTRACT(YEAR FROM date)=EXTRACT(YEAR FROM NOW()) AND MONTH(date)>=MONTH(NOW())");
            //  fl = !rs.next() || rs.getInt(1) == 0;
            fl = !rs.next() || rs.getObject(1) == null; /*|| rs.getInt(1) == 0;// bug if a date falls in the last week of the previous year, MySQL returns 0 */
        } catch (SQLException ex) {
            util.Logger.log(ex);
        } finally {
            SQLAdmin.closeFullResultSet(rs);
        }
        if (fl) {
            String avivid = SQLAdmin.getInstance().getSystemValue(SQLAdmin.AVIV_ID);
            if (avivid != null && avivid.trim().length() > 0) {
                Logger.log("Check if it is time to build backup by last digit of avivId " + avivid);
                char lastChar = avivid.charAt(avivid.length() - 1);
                if (Character.isDigit(lastChar)) {
                    GregorianCalendar c = new GregorianCalendar();
                    int day = c.get(Calendar.DAY_OF_MONTH);
                    fl = day >= Integer.parseInt("" + lastChar);
                }
            }
        }
        return fl;
    }

    private long lastFlush;

    private void flushLog() {
        ResultSet rs = null;

        try {
            if (lastFlush - System.currentTimeMillis() > 86400) {
                SQLAdmin.getInstance().execute("FLUSH LOGS");
                lastFlush = System.currentTimeMillis();
            } else {
                rs = SQLAdmin.getInstance().executeQuery("SELECT max(ordinal) FROM logbin_info WHERE type='" + LOGBIN + "' AND EXTRACT(YEAR FROM date)=EXTRACT(YEAR FROM NOW()) AND WEEK(date)>=WEEK(NOW())"); // AND WEEK(date)%2=0");
                if (!rs.next() || rs.getInt(1) == 0) {
                    SQLAdmin.getInstance().execute("FLUSH LOGS");
                    lastFlush = System.currentTimeMillis();
                }
            }
        } catch (SQLException ex) {
            util.Logger.log(ex);
        } finally {
            SQLAdmin.closeFullResultSet(rs);
        }
        return;
    }

    private boolean fixBug109() {
        // fix bug at version 1.0.9
        ResultSet rs = null;

        boolean fl = false;
        try {
            rs = SQLAdmin.getInstance().executeQuery("SELECT * FROM logbin_defines WHERE id='BUG_1_0_9'");
            if (!rs.next()) {
                fl = true;
                clearLogbinTransferBuggedFiles();
                clearLogbinBuggedFiles();
                SQLAdmin.getInstance().executeUpdate("INSERT IGNORE INTO logbin_defines (ID,NAME) VALUES ('BUG_1_0_9',concat('Fixed ',NOW()))");
            }
        } catch (SQLException ex2) {
            util.Logger.log(ex2);
        } finally {
            SQLAdmin.getInstance().closeFullResultSet(rs);
        }
        return fl;
    }

    private boolean fixBug131() {
        util.Logger.log("______________   fix bug at version 1.0.31  ______________");
        ResultSet rs = null;

        boolean fl = false;
        try {
            rs = SQLAdmin.getInstance().executeQuery("SELECT * FROM logbin_defines WHERE id='BUG_1_0_31'");
            if (!rs.next()) {
                fl = true;
                clearLogbinTransferBuggedFiles();
                clearLogbinBuggedFiles();
                SQLAdmin.getInstance().executeUpdate("INSERT IGNORE INTO logbin_defines (ID,NAME) VALUES ('BUG_1_0_31',concat('Fixed ',NOW()))");
            }
        } catch (SQLException ex2) {
            util.Logger.log(ex2);
        } finally {
            SQLAdmin.getInstance().closeFullResultSet(rs);
        }
        return fl;
    }

    private void clearLogbinBuggedFiles() {
        String lbPath = getLogBinPath();
        if (lbPath != null) {
            util.Logger.log("Clear logbin old zip files ");
            File f = new File(lbPath);
            if (f.exists() && f.isDirectory()) {
                File[] files = f.listFiles();
                if (files != null) {
                    for (File elem : files) {
                        if (elem.getName().endsWith("zip") && !elem.delete()) {
                            util.Logger.log("Cant remove file " + elem);
                        }
                    }
                }
            }
        }

    }

    private void clearLogbinTransferBuggedFiles() {
        String lbTransferPath = getLogBinTransferPath();
        if (lbTransferPath != null) {
            util.Logger.log("Clear logbin_transfer ");
            File f = new File(lbTransferPath);
            if (f.exists() && f.isDirectory()) {
                File[] files = f.listFiles();
                if (files != null) {
                    for (File elem : files) {
                        if (!elem.delete()) {
                            util.Logger.log("Cant remove file " + elem);
                        }
                    }
                }
            }
        }
    }

    private boolean checkTerminalId() throws SQLException {
        util.Logger.log("Check terminal Reg Id");
        boolean fl = false;
        ResultSet rs = null;
        Connection c = SQLAdmin.getInstance().getConnection();
        rs = c.createStatement().executeQuery("SELECT * FROM sysdefines WHERE name='CREDIT_CARD_WORK_DIRECTORY'");
        String str = null;
        try {
            if (rs.next()) {
                str = rs.getString("value");
                File parent = new File(str);
                util.Logger.log("ASH PATH is " + parent.getAbsolutePath());
                if (parent != null && parent.exists() && parent.isDirectory()) {
                    File fStart = new File(parent, "START");
                    String line;
                    if (fStart.exists() && !fStart.isDirectory()) {
                        RandomAccessFile params = null;
                        try {
                            params = new RandomAccessFile(fStart.getAbsolutePath(), "r");
                            do {
                                line = params.readLine();
                            }
                            while (line != null && line.trim().length() == 0);
                            fl = line != null && line.length() > 0;
                            if (fl) {
                                char[] arr = line.toCharArray();
                                regId = "";
                                for (int i = 0; i < arr.length && i < 7; i++) {
                                    if (Character.isDigit(arr[i])) {
                                        regId += arr[i];
                                    }
                                }
                                if (fl = regId.trim().length() > 0) {
                                    String old = getLogbinParam("client_id");
                                    setLogbinParam("client_id", regId);
                                    if (!regId.equalsIgnoreCase(old)) {
                                        setLogbinParam("forceFullDump", "TRUE");
                                    } else {
                                        setLogbinParam("forceFullDump", "FALSE");
                                    }

                                    // SQLAdmin.getInstance().executeUpdate("REPLACE INTO logbin_defines (id,name) VALUES ('client_id','" + regId + "')");
                                } else {
                                    util.Logger.log("Cant get terminal id from " + fStart.getAbsolutePath() + ". It's empty.");
                                    fl = false;
                                }
                            }
                        } catch (FileNotFoundException ex) {
                            util.Logger.log(ex);
                        } catch (IOException ex) {
                            util.Logger.log(ex);
                        } finally {
                            if (params != null) {
                                try {
                                    params.close();
                                } catch (IOException ex1) {
                                    util.Logger.log(ex1);
                                }
                            }
                        }
                    } else {
                        util.Logger.log("START file not accessable. " + fStart.getAbsolutePath());
                    }
                } else {
                    util.Logger.log("ASH path not accessable. '" + parent.getAbsolutePath() + "'; exists -> " + parent.exists() + "; is directory -> " + parent.isDirectory());
                }
            }
        } finally {
            SQLAdmin.closeFullResultSet(rs);
        }
        return fl;
    }

    private void setLogbinParam(String key, Object value) throws SQLException {
        SQLAdmin.getInstance().executeUpdate("REPLACE INTO logbin_defines (id,name) VALUES ('" + key + "','" + value + "')");
    }

    private String getLogbinParam(String key) {
        ResultSet rs = null;
        String value = null;
        try {
            rs = SQLAdmin.getInstance().executeQuery("SELECT  * FROM logbin_defines WHERE id='" + key + "'");
            if (rs.next()) {
                value = rs.getString("name");
            }
        } catch (SQLException ex) {
            util.Logger.log(ex);
        } finally {
            SQLAdmin.getInstance().closeFullResultSet(rs);
        }
        return value;
    }

    private boolean checkLogBinInstalled() {
        Connection con = SQLAdmin.getInstance().getConnection();
        boolean fl = false;
        try {
            fl = isLogBinInstalled();
            if (!fl) {
                util.Logger.log("Log bin is not installed. Try install it...");
                if (!setupLogBin(getMyIni())) {
                    util.Logger.log("Cant add log bin definitions to my.ini ");
                } else {
                    util.Logger.log("Log bin process was installed successfully. Wait for MySQL restart...");
                    while (!fl) {
                        Thread.sleep(3 * 60 * 1000);
                        if (!(fl = isLogBinInstalled())) {
                            ;
                            util.Logger.log("Wait for MySQL restart...");
                        }
                    }
                    util.Logger.log("Logbin process is running!");
                }
            } else {
                util.Logger.log("Log bin already installed.");
            }
        } catch (Exception ex) {
            util.Logger.log(ex);
        }
        return fl;
    }

    private boolean isLogBinInstalled() {
        ResultSet rs = null;
        try {
            Connection con = SQLAdmin.getInstance().getConnection();
            rs = con.createStatement().executeQuery("show variables like 'log_bin'");
            return rs.next() && "ON".equalsIgnoreCase(rs.getString("value"));
        } catch (SQLException ex) {
            return false;
        } finally {
            SQLAdmin.closeFullResultSet(rs);
        }

    }

    private String getBaseDir() {
        String basedir = null;
        Connection con = SQLAdmin.getInstance().getConnection();
        boolean fl = false;
        ResultSet rs = null;
        try {
            rs = con.createStatement().executeQuery("show variables like 'basedir'");
            basedir = rs.next() ? rs.getString("value") : null;
        } catch (SQLException ex) {
            util.Logger.log(ex);
        } finally {
            SQLAdmin.closeFullResultSet(rs);
        }
        return basedir;

    }

    private boolean setupLogBin(File myini) {
        boolean fl = false;
        String COMMAND_STR = "log-bin";
        String BASEDIR_STR = "basedir";
        String s = "asdas";
        if (myini != null) {
            try {
                FileInputStream f = new FileInputStream(myini);
                byte[] b = new byte[f.available()];
                f.read(b);
                String text = new String(b);
                String textl = text.toLowerCase();
                StringBuffer newText = new StringBuffer();
                int p1 = textl.indexOf(COMMAND_STR);
                if (p1 >= 0) {
                    p1 = textl.lastIndexOf("\n", p1) + 1;
                    int p2 = textl.indexOf("\n", p1) + 1;
                    if (p2 == 0) {
                        p2 = textl.length();
                    }
                    newText.append(text.substring(0, p1)).append(text.substring(p2));
                } else {
                    newText.append(text);
                }
                String output = newText.toString();
                String basedir = getBaseDir();
                basedir = basedir.replaceAll("\\\\", "/");
                output = output.replaceFirst("basedir",
                        "log-bin=../logbin/logg" +
                                "\r\n" +
                                "log-bin-index=../logbin/logg-index" +
                                "\r\n" +
                                "max_binlog_size=10000000" +
                                "\r\n\r\n" +
                                "basedir");

                f.close();
                FileOutputStream fo = new FileOutputStream(myini);
                fo.write(output.getBytes());
                fo.close();
                fl = true;
                File logDir = new File(basedir, "logbin");
                if (!(logDir.exists() || logDir.mkdir())) {
                    util.Logger.error("Cant create " + logDir.getAbsolutePath());
                    fl = false;
                }
                logDir = new File(basedir, "logbin-backup");
                if (!(logDir.exists() || logDir.mkdir())) {
                    util.Logger.error("Cant create " + logDir.getAbsolutePath());
                    fl = false;
                }
                if (fl) {
                    //createSnapshot(); //commented temporary for testing
                    util.Logger.log("Log bin was installed successfully! Log bin path is ../logbin/logg. Restart MySQL server.");
                }
            } catch (IOException ex) {
                util.Logger.log(ex);
            }
        }

        return fl;
    }

    private void createSnapshot() {
        ping();
        createFullDump();
        createBackupSystem();
    }

    private File getMyIni() {
        File f = null;
        String ver = SQLAdmin.getInstance().getMySQLVersion();
        if (ver != null && !ver.isEmpty()) {
            util.Logger.log("MySQL version is " + ver);
            f = ver.startsWith("4") ? getMyini4() : getMyini5();
        } else {
            util.Logger.error("Unknown version of MySQL:" + ver);
        }
        return f;
    }

    public boolean isMySQL5() {
        String ver = SQLAdmin.getInstance().getMySQLVersion();
        return ver != null && !ver.isEmpty() && ver.startsWith("5");
    }

    private File getMyini5() {
        File f = null;
        try {
            Process p = Runtime.getRuntime().exec("sc qc mysql");
            try {
                String flowString = "";
                int c;
                while ((c = p.getInputStream().read()) >= 0) {
                    byte[] b = new byte[p.getInputStream().available() + 1];
                    // int i=0;
                    //b[0] = (byte)i;
                    p.getInputStream().read(b, 1, b.length - 1);
                    String s = flowString + new String(b);
                    String defaultsFile = "--defaults-file=\"";
                    s = s.toLowerCase();
                    int p1 = s.indexOf(defaultsFile);
                    if (p1 < 0) {
                        return null;
                    }
                    p1 += defaultsFile.length();
                    int p2 = s.indexOf("\"", p1);
                    if (p2 < 0) {
                        //p2 = s.indexOf("\n", p1);
                    }

                    if (p2 < 0) {
                        p2 = s.length();
                    }

                    String str = s.substring(p1, p2);
                    f = new File(str);
                    if (!f.exists()) {
                        f = null;
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return f;
    }

    private File getMyini4() {
        File f = new File("c:/windows/my.ini");
        if (f.exists()) {
            return f;
        }
        f = new File("c:/winnt/my.ini");
        if (f.exists()) {
            return f;
        }
        return null;
    }

    private String getLogBinPath() {
        return getPath("logbin", "Logbin");
    }

    private String getLogBinBackupPath() {
        return getPath("logbin-backup", "Logbin backup");
    }

    private String getLogBinTransferPath() {
        return getPath("logbin-transfer", "Logbin transfer");
    }

    private String getPath(String dir, String name) {
        String basedir = this.getBaseDir();
        String path = null;
        if (basedir != null) {
            File indexFile = new File(basedir + File.separatorChar + dir);
            if (indexFile.exists()) {
                if (indexFile.isDirectory()) {
                    path = indexFile.getAbsolutePath();
                } else {
                    path = indexFile.getParentFile().getAbsolutePath();
                }
            } else {
                util.Logger.warning("Logbin path " + indexFile + " doesnt exist. Try create it.");
                if (indexFile.mkdirs()) {
                    if (indexFile.isDirectory()) {
                        path = indexFile.getAbsolutePath();
                    } else {
                        path = indexFile.getParentFile().getAbsolutePath();
                    }
                } else {
                    util.Logger.error(name + " path " + indexFile + " doesnt exist and cant create it!");
                }
            }
        }
        return path;
    }

    private String getBackupSuffix() {
        return new SimpleDateFormat("yyyyMMddHHmmss").format(new java.util.Date());
    }

    private boolean initIndex() {
        indexMD5 = null;
        String logbinPath = getLogBinPath();
        boolean fl = false;
        if (logbinPath != null) {
            File indexFile = new File(logbinPath, "logg-index.index");
            if (indexFile.exists()) {
                try {
                    indexMD5 = md5(indexFile);
                    fl = true;
                } catch (FileNotFoundException ex) {
                    util.Logger.log(ex);
                } catch (IOException ex) {
                    util.Logger.log(ex);
                } catch (NoSuchAlgorithmException ex) {
                    util.Logger.log(ex);
                } catch (RuntimeException ex) {
                    util.Logger.log(ex);
                }

            } else {
                util.Logger.error("Log bin index doesnt exist");
            }
        } else {
            util.Logger.error("Log bin folder doesnt exist");
        }
        return fl;
    }

    private boolean isLocked(File file) {
        boolean fl = true;
        if (file.exists()) {
            FileChannel channel = null;
            try {
                channel = new RandomAccessFile(file, "rw").getChannel();
                // Get an exclusive lock on the whole file
                FileLock lock = null;
                try {
                    lock = channel.tryLock(); // Ok. You get the lock
                    //if is not Shared => excusive
                    util.Logger.log("Lock " + lock);
                    fl = lock == null || lock.isShared(); //false
                } catch (OverlappingFileLockException ex) {
                    // File is open by someone else
                } finally {
                    if (lock != null) {
                        lock.release();
                    }
                }
            } catch (FileNotFoundException ex) {
                util.Logger.log(ex);
            } catch (IOException ex) {
                util.Logger.log(ex);
            } finally {
                if (channel != null) {
                    try {
                        channel.close();
                    } catch (IOException ex1) {
                        util.Logger.log(ex1);
                    }
                }
            }
        }
        util.Logger.log("File " + file.getName() + " is locked " + fl);
        return fl;
    }

    private boolean isEquals(File f) {
        boolean fl = false;
        byte[] md5 = null;
        try {
            md5 = md5(f);
            fl = indexMD5 != null && md5 != null && indexMD5.length == md5.length;
            for (int i = 0; fl && i < md5.length; i++) {
                fl = indexMD5[i] == md5[i];
            }
        } catch (NoSuchAlgorithmException ex) {
            util.Logger.log(ex);
        } catch (RuntimeException ex) {
            util.Logger.log(ex);
        } catch (IOException ex) {
            util.Logger.log(ex);
        }

        return fl;
    }

    private void checkLogs() {
        flushLog();
        String logbinPath = getLogBinPath();
        util.Logger.log("Check new logs in the " + logbinPath);
        if (logbinPath != null) {
            File indexFile = new File(logbinPath, "logg-index.index");
            if (indexFile.exists()) {
                File lbtransfer = new File(logbinPath + File.separatorChar + ".." + File.separatorChar + "logbin-transfer");
                String[] files = null;
                try {
                    files = lbtransfer.list();
                } catch (Exception ex) {
                    util.Logger.log(ex);
                }
                if (!isEquals(indexFile) || files != null && files.length > 0) {
                    if (runScript()) {
                        initIndex();
                    }
                    ping();
                }
            } else {
                util.Logger.error("Log bin index doesnt exist");
            }
            //  checkCommands();  // transfered into to CommandTaskTimer
        } else {
            util.Logger.error("Log bin folder doesnt exist");
        }
    }

    private String getMD5(String str) throws NoSuchAlgorithmException {
    /*byte[] bytesOfMessage = regId.getBytes();
         MessageDigest md = MessageDigest.getInstance("MD5");
         byte[] thedigest = md.digest(bytesOfMessage);
         return new String(thedigest);*/
        ResultSet rs = null;
        String res = null;
        try {
            rs = SQLAdmin.getInstance().executeQuery("SELECT MD5('" + str + "')");
            res = rs.next() ? rs.getString(1) : null;
        } catch (SQLException ex) {
            util.Logger.log(ex);
        } finally {
            SQLAdmin.closeFullResultSet(rs);
        }
        return res;

    }

    private LoginInfo createPassword(String base) {
        LoginInfo pwd = null;
        util.Logger.log("Ping WS request...");

        RegisterService service = new RegisterService();
        Register port = service.getRegisterPort();
        ((BindingProvider) port).getRequestContext();
        try {
            pwd = port.getPassword(regId);
        } catch (Exception ex) {
            util.Logger.log(ex);
        }
        return pwd;
    }

    private class ProcessTimerTask extends TimerTask {
        private Process process;

        private Process getProcess() {
            return process;
        }

        private void setProcess(Process process) {
            this.process = process;
        }


        public ProcessTimerTask(Process process) {
            this.process = process;
        }

        @Override
        public void run() {
            if (process != null) {
                Logger.warning("Destroy Process by timer ");
                process.destroy();
            }
        }
    }

    private boolean runScript() {
        boolean fl = false;
        if (executable == null || !new File(executable).exists()) {
            util.Logger.error("Script doesnt exists.");
            return fl;
        }

        String logbinPath = getLogBinPath();
        String backupSuffix = getBackupSuffix();
        String memo = "";
        int res = 9999;
        if (logbinPath != null) {

            try {
                //String pwd = getMD5(regId);
                LoginInfo logInfo = createPassword(regId);
                String user = logInfo != null ? logInfo.getUser() : null;
                String pwd = logInfo != null ? logInfo.getPwd() : null;
                if (pwd != null && !pwd.isEmpty() && user != null && !user.isEmpty()) {
                    String[] cmdArr = new String[6];
                    cmdArr[0] = executable;
                    cmdArr[1] = regId;
                    cmdArr[2] = user; // username
                    cmdArr[3] = pwd;
                    cmdArr[4] = logbinPath;
                    cmdArr[5] = backupSuffix;

                    File env = new File(executable).getParentFile();
                    util.Logger.log("------ Reg  ID " + regId + "-------");
                    util.Logger.log("------ Aviv ID " + SQLAdmin.getInstance().getSystemValue(SQLAdmin.AVIV_ID) + "------");
                    util.Logger.log("----------- Start script ---------");
                    Process child = Runtime.getRuntime().exec(cmdArr, null, env);

                    ProcessTimerTask tt = new ProcessTimerTask(child);
                    Timer t = new Timer("RunScriptTimer");
                    t.schedule(tt, 5 * 60 * 60 * 1000); // 5 hours

                    BufferedReader input = new BufferedReader(new InputStreamReader(child.getInputStream()));

                    String line = null;

                    while ((line = input.readLine()) != null) {
                        util.Logger.log("       " + line);
                    }

                    res = child.waitFor();
                    t.cancel();
                    tt.setProcess(null);
                    switch (res) {
                        case 0:
                            memo = "Transfer OK";
                            fl = true;
                            break;
                        case 4:
                            memo = "Another instance is running";
                            break;
                        case 9999:
                            memo = "Interrupted by ProcessTimerTask";
                            break;
                        default:
                            memo = "Binary log: Exited with error code " + res;
                            util.Logger.log(memo);
                            break;
                    }
                    String lbTransferPath = getLogBinTransferPath();
                    File dir;
                    String[] fileList;
                    if (fl && lbTransferPath != null && (dir = new File(lbTransferPath)).exists() && (fileList = dir.list()) != null) {
                        boolean isFile = false;
                        for (int i = 0; i < fileList.length && !isFile; i++) {
                            isFile = new File(fileList[i]).isFile();
                        }
                        if (isFile) {
                            memo += " Not all files was transferred";
                        }
                    }
                    util.Logger.log("----------- Finish script with result code=" + res + " " + memo + " ---------");
                } else {
                    memo = "Cant getMD5 of " + regId;
                    util.Logger.log(memo);
                }
            } catch (InterruptedException ex) {
                util.Logger.log(ex);
                memo = "Process was interrupted";
            } catch (IOException ex) {
                util.Logger.log(ex);
                memo = ERROR_MEMO + ex.getMessage();
            } catch (Throwable ex) {
                util.Logger.log(ex);
                memo = ERROR_MEMO + ex.getMessage();
            } finally {
                if (res != 4) {
                    unlockScript();
                }
            }

            try {
                SQLAdmin.getInstance().executeUpdate("INSERT INTO logbin_info (id,date,type,name,ordinal,err_code,memo) VALUES " +
                        " (0,NOW(),'" + LOGBIN + "','logbin',WEEK(NOW())," + res + ",'" + memo + "')");
            } catch (SQLException ex1) {
                util.Logger.log(ex1);
            }
        } else {
            util.Logger.log("Cant run script: logbin path is null");
        }
        return fl;
    }

    private void unlockScript() {
        File fe = null;
        util.Logger.log("Unlock script: " + executable);
        if (executable != null && (fe = new File(executable)) != null && fe.exists()) {
            File env = fe.getParentFile();
            String str = fe.getName();
            String[] strRun = str.split("\\.");
            if (strRun.length > 0) {
                File fRun = new File(env, strRun[0] + ".run");
                if (fRun.exists() && !fRun.delete()) {
                    util.Logger.log("Cant delete " + fRun.getAbsolutePath() + " !");
                }
            }
        } else {
            util.Logger.error("Executable " + (fe != null ? fe.getAbsolutePath() : "NULL") + " doesnt exist!");
        }
    }

    public void setExecutable(String executable) {
        if (executable == null || !new File(executable).exists()) {
            util.Logger.error("Script doesnt exists.");
        }
        this.executable = executable;
    }

    public void checkExecutable() {
        String logbinLocation = Utils.getRegistryValue("HKLM\\SOFTWARE\\Aviv\\LogBinBackup\\Settings", "Location");
        if (logbinLocation == null) {
            util.Logger.log("Logbin doesnt exist!");
            return;
        }
        util.Logger.log("Logbin path " + logbinLocation);
        String fName = "backup.cmd";
        util.ResourceManager.saveResource(fName, logbinLocation + File.separatorChar + "backup" + File.separatorChar + fName);

    }

    private byte[] md5(File f) throws java.io.IOException, RuntimeException, NoSuchAlgorithmException {
        try {
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            URI uri = null;

            if (f != null && f.isFile()) {
                InputStream is = new FileInputStream(f);
                byte[] buffer = new byte[8192];
                int read = 0;
                try {
                    while ((read = is.read(buffer)) > 0) {
                        digest.update(buffer, 0, read);
                    }
                    byte[] md5sum = digest.digest();
                    return md5sum;

                } catch (IOException e) {
                    throw new RuntimeException("Unable to process file for MD5", e);
                } finally {
                    try {
                        is.close();
                    } catch (IOException e) {
                        throw new RuntimeException("Unable to close input stream for MD5 calculation", e);
                    }
                }
            } else {
                util.Logger.error("Cant get MD5 Sum: file " + f.getAbsolutePath() + " is directory !");
                return new byte[]{};
            }
        } catch (Exception ex1) {
            util.Logger.log(ex1);
            return new byte[]{};
        }
    }

    private File createBackupSystem() {
        // File newLocationFile = null;
        util.Logger.log("Create full system backup");
        String outFilePath = getLogBinPath();
        if (outFilePath == null) {
            util.Logger.error("Logbin path is not defined");
            return null;
        }
        File outFile = new File(outFilePath);
        if (!(outFile.exists() || outFile.mkdirs())) {
            util.Logger.error("Cannot create zipDb" + outFile.getName());
            return null;
        }

        outFile = new File(outFile, "system_" + regId + "_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new java.util.Date()) + ".zip");
        if (outFile.exists() && !outFile.delete()) {
            util.Logger.error("Cannot create zipDb" + outFile.getName() + ". File already exists and cant delete one");
            return null;
        }

        try {

            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(
                    outFile));

            String path = null;
            String outFileAbsolutePath = outFile.getAbsolutePath();
            outFileAbsolutePath = outFileAbsolutePath.replaceAll("\\\\", "/");

            path = SQLAdmin.getInstance().getPath("CREDIT_CARD_WORK_DIRECTORY", false);
            if (path != null) {
                zipDir(path, out, new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        boolean fl = false;
                        if (name != null) {
                            name = name.toLowerCase();
                            return !(name.endsWith("exe") || name.endsWith("dll"));
                        }
                        return fl;
                    }
                });
                log(SYSTEM, path, 0, outFileAbsolutePath);
            }

            path = SQLAdmin.getInstance().getPath("PROMOTION_PATH");
            if (path != null) {
                zipDir(path, out);
                log(SYSTEM, path, 0, outFileAbsolutePath);
            }

            path = SQLAdmin.getInstance().getPath("PRIORITY_PATH");
            if (path != null) {
                zipDir(path, out);
                log(SYSTEM, path, 0, outFileAbsolutePath);
            }

            path = SQLAdmin.getInstance().getPath("REPORT_PATH");
            if (path != null) {
                zipDir(path, out);
                log(SYSTEM, path, 0, outFileAbsolutePath);
            }

            path = SQLAdmin.getInstance().getPath("EXPORT_SETUP");
            if (path != null) {
                Logger.log("EXPORT_PATH :" + path);
                backupInterfaceDir(path, outFileAbsolutePath, out);
            }

            path = SQLAdmin.getInstance().getPath("IMPORT_SETUP");
            if (path != null) {
                Logger.log("IMPORT_SETUP :" + path);
                backupInterfaceDir(path, outFileAbsolutePath, out);
            }

            path = SQLAdmin.getInstance().getPath("PLUGIN_PATH");
            if (path != null) {
                String tableView = SQLAdmin.getInstance().getStringParam("PATH_TABLE_VIEW", "stateItem.xml");
                zipDir(path + File.separatorChar + tableView, out);
            }

            String skinPath = SQLAdmin.getInstance().getPath("DEFAULT_SKIN", ".xml");
            if (skinPath != null) {

                File f = new File(skinPath);
                if (f.exists()) {
                    zipFile(f.getAbsolutePath(), out);
                    log(SYSTEM, f.getAbsolutePath(), 0, outFileAbsolutePath);
                } else {
                    log(SYSTEM, f.getAbsolutePath() + " doesnt exist", 0, "");
                }
            }

            path = SQLAdmin.getInstance().getSystemValue(SQLAdmin.HOME_PATH);
            if (path != null) {
                String szFileName = "lib" + File.separatorChar + "JK12.lib";
                File f = new File(path);
                if (!f.isDirectory()) {
                    f = f.getParentFile();
                }
                f = new File(f, szFileName);
                if (f.exists()) {
                    zipFile(f.getAbsolutePath(), out);
                }
            }

            out.close();

        } catch (FileNotFoundException ex1) {
            util.Logger.log(ex1);
            log(SYSTEM, "System", 1, ex1.getMessage());
        } catch (IOException ex2) {
            util.Logger.log(ex2);
            log(SYSTEM, "System", 2, ex2.getMessage());
        }
        return outFile;
    }

    private void backupInterfaceDir(String path, String outFileAbsolutePath, ZipOutputStream out) {
        String[] paths = path.split("[;,]");
        for (String s : paths) {
            File f = new File(s);
            s = f.isFile() ? f.getParent() : s;
            zipDir(s, out);
            log(SYSTEM, s, 0, outFileAbsolutePath);
        }
    }

    private void log(String type, String fileName, int res, String memo) {
        try {
            SQLAdmin.getInstance().executeUpdate("INSERT INTO logbin_info (id,date,type,name,ordinal,err_code,memo) VALUES " +
                    " (0,NOW(),'" + type + "','" + fileName + "',WEEK(NOW())," + res + ",'" + memo + "')");
        } catch (SQLException ex1) {
            try {
                String typeTable = isMySQL5() ? "ENGINE" : "TYPE";
                if (!SQLAdmin.getInstance().isTableExists("logbin_info")) {
                    util.Logger.error("Table 'logbin_info' doesnt exist. Try create it. ");
                    SQLAdmin.getInstance().execute("CREATE TABLE `logbin_info` (`id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT," +
                            "`date` DATETIME NOT NULL, `type` CHAR(100) NOT NULL,`name` CHAR(100) NOT NULL, `ordinal` INT(10) UNSIGNED NOT NULL DEFAULT 0,`err_code` INT(10) UNSIGNED NOT NULL, `memo` CHAR(255) NOT NULL," + " PRIMARY KEY (`id`))" + typeTable + " = MyISAM;");
                } else {
                    util.Logger.log(ex1);
                }

            } catch (Exception ex) {
                util.Logger.log(ex);
            }
        }
    }

    private boolean zipDir(String dir2zip, ZipOutputStream zos) {
        File zipDir = new File(dir2zip);
        boolean fl = false;
        if (zipDir.exists()) {
            fl = zipDir(dir2zip, zos, null);
        } else {
            util.Logger.log("File/folder " + zipDir + " doesnt exist");

        }
        return fl;
    }

    private boolean zipDir(String dir2zip, ZipOutputStream zos, FilenameFilter filter) {
        boolean fl = true;
        FileInputStream fis = null;
        try {
            //create a new File object based on the directory we
            // have to zip File
            File zipDir = new File(dir2zip);
            //get a listing of the directory content
            boolean isDir = zipDir.isDirectory();
            String[] dirList = isDir ? zipDir.list(filter) : new String[]{dir2zip};
            byte[] readBuffer = new byte[2156];
            int bytesIn = 0;
            //loop through dirList, and zip the files
            fl = dirList != null;
            if (!fl) {
                util.Logger.log("Empty folder " + dir2zip);
            }
            for (int i = 0; fl && i < dirList.length; i++) {
                File f = isDir ? new File(zipDir, dirList[i]) : new File(dirList[i]);
                // File f =  new File(dirList[i]);
                if (f.isDirectory()) {
                    //if the File object is a directory, call this
                    //function again to add its content recursively
                    String filePath = f.getPath();
                    zipDir(filePath, zos, filter);
                    //loop again
                    continue;
                }
                //if we reached here, the File object f was not  a directory
                //create a FileInputStream on top of f
                fis = new FileInputStream(f);
                //create a new zip entry
                //ZipEntry anEntry = new ZipEntry( f.getPath());
                ZipEntry anEntry = new ZipEntry((isDir ? f.getParentFile().getName() + File.separatorChar : "") + f.getName());
                //place the zip entry in the ZipOutputStream object

                zos.putNextEntry(anEntry);
                //now write the content of the file to the ZipOutputStream
                while ((bytesIn = fis.read(readBuffer)) != -1) {
                    zos.write(readBuffer, 0, bytesIn);
                }
                //close the Stream
                fis.close();
            }
        } catch (Exception e) {
            util.Logger.log(e);
            fl = false;
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ex) {
                    util.Logger.log(ex);
                }
            }

        }
        return fl;
    }

    private boolean zipFile(String file2zip, ZipOutputStream zos) {
        boolean fl = true;
        FileInputStream fis = null;
        try {
            //create a new File object based on the directory we
            // have to zip File
            File f = new File(file2zip);
            //get a listing of the directory content
            fl = f.exists() && !f.isDirectory();
            if (fl) {
                byte[] readBuffer = new byte[2156];
                int bytesIn = 0;
                //if we reached here, the File object f was not  a directory
                //create a FileInputStream on top of f
                fis = new FileInputStream(file2zip);
                //create a new zip entry
                ZipEntry anEntry = new ZipEntry(f.getName());
                //place the zip entry in the ZipOutputStream object
                zos.putNextEntry(anEntry);
                //now write the content of the file to the ZipOutputStream
                while ((bytesIn = fis.read(readBuffer)) != -1) {
                    zos.write(readBuffer, 0, bytesIn);
                }
                //close the Stream
                fis.close();
            }
        } catch (Exception e) {
            util.Logger.log(e);
            fl = false;
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ex) {
                    util.Logger.log(ex);
                }
            }

        }
        return fl;
    }

    private String checkEmpty(String str, String def) {
        return str != null ? str : def != null ? def : "";
    }

    private String checkEmpty(String str) {
        return checkEmpty(str, null);
    }

    private void ping() {
        SQLAdmin.getInstance().reloadData();
        util.Logger.log("Ping WS ...");

        try {
            RegisterService service = new RegisterService();
            Register port = service.getRegisterPort();
            ((BindingProvider) port).getRequestContext();

            ws.RegInfo info = new ObjectFactory().createRegInfo();
            info.setTerminalId(regId);
            info.setJavaVer(System.getProperty("java.version"));
            info.setOsVer(System.getProperty("os.name"));
            info.setDbVer(SQLAdmin.getInstance().getMySQLVersion());
            info.setAppVer(checkEmpty(SQLAdmin.getInstance().getSystemValue(SQLAdmin.APP_VER), "older than 11.5"));
            info.setName(checkEmpty(SQLAdmin.getInstance().getSystemValue(SQLAdmin.SP_NAME)));
            info.setCity(checkEmpty(SQLAdmin.getInstance().getSystemValue("city")));
            info.setAddress(checkEmpty(SQLAdmin.getInstance().getSystemValue("address")));
            info.setPhone(checkEmpty(SQLAdmin.getInstance().getSystemValue("phone")));
            info.setMhmlk(SQLAdmin.getInstance().getMHMLK());
            info.setBranch(checkEmpty(SQLAdmin.getInstance().getSystemValue(SQLAdmin.SP_BRANCH)));
            info.setAvivId(SQLAdmin.getInstance().getSystemValue(SQLAdmin.AVIV_ID));
            info.setPaydeskCount(SQLAdmin.getInstance().getPaydeskCount());
            info.setTotalSize(SQLAdmin.getInstance().getDataRootTotalSize());
            info.setFreeSize(SQLAdmin.getInstance().getDataRootUsableSize());
            info.setDataSize(SQLAdmin.getInstance().getDataTotalSize());
            info.setVersion(LB_VERSION);
            info.setDBName(SQLAdmin.getInstance().getDbCatalog());
            info.setReplicateSystem(SQLAdmin.getInstance().isReplicateSystem());
            info.setClientType(SQLAdmin.getInstance().getClientType());

            boolean result = false;
            // pingRunLastTime = System.currentTimeMillis();
            try {
                result = port.ping(regId, info);
                pingRunLastTime = System.currentTimeMillis();
            } catch (Throwable ex) {
                util.Logger.log(ex);
            }

            util.Logger.log(" WS was pinged " + result);

            // update interface info
            pingInterfaceInfo(port);
            // don't send terminal info if there is definition to use status (The status is already sent every X time)
            if (!"TRUE".equalsIgnoreCase(LogManager.getInstance().getLogbinParam("STATUS"))) {
                try {
                    result = false;
                    TerminalInfoArray arr;
                    WorkhourInfo w;
                    synchronized (monitor) {
                        arr = createTerminalInfo();
                        w=createWorkhourInfo();
                    }

                    result = port.pingTerminalsWithWH(regId, SQLAdmin.getInstance().getSystemValue(SQLAdmin.AVIV_ID), arr, w);
                } catch (Throwable ex1) {
                    util.Logger.log(ex1);
                }
                util.Logger.log("TerminalInfo was sent " + result);
            }
        } catch (javax.xml.ws.WebServiceException ex) {
            Throwable cause = ex.getCause();
            //util.Logger.error(ex.getMessage() + " " + (cause != null ? "cause :" + cause.getMessage() : ""));
            util.Logger.log(ex);
        } catch (Throwable ex) {
            util.Logger.log(ex);
        }

    }

    private void runTerminalAgent() {
        util.Logger.log("runTerminalAgent");

        Runnable r = new Runnable() {
            public void run() {
                int statusTimeout = 180000;
                String val = getLogbinParam("statusTimeout");
                if (val == null || val.isEmpty()) {
                    val = "180";
                    util.Logger.log("Set default statusTimeout to 180 sec");
                    try {
                        setLogbinParam("statusTimeOut", "180");
                    } catch (SQLException ex1) {
                        util.Logger.log(ex1);
                    }
                    util.Logger.log("The Status Timeout is " + val.trim() + " sec");
                    try {
                        statusTimeout = Integer.parseInt(val.trim()) * 1000;
                    } catch (Exception ex) {
                        util.Logger.log("Illegal statusTimeout value " + val + " it sets up to 180 sec");
                        try {
                            setLogbinParam("statusTimeout", "180");
                        } catch (SQLException ex1) {
                            util.Logger.log(ex1);
                        }
                        statusTimeout = 180000;
                    }
                }

                RegisterService service = null;
                Register port = null;
                while (true) {
                    try {
                        if (service == null || port == null) {
                            Logger.log("Create new RegisterService");
                            service = new RegisterService();
                            port = service.getRegisterPort();
                            ((BindingProvider) port).getRequestContext();

                        }

                        pingTerminals(port);
                    } catch (Throwable ex1) {
                        util.Logger.log(ex1.getMessage());
                        service = null;
                        port = null;
                    }
                    try {
                        Thread.sleep(statusTimeout /*60000*/);
                    } catch (InterruptedException ex) {
                    }

                }
            }
        };
        Thread t = new Thread(r, "BOUpdates");
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    class Synchronizer {
        public synchronized void wakeUp(boolean fl) throws Exception {
            if (fl) {
                notifyAll();
            } else {
                wait();
            }
        }
    }

    private boolean pingTerminals(Register port) {
        util.Logger.log("pingTerminals " + Thread.currentThread().getName());
        TerminalInfoArray arr;
        WorkhourInfo w;
        synchronized (monitor) {
            arr = createTerminalInfo();
            w=createWorkhourInfo();
            util.Logger.log("createTerminalInfo " + Thread.currentThread().getName());
        }
        return port.pingTerminalsWithWH(regId, SQLAdmin.getInstance().getSystemValue(SQLAdmin.AVIV_ID), arr, w);
    }

    private void checkCommands() {
        util.Logger.log("CheckCommands...  ");

        ResultSet rs = null;

        try {
            rs = SQLAdmin.getInstance().executeQuery("SELECT MAX(id) FROM logbin_command ");
            int id = rs.next() ? rs.getInt(1) : 0;
            SQLAdmin.getInstance().closeFullResultSet(rs);
            util.Logger.log("CheckCommands: Call to the Service...  ");
            RegisterService service = new RegisterService();
            Register port = service.getRegisterPort();
            util.Logger.log("CheckCommands: Call to the Service function (getCommand)...  last command was " + id);
            CommandArray ca = port.getCommand(regId, SQLAdmin.getInstance().getSystemValue(SQLAdmin.AVIV_ID), id);
            List<Command> ls = ca.getItem();
            util.Logger.log("Found " + (ls != null ? ls.size() : 0) + "commands");
            if (ls != null && ls.size() > 0) {
                SQLAdmin.getInstance().saveCommands(ls);
                mCommandManager.wakeup();
            }

        } catch (Throwable ex1) {
            util.Logger.log(ex1);
        } finally {
            SQLAdmin.getInstance().closeFullResultSet(rs);

        }
    }

    private boolean migrateToNewServer() {
        boolean fl = false;
        ResultSet rs = null;
        String varName = "UPDATE_NEW_SERVER_VER_15";
        try {
            rs = SQLAdmin.getInstance().executeQuery("SELECT * FROM logbin_defines WHERE id='" + varName + "'");
            if (!(fl = rs.next())) {
                util.Logger.log("Check update for " + varName);
                String fileName = "wrapper_update15.conf";
                String fileServerPath = "http://192.168.55.13/uploads/" + fileName;
                File tmpFile = new File(System.getProperty("user.dir", "."), fileName);

                if ((!tmpFile.exists() || tmpFile.delete())) {
                    if (Utils.download(fileServerPath, tmpFile)) {
                        Utils.exec("net stop AvivLogbinVersionUpdater");
                        try {
                            Thread.sleep(30000);
                        } catch (InterruptedException ex1) {
                            util.Logger.log("net stop AvivLogbinVersionUpdater was interrupted " + ex1.getMessage());
                        }
                        util.Logger.log("net stop AvivLogbinVersionUpdater");

                        String szLocalPath = System.getProperty("user.dir") + "\\..\\versionUpdater\\conf";
                        File oldConf = new File(szLocalPath, "wrapper.conf");

                        // rename olf conf file
                        if (!oldConf.exists() || oldConf.delete()) {
                            if (tmpFile.renameTo(oldConf)) {
                                util.Logger.log("New Updater config file is installed " + oldConf.exists() + " " + oldConf.getAbsolutePath());
                                fl = true;
                                try {
                                    SQLAdmin.getInstance().executeUpdate("INSERT IGNORE INTO logbin_defines (ID,NAME) VALUES ('" + varName + "',concat('Fixed', NOW()))");
                                } catch (SQLException ex) {
                                    util.Logger.error("INSERT IGNORE INTO logbin_defines (ID,NAME) VALUES ('" + varName + "',concat('Fixed', NOW()))");
                                    util.Logger.log(ex);
                                }

                            } else {
                                util.Logger.log("Cant update Updater config file: Cant move the downloaded file " + tmpFile.getAbsolutePath() + " to " + oldConf.getAbsolutePath());
                            }
                        } else {
                            util.Logger.log("Cant update Updater config file: Result !oldConf.exists() || oldConf.delete(): " + !oldConf.exists() + "||" + oldConf.delete());
                        }
                    } else {
                        util.Logger.log("Cant update Updater config file: Cant download file '" + fileServerPath + "'!");
                    }
                } else {
                    util.Logger.log("Cant update Updater config file: Temporary file '" + tmpFile + "' exists and cant delete it!");
                }
            }
        } catch (SQLException ex1) {
            util.Logger.log(ex1);
        } catch (Throwable ex1) {
            util.Logger.log(ex1);
        } finally {
            SQLAdmin.closeFullResultSet(rs);
            util.Logger.log("net start AvivLogbinVersionUpdater");
            if (Utils.exec("net start AvivLogbinVersionUpdater") != 0) {
                util.Logger.error("Cant start AvivLogbinVersionUpdater !");
            }

        }

        return fl;
    }

    private WorkhourInfo createWorkhourInfo() {
        ObjectFactory of = new ObjectFactory();
        WorkhourInfo w = of.createWorkhourInfo();
        // current Z todo add dedicated function for that
        WorkHourInfoLocal whInfo = SQLAdmin.getInstance().getWorkHourInfo(0);
        w.setEmployeeCount(whInfo.getEmployeeCount());
        w.setEmployeeCurrentCount(whInfo.getEmployeeCurrentCount());
        w.setFirstWorkhourEntry(whInfo.getFirstWorkhourEntry());
        w.setWorkhourSum(whInfo.getWorkhourSum());
        return w;
    }

    private TerminalInfoArray createTerminalInfo() {
        ObjectFactory of = new ObjectFactory();
        TerminalInfoArray arr = of.createTerminalInfoArray();
        ResultSet rs = null, rs1 = null, rsDeal = null;
        try {
            rs = SQLAdmin.getInstance().executeQuery("SELECT * FROM bo_terminals");

            while (rs.next()) {
                TerminalInfo info = of.createTerminalInfo();
                int id = rs.getInt("id");
                info.setId(id);
                info.setName(rs.getString("name"));
                info.setIPAddress(rs.getString("ip_address"));
                info.setDealCount(rs.getInt("deal_count"));
                info.setDealSum(rs.getFloat("deal_sum"));
                info.setMemo(rs.getString("memo"));
                info.setStatus(rs.getInt("status"));
                java.sql.Timestamp tm = rs.getTimestamp("status_time");
                info.setStatusTime(tm != null ? tm.getTime() : 0);
                int user = Math.max(0, rs.getInt("id_user"));
                String userName = null;
                if (user > 0) {
                    try {
                        rs1 = SQLAdmin.getInstance().executeQuery("SELECT name FROM employee WHERE id=" + user);
                        if (rs1.next()) {
                            userName = rs1.getString(1);
                        }
                        SQLAdmin.getInstance().closeFullResultSet(rs1);
                    } catch (SQLException ex) {
                        util.Logger.log(ex);
                    }

                }

                rsDeal = SQLAdmin.getInstance().executeQuery("SELECT COUNT(*),SUM(sum),MIN(tm_close) FROM deals WHERE id_z=0 AND paydesk=" + id + " AND tm_close IS NOT NULL");
                if (rsDeal.next()) {
                    info.setDealCount(rsDeal.getInt(1));
                    info.setDealSum(rsDeal.getFloat(2));
                    info.setFirstDealTime(rsDeal.getTimestamp(3) != null ? rsDeal.getTimestamp(3).getTime() : 0);
                    util.Logger.log("Paydesk " + id + " sum=" + rsDeal.getFloat(2) + " count=" + rsDeal.getFloat(1));
                }
                SQLAdmin.closeFullResultSet(rsDeal);

                rsDeal = SQLAdmin.getInstance().executeQuery("SELECT COUNT(*),SUM(deals.sum) FROM tbls LEFT JOIN deals ON id_deal=deals.id WHERE  id_deal!=0 AND id_z=0 AND deals.paydesk=" + id);
                if (rsDeal.next()) {
                    info.setOpenDealCount(rsDeal.getInt(1));
                    info.setOpenDealSum(rsDeal.getFloat(2));
                }
                SQLAdmin.closeFullResultSet(rsDeal);
                rsDeal = SQLAdmin.getInstance().executeQuery("SELECT COUNT(*),SUM(deals.sum) FROM delivery_deals_open LEFT JOIN deals ON delivery_deals_open.id=deals.id WHERE deals.id_z=0 AND deals.paydesk=" + id);
                if (rsDeal.next()) {
                    info.setOpenDealCount(info.getOpenDealCount() + rsDeal.getInt(1));
                    info.setOpenDealSum(info.getOpenDealSum() + rsDeal.getFloat(2));
                }
                SQLAdmin.closeFullResultSet(rsDeal);


                if (userName == null) {
                    userName = "UNKNOWN";
                }

                info.setUserId(user);
                info.setUserName(userName);
                arr.getItem().add(info);

            }
        } catch (SQLException ex1) {
            util.Logger.log(ex1);
        } finally {
            SQLAdmin.closeFullResultSet(rs);
            SQLAdmin.closeFullResultSet(rs1);
            SQLAdmin.closeFullResultSet(rsDeal);
        }
        return arr;
    }

    class KeepAliveTask
            extends TimerTask {
        public void run() {
            ping();
        }
    }

    public void setStatusTick() {
        util.Logger.log("Set Status Tick");
        if (keepAliveTimer != null) {
            keepAliveTimer.cancel();
        }
        keepAliveTimer = new java.util.Timer("KeepAliveTask");
        keepAliveTimer.scheduleAtFixedRate(new KeepAliveTask(), 24 * 60 * 60 * 1000, 24 * 60 * 60 * 1000);
    }

    class CommandTask
            extends TimerTask {
        public void run() {
            checkCommands();
        }
    }

    public void setCommandTaskTimer() {
        util.Logger.log("Set Command Task Timer");
        if (commandTaskTimer != null) {
            commandTaskTimer.cancel();
        }
        commandTaskTimer = new java.util.Timer("CommandTaskTimer");
        commandTaskTimer.scheduleAtFixedRate(new CommandTask(), 10 * 60 * 1000, 60 * 60 * 1000);
    }


    private boolean pingInterfaceInfo(Register port) {
        SQLAdmin.getInstance().reloadData();
        util.Logger.log("Update Interface Info ..." + Thread.currentThread().getName());
        InterfaceInfoArray arr;
        synchronized (monitor) {
            arr = createInterfaceInfo();
            util.Logger.log("createTerminalInfo " + Thread.currentThread().getName());
        }
        return port.updateInterfaceInfo(regId, SQLAdmin.getInstance().getSystemValue(SQLAdmin.AVIV_ID), arr);
    }


    private InterfaceInfoArray createInterfaceInfo() {
        ObjectFactory of = new ObjectFactory();
        InterfaceInfoArray arr = of.createInterfaceInfoArray();
        ResultSet rs = null;
        try {
            rs = SQLAdmin.getInstance().executeQuery("SELECT * FROM sysdefines WHERE name ='BY_DC'");
            InterfaceInfo info = null;
            if (rs.next() && "TRUE".equals(rs.getString("value"))) {
                SQLAdmin.getInstance().closeFullResultSet(rs);
                info = createDCInterfaceInfo(of, LogbinConstants.CIBUS, LogbinConstants.DC_CIBUS, "DC_CIBUS", "Cibus", "", "REST_ID");
                if (info != null) {
                    arr.getItem().add(info);
                }
                info = createDCInterfaceInfo(of, LogbinConstants.TENBIS, LogbinConstants.DC_10BIS, "DC_10BIS", "10Bis", "", "REST_ID");
                if (info != null) {
                    arr.getItem().add(info);
                }
                info = createDCInterfaceInfo(of, LogbinConstants.MULTIPASS, LogbinConstants.DC_MULTIPASS, "DC_MULTIPASS", "Multipass", "", "POS_ID");
                if (info != null) {
                    arr.getItem().add(info);
                }
            }
            if (SQLAdmin.getInstance().isTableExists("lc_deals")) {
                info = createLCInfo(of, LogbinConstants.BUSINESSLOGIC, LogbinConstants.LC_BUSINESSLOGIC, "BusinessLogic", "", "USER");
                if (info != null) {
                    arr.getItem().add(info);
                }
                info = createLCInfo(of, LogbinConstants.VALUECARD, LogbinConstants.LC_VALUECARD, "ValueCard", "", "POSID");
                if (info != null) {
                    arr.getItem().add(info);
                }
            }
            info = createAccountingInfo(of);
            if (info != null) {
                arr.getItem().add(info);
            }

            info = createChequeInfo(of);
            if (info != null) {
                arr.getItem().add(info);
            }


            info = createHOInfo(of);
            if (info != null) {
                arr.getItem().add(info);
            }

            SQLAdmin.getInstance().closeFullResultSet(rs);
            rs = SQLAdmin.getInstance().executeQuery("SELECT * FROM dmplugins WHERE id=43 AND jar LIKE 'RemoteDeliveryPlugin%'");
            if (rs.next()) {
                info = createRDInterfaceInfo(of, LogbinConstants.OPEN_REST, LogbinConstants.RD_OPEN_REST, "RD_OPEN_REST", "OpenRest", "", "USERNAME", "REST_ID");
                if (info != null) {
                    arr.getItem().add(info);
                }

                info = createRDInterfaceInfo(of, LogbinConstants.IORDER, LogbinConstants.RD_IORDER, "RD_IORDER", "IOrder", "", "POS_ID", "REST_ID");
                if (info != null) {
                    arr.getItem().add(info);
                }

                info = createRDInterfaceInfo(of, LogbinConstants.PREGO, LogbinConstants.RD_PREGO, "RD_PREGO", "Prego", "", "POS_ID", "REST_ID");
                if (info != null) {
                    arr.getItem().add(info);
                }

                info = createRDInterfaceInfo(of, LogbinConstants.MISHLOHA, LogbinConstants.RD_MISHLOHA, "RD_MISHLOHA", "Mishloha", "", "POS_ID", "REST_ID");
                if (info != null) {
                    arr.getItem().add(info);
                }
            }
        } catch (SQLException ex1) {
            util.Logger.log(ex1);
        } finally {
            SQLAdmin.closeFullResultSet(rs);
        }
        return arr;
    }

    private InterfaceInfo createHOInfo(ObjectFactory of) { // china town
        InterfaceInfo info = null;
        ResultSet rs = null;
        try {
            rs = SQLAdmin.getInstance().executeQuery("SELECT * FROM dmplugins WHERE id=39 ");
            if (rs.next() && ("HOPlugin.jar".equalsIgnoreCase(rs.getString("jar")) || "HOPlugin".equalsIgnoreCase(rs.getString("jar")))) {
                info = of.createInterfaceInfo();
                info.setType(LogbinConstants.HO_PLUGIN);
                info.setName("\u05de\u05e8\u05db\u05d6 \u05e8\u05e9\u05ea");
                Timestamp tmCreate = rs.getTimestamp("create_date");
                info.setTmCreate(tmCreate != null ? tmCreate.getTime() : 0);
                info.setParams(rs.getString("data"));
                info.setMemo("");
            }
        } catch (SQLException e) {
            Logger.log(e);
        } finally {
            SQLAdmin.closeFullResultSet(rs);
        }
        return info;
    }

    private InterfaceInfo createAccountingInfo(ObjectFactory of) {
        InterfaceInfo info = null;
        ResultSet rs = null;
        try {
            rs = SQLAdmin.getInstance().executeQuery("SELECT * FROM sysdefines WHERE name='ACCOUNTING_INTERFACE'");
            if (rs.next() && "TRUE".equalsIgnoreCase(rs.getString("value"))) {
                info = of.createInterfaceInfo();
                info.setType(LogbinConstants.ACCOUNTING);
                info.setName("\u05d7\u05e9\u05d1\u05e9\u05d1\u05ea");
                info.setTmCreate(0);
                info.setParams("");
                SQLAdmin.getInstance().closeFullResultSet(rs);
                rs = SQLAdmin.getInstance().executeQuery("SELECT * FROM msg_recipient_map where type=6");
                info.setMemo(rs.next() ? "נשלח בדוא\"ל" : "");
            }
        } catch (SQLException e) {
            Logger.log(e);
        } finally {
            SQLAdmin.closeFullResultSet(rs);
        }
        return info;
    }

    private InterfaceInfo createChequeInfo(ObjectFactory of) {
        InterfaceInfo info = null;
        ResultSet rs = null;
        try {
            rs = SQLAdmin.getInstance().executeQuery("SELECT * FROM sysdefines WHERE name LIKE 'CHEQUE_COMPANY'");
            if (rs.next() && !"NONE".equalsIgnoreCase(rs.getString("value"))) {
                String value = rs.getString("value");
                String path = SQLAdmin.getInstance().getSystemValue(SQLAdmin.HOME_PATH);
                if (path != null) {
                    File f = new File(path);
                    if (!f.isDirectory()) {
                        f = f.getParentFile();
                    }
                    String szFileName = "Ern_Box";
                    f = new File(f, szFileName);
                    if (!f.exists()) {
                        // if the ern_box is not in the databrowse then may be in the d:/aviv
                        f = new File(path).getParentFile();
                        f = new File(f, szFileName);
                    }
                    if (f.exists() && f.isDirectory()) {
                        info = of.createInterfaceInfo();
                        info.setType(LogbinConstants.CHEQUE_INTERFACE);
                        info.setName("בדיקת צקים");
                        info.setTmCreate(0);
                        info.setParams(value);
                        info.setMemo(f.getAbsolutePath());

                    }
                }
            }
        } catch (Exception e) {
            Logger.log(e);
        } finally {
            SQLAdmin.closeFullResultSet(rs);
        }
        return info;
    }


    private InterfaceInfo createLCInfo(ObjectFactory of, int type, int lcType, String name, String memo, String... params) {
        InterfaceInfo info = null;
        ResultSet rs = null;
        try {
            rs = SQLAdmin.getInstance().executeQuery("SELECT * FROM lc_defines WHERE type=" + lcType);
            if (rs.next()) {
                info = of.createInterfaceInfo();
                info.setType(type);
                info.setName(name);
                SQLAdmin.getInstance().closeFullResultSet(rs);
                rs = SQLAdmin.getInstance().executeQuery("SELECT MIN(tm_create) FROM lc_deals WHERE provider_id=0 OR provider_id=" + lcType);
                Timestamp tmCreate = rs.next() ? rs.getTimestamp(1) : null;
                info.setTmCreate(tmCreate != null ? tmCreate.getTime() : 0);
                String param = "";
                for (int i = 0; i < params.length; i++) {
                    if (!param.isEmpty()) {
                        param += ";";
                    }

                    SQLAdmin.getInstance().closeFullResultSet(rs);
                    rs = SQLAdmin.getInstance().executeQuery("SELECT * FROM lc_defines WHERE type=" + lcType + " AND id='" + params[i] + "'");
                    if (rs.next()) {
                        param += params[i] + "=" + rs.getString("value");
                    }
                }
                info.setParams(param);
                info.setMemo(memo);
            }
        } catch (SQLException e) {
            Logger.log(e);
        } finally {
            SQLAdmin.closeFullResultSet(rs);
        }
        return info;
    }

    private InterfaceInfo createDCInterfaceInfo(ObjectFactory of, int type, int dcType, String sysdefinesParam, String name, String memo, String... params) throws SQLException {
        ResultSet rs = null;
        InterfaceInfo info = null;
        try {
            rs = SQLAdmin.getInstance().executeQuery("SELECT * FROM sysdefines WHERE name ='" + sysdefinesParam + "'");
            if (rs.next() && "TRUE".equals(rs.getString("value"))) {
                info = of.createInterfaceInfo();
                info.setType(type);
                info.setName(name);
                SQLAdmin.getInstance().closeFullResultSet(rs);
                rs = SQLAdmin.getInstance().executeQuery("SELECT MIN(date_pay) FROM dc_deals WHERE type=" + dcType);
                Timestamp tmCreate = rs.next() ? rs.getTimestamp(1) : null;
                info.setTmCreate(tmCreate != null ? tmCreate.getTime() : 0);
                String param = "";
                for (int i = 0; i < params.length; i++) {
                    if (!param.isEmpty()) {
                        param += ";";
                    }

                    SQLAdmin.getInstance().closeFullResultSet(rs);
                    rs = SQLAdmin.getInstance().executeQuery("SELECT * FROM dc_defines WHERE type=" + dcType + " AND id='" + params[i] + "'");
                    if (rs.next()) {
                        param += params[i] + "=" + rs.getString("value");
                    }
                }
                info.setParams(param);
                info.setMemo(memo);

            }
        } finally {
            SQLAdmin.getInstance().closeFullResultSet(rs);
        }
        return info;
    }

    private InterfaceInfo createRDInterfaceInfo(ObjectFactory of, int type, int rdType, String sysdefinesParam, String name, String memo, String... params) throws SQLException {
        ResultSet rs = null;
        InterfaceInfo info = null;
        try {
            rs = SQLAdmin.getInstance().executeQuery("SELECT * FROM sysdefines WHERE name ='" + sysdefinesParam + "'");
            if (rs.next() && "TRUE".equals(rs.getString("value"))) {
                info = of.createInterfaceInfo();
                info.setType(type);
                info.setName(name);
                SQLAdmin.getInstance().closeFullResultSet(rs);
                rs = SQLAdmin.getInstance().executeQuery("SELECT MIN(tm_open) FROM delivery_deals WHERE remote_vendor=" + rdType);
                Timestamp tmCreate = rs.next() ? rs.getTimestamp(1) : null;
                info.setTmCreate(tmCreate != null ? tmCreate.getTime() : 0);
                String param = "";
                String query = "SELECT * FROM rd_defines WHERE type=" + rdType;
                if (params.length > 0) {
                    if (!"*".equals(params[0])) {
                        for (int i = 0; i < params.length; i++) {
                            if (i == 0) {
                                query += " AND id IN (";
                            } else {
                                query += ", ";
                            }
                            query += "'" + params[i] + "'";
                        }
                        query += ")";
                    }
                    SQLAdmin.getInstance().closeFullResultSet(rs);
                    rs = SQLAdmin.getInstance().executeQuery(query);
                    while (rs.next()) {
                        if (!param.isEmpty()) {
                            param += ";";
                        }
                        param += rs.getString("id") + "=" + rs.getString("value");
                    }
                }
                info.setParams(param);
                info.setMemo(memo);

            }
        } finally {
            SQLAdmin.getInstance().closeFullResultSet(rs);
        }
        return info;
    }

}
