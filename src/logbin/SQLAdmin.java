package logbin;

import java.io.*;
import java.sql.*;
import java.util.*;
import javax.xml.parsers.*;

import org.w3c.dom.*;
import util.*;

import ws.*;

public class SQLAdmin {
    public static final String DB_DRIVER = "dbDriver";
    public static final String DB_USER = "dbUser";
    public static final String DB_URL = "dbURL";
    public static final String DB_PWD = "dbPswd";
    public static final String DM_USER = "dmUser";
    public static final String DM_URL = "dmURL";
    public static final String DM_PWD = "dmPswd";
    public static final String DB_BACKUP_URL = "dbBackupURL";
    public static final String DB_LOCAL_URL = "dbLocalURL";
    public static final String DB_ENCODING = "characterEncoding";
    public static final String HOME_PATH = "home_path";
    public static final String SP_SERVER_HOSTNAME = "serverHostname";
    public static final String SP_BRANCH = "branch";
    public static final String SP_BANK_BRANCH = "b_branch";
    public static final String SP_BANK_ACCOUNT = "b_account";
    public static final String SP_BANK_CHEQUE_CASH_DELAY = "b_cheque_delay";
    public static final String SP_BANK_ID = "b_bank";
    public static final String SP_OWNER = "owner";
    public static final String CLIENT_ID = "client_id";
    public static final String VALID = "valid";
    public static final String SYSTEM_PWD = "SystemPWD";
    public static final String SP_NAME = "name";
    public static final String MHMLK = "MHMLK";
    public static final String APP_VER = "app_ver";
    public static final String AVIV_ID = "aviv_id";

    private Connection connection;
    private DatabaseMetaData dbMetaData;
    private String dbCatalog;
    private HashMap<String, String> mMap = new HashMap<String, String>();
    private SrLock mSrLock;
    private static SQLAdmin mInstance;

    private SQLAdmin() {
        try {
            init();
            getConnection();
        } catch (Throwable ex) {
            ex.printStackTrace();
            System.exit(1);

        }
    }

    public static SQLAdmin getInstance() {
        if (mInstance == null) {
            mInstance = new SQLAdmin();
        }
        return mInstance;
    }

    public Connection getConnection() {
        try {
            while (connection == null || !connection.isValid(500)) {
                util.Logger.log(connection == null ? "Create connection " : "Lost connection. Try reconnect...");
                if (!createMainConnectionInstance()) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ex1) {
                    }
                    util.Logger.log("Connection was created successfully !");
                }
            }
        } catch (SQLException ex) {
            util.Logger.log(ex);
            System.exit(2);
        }
        return connection;
    }

    public String getDbCatalog() {
        return dbCatalog;
    }

    public String getSystemValue(String key) {
        return mMap.get(key.toLowerCase());
    }

    public String getMHMLK() {
        String mhmlk = getSystemValue(MHMLK);
        return mhmlk != null ? mhmlk : "";
    }

    public boolean createMainConnectionInstance() {
        boolean fl = false;
        String encoding = getSystemValue(DB_ENCODING);
        String szURL = getSystemValue(DB_URL) +
                (encoding != null && encoding.trim().length() > 0 ?
                        "&" + encoding : "");

        try {
            connection = createConnectionInstance(szURL);
            dbCatalog = connection.getCatalog();
            dbMetaData = connection.getMetaData();
            loadParams();
            fl = true;
        } catch (ClassNotFoundException ex) {
            util.Logger.log(ex);
        } catch (SQLException ex) {
            util.Logger.log(ex);
        }
        return fl;
    }

    private void loadParams() {
        ResultSet rs = null;
        try {
            rs = executeQuery("SELECT * FROM client_res");
            while (rs.next()) {
                mMap.put(rs.getString("id").toLowerCase(), rs.getString("name"));
            }
        } catch (SQLException ex1) {
            util.Logger.log(ex1);
        } finally {
            this.closeFullResultSet(rs);
        }
        try {
            rs = executeQuery("SELECT id,max(0+sub_id) sb FROM dbversion GROUP BY id");
            while (rs.next()) {
                mMap.put(APP_VER, rs.getString("id") /*+ "/" + rs.getString("sb")*/);
            }
        } catch (SQLException ex1) {
            util.Logger.log(ex1);
        } finally {
            this.closeFullResultSet(rs);
        }

    }

    private Connection createConnectionInstance(String szURL) throws
            SQLException, ClassNotFoundException {
        Connection aConnection = null;
        Class.forName(getSystemValue(DB_DRIVER));
        Logger.log(getSystemValue("HOME_PATH"));
        Logger.log(szURL);
        Properties p = new Properties();
        p.setProperty("user", getSystemValue(DB_USER));
        p.setProperty("password", getSystemValue(DB_PWD));
        p.setProperty("zeroDateTimeBehavior", "convertToNull");
        try {
            aConnection = DriverManager.getConnection(szURL, p);
        } catch (SQLException ex) {
            util.Logger.log(ex.getMessage() + " Cant get connection to " + szURL + ". Try to reload pedit and reconnect.");
            reloadSrlock();
            String encoding = getSystemValue(DB_ENCODING);
            szURL = getSystemValue(DB_URL) +
                    (encoding != null && encoding.trim().length() > 0 ?
                            "&" + encoding : "");

            p.setProperty("user", getSystemValue(DB_USER));
            p.setProperty("password", getSystemValue(DB_PWD));
            p.setProperty("zeroDateTimeBehavior", "convertToNull");
            try {
                aConnection = DriverManager.getConnection(szURL, p);
                loadParams();
            } catch (SQLException ex3) {
                throw ex3;
            }
        }
        return aConnection;
    }

    public String getUser() {
        return getSystemValue(DB_USER);
    }

    public String getPassword() {
        return getSystemValue(DB_PWD);
    }

    private void init() throws Exception {
        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().
                newDocumentBuilder();
        String iniPath = System.getProperty("ROMINI");
        if (iniPath == null) {
            throw new Exception("Init path not found");
        }
        String str = iniPath + System.getProperty("file.separator")
                + "restini.xml";
        File iniFile = new File(str);
        if (iniFile.exists()) {
            util.Logger.log("Ini path " + iniFile.getAbsolutePath());
            Document doc = docBuilder.parse(iniFile);
            Element root = doc.getDocumentElement();
            root.normalize();
            NodeList nl = root.getElementsByTagName("parameter");
            int size = nl.getLength();
            for (int i = 0; i < size; i++) {
                Element el = (Element) nl.item(i);
                mMap.put(el.getAttribute("id").toLowerCase(),
                        el.getAttribute("value"));
            }
            try {
                boolean fl;
                do {
                    fl=checkSec(getSystemValue(HOME_PATH));
                    if (!fl) {
                        // in case of the network(router) problem try later
                        long timeout=2*60*60*1000;
                        util.Logger.error("CheckSec fault: Sleep for 2 hours( try later )");
                        try {
                            Thread.sleep(timeout);
                        }catch(InterruptedException ex){}

                        //System.exit(0);
                    }
                } while (!fl);
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(0);
            }
        } else {
            util.Logger.error("Ini file " + iniFile.getAbsolutePath() + " doesnt exist !");
            System.exit(22);
        }
    }

    private boolean checkSec(String szHomePath) throws Exception {
        util.Logger.log("HomePath: " + szHomePath);
        if (szHomePath == null) {
            util.Logger.error("home_path not defined...");
            return false;
        }
        boolean fl = true;
        String szFileName = "lib" + File.separatorChar + "JK12.lib";
        File f = new File(szHomePath);
        if (!f.isDirectory()) {
            f = f.getParentFile();
        }
        f = new File(f, szFileName);
        if (f.exists()) {
            String szValidTo = null;
            mSrLock = new SrLock(f, "");
            mSrLock.setPwd("xxz0100".getBytes());
            if (!mSrLock.checkPwd()) {
                return false;
            }
            reloadData();
        } else {
            util.Logger.error("file " + szFileName + " doesnt exist:" +
                    f.getAbsolutePath());
        }
        return fl;
    }

    public void reloadData() {
        reloadSrlock();
        loadParams();
    }

    private void reloadSrlock() {
        if (mSrLock != null) {
            try {
                char[] nb = new char[50];
                char[] rb = new char[250];
                mSrLock.f.seek(20);
                long length = mSrLock.f.length();
                while (mSrLock.f.getFilePointer() < length) {
                    for (int i = 0; i < nb.length; ++i) {
                        nb[i] = mSrLock.f.readChar();
                    }
                    String n = mSrLock.decode(String.valueOf(nb), mSrLock.key);
                    if (n != null) {
                        for (int i = 0; i < rb.length; ++i) {
                            rb[i] = mSrLock.f.readChar();
                        }
                        String val = mSrLock.decode(String.valueOf(rb), mSrLock.key);
                        if ((val != null) && !n.equals("")) {
                            mMap.put(n.toLowerCase(), val);
                        }
                    } else {
                        break;
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            util.Logger.error("Cant reload parameters: Srlock is null");
        }
    }

    public boolean isTableExists(String szTable) throws SQLException {
        ResultSet rs = null;
        boolean fl = false;
        if (dbMetaData != null) {
            try {
                rs = connection.createStatement().executeQuery("SHOW TABLES LIKE '" + szTable + "'"); // dbMetaData.getTables(dbCatalog, null, szTable, null);
//                 rs =  dbMetaData.getTables(dbCatalog, null, szTable, null);
                fl = rs.next();

            } catch (SQLException ex) {
                throw ex;
            } finally {
                closeFullResultSet(rs);
            }
        }
        return fl;
    }

    public static void closeFullResultSet(ResultSet rs) {
        if (rs != null) {
            try {
                boolean isCloseStatetment = true;
                try {
                    isCloseStatetment = !rs.isClosed();
                } catch (Error ex1) {
                    isCloseStatetment = false; // connector 3
                }
                if (isCloseStatetment) {
                    Statement st = rs.getStatement();
                    if (st != null && !st.isClosed() && !(st instanceof PreparedStatement) /* !st.isPoolable()*/) {
                        st.close();
                    }
                    rs.close();
                } else {
                    rs.close();
                }
            } catch (SQLException ex) {
                util.Logger.log(ex);
            }
        }
    }

    public boolean execute(String szQuery) throws SQLException {
        Statement st = getConnection().createStatement();
        try {
            return st.execute(szQuery);
        } finally {
            closeStatement(st);
        }
    }

    public ResultSet executeQuery(String szQuery) throws SQLException {
        return getConnection().createStatement().executeQuery(szQuery);
    }

    public int executeUpdate(String szQuery) throws SQLException {
        Statement st = getConnection().createStatement();
        try {
            return st.executeUpdate(szQuery);
        } catch (SQLException ex) {
            util.Logger.error(szQuery);
            throw ex;
        } finally {
            closeStatement(st);
        }
    }

    public long executeAutoIncrementUpdate(String szQuery) throws SQLException {
        Statement st = getConnection().createStatement();
        try {
            st.executeUpdate(szQuery, Statement.RETURN_GENERATED_KEYS);
            ResultSet rs = st.getGeneratedKeys();
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException ex) {
            util.Logger.error(szQuery);
            throw ex;
        } finally {
            closeStatement(st);
        }
        return -1;
    }

    public static void closeStatement(Statement st) {
        try {
            if (st != null) {
                st.close();
            }
        } catch (SQLException ex) {
            /**@todo write to log file*/
            util.Logger.log(ex);
        }
    }

    public String getStringParam(String key, String defaultValue) {
        ResultSet rs = null;
        String res = null;
        try {
            rs = executeQuery("SELECT * FROM sysdefines WHERE name='" + key + "'");
            res = rs.next() ? rs.getString("value") : null;
        } catch (SQLException ex) {
            util.Logger.log(ex);
            res = null;
        } finally {
            this.closeFullResultSet(rs);
        }
        if (res == null) {
            util.Logger.log(key + " is not defined and setup " + defaultValue);
            key = defaultValue;
        }
        return res;
    }

    public boolean isReplicateSystem() {
        ResultSet rs = null;
        boolean res = false;
        try {
            rs = executeQuery("SELECT * FROM sysdefines WHERE name='IS_REPLICATOR' AND value='TRUE'");
            res = rs.next();

            if (!res) {
                closeFullResultSet(rs);
                rs = executeQuery("SELECT * FROM sysdefines_client WHERE name='IS_REPLICATOR' AND value='TRUE'");
                res = rs.next();
            }
        } catch (SQLException ex) {
            util.Logger.log(ex);
            res = false;
        } finally {
            this.closeFullResultSet(rs);
        }

        return res;
    }


    public int getClientType() {
        ResultSet rs = null;
        int type = LogbinConstants.TYPE_REST;
        try {
            rs = executeQuery("SELECT value FROM sysdefines WHERE name='IS_REST'");
            if (rs.next() && "TRUE".equals(rs.getString(1))) {
                closeFullResultSet(rs);
                rs = executeQuery("SELECT id FROM tbl_deals LIMIT 1");
                type = rs.next() ? LogbinConstants.TYPE_REST : LogbinConstants.TYPE_MIX;
            } else {
                type = LogbinConstants.TYPE_MARKET;
            }
        } catch (SQLException ex) {
            util.Logger.log(ex);
        } finally {
            this.closeFullResultSet(rs);
        }

        return type;
    }

    public int getPaydeskCount() {
        ResultSet rs = null;
        int res = 0;
        try {
            rs = executeQuery("SELECT COUNT(*) FROM bo_terminals");
            res = rs.next() ? rs.getInt(1) : null;
        } catch (SQLException ex) {
            util.Logger.log(ex);
            res = 0;
        }
        if (res == 0) {
            util.Logger.error("Cant get terminal count");
        }
        return res;
    }

    public String getPath(String keyPath) throws IllegalArgumentException {
        return getPath(keyPath, true, null);
    }

    public String getPath(String keyPath, boolean isRelative) throws IllegalArgumentException {
        return getPath(keyPath, isRelative, null);
    }

    public String getPath(String keyPath, String extension) throws IllegalArgumentException {
        return getPath(keyPath, true, extension);
    }

    public String getPath(String keyPath, boolean isRelative, String extension) throws IllegalArgumentException {
        String pathParam = getStringParam(keyPath, null);
        if (pathParam == null) {
            util.Logger.error(keyPath + " is not defined!");
        }
        String[] arr = pathParam != null ? pathParam.split("[;,]") : new String[]{};
        String result = null;
        for (String path : arr) {
            String res = path != null ? isRelative ? new File(getSystemValue("HOME_PATH"), path).getAbsolutePath() : path : null;
            if (res != null) {
                File f = new File(res);
                if (f.exists()) {
                    res = res.replaceAll("\\\\", "/");
                } else {
                    util.Logger.error("File " + res + " doesnt exist. Try to check an extension " + extension);
                    if (extension != null) {
                        if (res.indexOf(extension) < 0) {
                            res += extension;
                        }
                        f = new File(res);
                        if (f.exists()) {
                            res = res.replaceAll("\\\\", "/");
                            util.Logger.error("Ok. Now file " + res + " exists.");
                        } else {
                            util.Logger.error("File " + res + " doesnt exist.");
                            res = null;
                        }
                    }
                }
            }
            if (res != null) {
                result = result == null ? res : (result + ";" + res);
            }
        }
        return result;
    }

    public String getMySQLVersion() {
        String ver = null;
        ResultSet rs = null;
        try {
            rs = executeQuery("show variables like \"version\"");
            ver = rs.next() ? rs.getString("value") : null;
        } catch (SQLException ex) {
            util.Logger.log(ex);
        } finally {
            closeFullResultSet(rs);
        }
        return ver;
    }

    public String getMySQLPort() {
        return getVariableValue("port");
    }


    public long getPathTotalSize(String path) {
        File f = new File(path);
        return f.exists() ? f.getParentFile() == null ? f.getTotalSpace() : getDirSize(f) : 0;
    }

    public long getPathUsableSize(String path) {
        File f = new File(path);
        return f.exists() ? f.getUsableSpace() : 0;
    }

    public String formatSize(long size) {
        String res = null;
        if (size > 0) {
            res = String.valueOf(size);
        }
        return res != null ? res : "0";
    }

    public long getDirSize(File dir) {
        long size = 0;
        if (dir.isFile()) {
            size = dir.length();
        } else {
            File[] subFiles = dir.listFiles();
            if (subFiles != null) {
                for (File file : subFiles) {
                    if (file.isFile()) {
                        size += file.length();
                    } else {
                        size += this.getDirSize(file);
                    }
                }
            }
        }

        return size;
    }

    public String getDataDir() {
        return getVariableValue("datadir");
    }

    public String getRoot(String path) {
        File f = path != null ? new File(path) : null;
        return f != null && f.exists() ? f.getParentFile() != null ? getRoot(f.getParent()) : f.getAbsolutePath() : null;
    }

    public String getDataRootTotalSize() {
        String root = getRoot(getDataDir());
        return root != null ? formatSize(getPathTotalSize(root)) : "0";
    }

    public String getDataRootUsableSize() {
        String root = getRoot(getDataDir());
        return root != null ? formatSize(getPathUsableSize(root)) : "0";
    }

    public String getDataTotalSize() {
        String root = getDataDir();
        return root != null ? formatSize(getPathTotalSize(root)) : "0";
    }

    public String getVariableValue(String name) {
        String path = null;
        ResultSet rs = null;
        try {
            rs = executeQuery("show variables like \"" + name + "\"");
            path = rs.next() ? rs.getString("value") : null;
        } catch (SQLException ex) {
            util.Logger.log(ex);
        } finally {
            closeFullResultSet(rs);
        }
        return path;
    }

    public int setSystemValue(String key, String value, boolean encoded) {
        int res = LogManager.STATUS_OK;
        mMap.put(key.toLowerCase(), value);
        if (encoded) {
            mSrLock.setParam(key, value);
        } else {
            try {
                executeUpdate("REPLACE INTO client_res (id,name) VALUES (\"" + key + "\",\"" + value + "\")");
            } catch (SQLException ex) {
                res = LogManager.STATUS_SQL_ERROR;
                util.Logger.log(ex);
            }
        }
        return res;
    }

    public Collection<Command> getUnexecutableCommands() {
        ArrayList<Command> arr = new ArrayList();
        ResultSet rs = null;
        try {
            rs = this.executeQuery("SELECT * FROM logbin_command WHERE status IS NULL");
            while (rs.next()) {
                Command c = new Command();
                c.setId(rs.getInt("id"));
                c.setType(rs.getInt("type"));
                c.setDependencyId(rs.getInt("dependency_id"));
                Timestamp tm = rs.getTimestamp("tm_exec");
                c.setExecuteTime(tm != null ? tm.getTime() : 0);
                c.setInteractive(rs.getBoolean("isinteractive"));
                c.setMemo(rs.getString("memo"));
                c.setPropertyName(rs.getString("prop_name"));
                c.setPropertyValue(rs.getString("prop_value"));
                arr.add(c);
            }
        } catch (SQLException ex) {
            util.Logger.log(ex);
        } finally {
            this.closeFullResultSet(rs);
        }

        return arr;
    }

    public void saveCommands(Collection<Command> arr) {
        PreparedStatement prst = null;
        try {
            prst = getConnection().prepareStatement(
                    "INSERT IGNORE INTO logbin_command (id, type, prop_name, prop_value, tm_exec, isinteractive, memo, dependency_id)" +
                            " VALUES (?,?,?,?,?,?,?,?)");
            for (Iterator<Command> iter = arr.iterator(); iter.hasNext(); ) {
                Command c = iter.next();
                util.Logger.log(c.getId() + "  " + c.getMemo());
                prst.setInt(1, c.getId());
                prst.setInt(2, c.getType());
                prst.setString(3, c.getPropertyName());
                prst.setString(4, c.getPropertyValue());
                prst.setTimestamp(5, new java.sql.Timestamp(c.getExecuteTime()));
                prst.setBoolean(6, c.isInteractive());
                prst.setString(7, c.getMemo() != null ? c.getMemo() : "");
                prst.setInt(8, c.getDependencyId());
                prst.executeUpdate();
            }
        } catch (SQLException ex) {
            util.Logger.log(ex);
        } finally {
            this.closeStatement(prst);
        }
    }

    public boolean updateCommandStatus(int id, int status) {
        boolean fl = true;
        try {
            this.executeUpdate("UPDATE logbin_command SET status=" + status + ", tm_status=NOW() WHERE id=" + id);
        } catch (SQLException ex) {
            util.Logger.log(ex);
            fl = false;
        }
        return fl;
    }

    public void updateCommandAck(int id, boolean fl) {
        try {
            this.executeUpdate("UPDATE logbin_command SET ack=" + (fl ? 1 : 0) + "  WHERE id=" + id);
        } catch (SQLException ex) {
            util.Logger.log(ex);
        }
    }

    public int updateSysdefines(String name, String value) {
        int res = validateSysdefinesValue(name, value);
        if (res == LogManager.STATUS_OK) {
            try {
                this.executeUpdate("UPDATE sysdefines SET VALUE='" + value + "' WHERE name='" + name.toUpperCase() + "'");
                if (isReplicatedSystem()) {
                    Logger.log("Update offline terminals");
                    try {
                        long key = executeAutoIncrementUpdate("INSERT INTO bo_public(id, type, is_delta, date_update, data_type, value) VALUES (0, 'sysdefines', 1, NOW(), 1, 'update#" + name.toUpperCase() + "')");
                        executeUpdate("INSERT INTO bo_public_replicate_info (id, data_type, value, tm_create, terminal_create, tm_update, err_code, memo)" +
                                " SELECT " + key + ",1, type, date_update, 99, NULL,NULL,'Logbin' FROM bo_public WHERE id=" + key);
                    } catch (Exception ex) {
                        Logger.log(ex);
                    }
                }
            } catch (SQLException ex) {
                util.Logger.log(ex);
                res = LogManager.STATUS_SQL_ERROR;
            }
        }
        return res;
    }

    public void publicUpdates(String tbl, String[] pkValues) {
        Logger.log("Update offline terminals");
        try {
            String strPk = pkValues.length > 0 ? "update" : "";
            for (int i = 0; i < pkValues.length; i++) {
                strPk += "#" + pkValues[i];
            }
            long key = executeAutoIncrementUpdate("INSERT INTO bo_public(id, type, is_delta, date_update, data_type, value) VALUES (0, '" + tbl + "', 1, NOW(), 1, '" + strPk + "')");
            executeUpdate("INSERT INTO bo_public_replicate_info (id, data_type, value, tm_create, terminal_create, tm_update, err_code, memo)" +
                    " SELECT " + key + ",1, type, date_update, 99, NULL,NULL,'Logbin' FROM bo_public WHERE id=" + key);
        } catch (Exception ex) {
            Logger.log(ex);
        }

    }

    public boolean isReplicatedSystem() {
        boolean fl = false;
        ResultSet rs = null;
        try {
            rs = executeQuery("SELECT value FROM sysdefines_client WHERE name='IS_REPLICATOR'");
            fl = rs.next() && "TRUE".equalsIgnoreCase(rs.getString("value"));
            if (!fl) {
                closeFullResultSet(rs);
                rs = executeQuery("SELECT value FROM sysdefines WHERE name='IS_REPLICATOR'");
                fl = rs.next() && "TRUE".equalsIgnoreCase(rs.getString("value"));
            }
        } catch (Exception ex) {
            Logger.log(ex);
        } finally {
            closeFullResultSet(rs);
        }
        return fl;
    }

    private int validateSysdefinesValue(String name, String szValue) {
        int result = LogManager.STATUS_OK;
        ResultSet rs = null;
        try {
            rs = this.executeQuery("SELECT java_class,factor FROM sysdefines WHERE name='" + name + "'");

            Class[] arg = {String.class};
            if (rs.next()) {

                Class clazz = Class.forName(rs.getString("java_class"));
                int factor = rs.getInt("factor");

                Object value;
                if (factor == 1) {
                    value = clazz.getField(szValue).get(null);
                } else {
                    value = clazz.getConstructor(arg).newInstance(new
                            Object[]{
                            szValue});

                }

            }
        } catch (NoSuchMethodException ex) {
            util.Logger.log(ex);
            result = LogManager.STATUS_INVALID;
        } catch (NoSuchFieldException ex) {
            util.Logger.log(ex);
            result = LogManager.STATUS_NO_SUCH_FIELD;
        } catch (SQLException ex) {
            util.Logger.log(ex);
            result = LogManager.STATUS_SQL_ERROR;
        } catch (Throwable ex) {
            util.Logger.log(ex);
            result = LogManager.STATUS_UPDATE_ERROR;
        }
        return result;
    }

    public WorkHourInfoLocal getWorkHourInfo(int z) {
        WorkHourInfoLocal res = new WorkHourInfoLocal();
        ResultSet rs = null;
        try {
            Timestamp prevZTime = null;
            rs = executeQuery("SELECT date FROM z_report WHERE id IN (SELECT MAX(id) FROM z_report)");
            if (rs.next()) {
                prevZTime = rs.getTimestamp(1);
            }
            closeFullResultSet(rs);
            long zTime = prevZTime == null ? 0 : prevZTime.getTime();

            rs = executeQuery("SELECT * FROM workhour WHERE enter_time>(SELECT date FROM z_report WHERE id IN (SELECT MAX(id) FROM z_report))");
            Timestamp first = null;
            long sum = 0;
            long now = System.currentTimeMillis();
            while (rs.next()) {
                Timestamp enterTime = rs.getTimestamp("enter_time");
                Timestamp exitTime = rs.getTimestamp("exit_time");
                sum += enterTime != null ? exitTime != null ? exitTime.getTime() - enterTime.getTime() : now - enterTime.getTime() : exitTime != null ? exitTime.getTime() - zTime : 0;
                res.setEmployeeCount(res.getEmployeeCount() + 1);
                if (exitTime == null) {
                    res.setEmployeeCurrentCount(res.getEmployeeCurrentCount() + 1);
                }
                if (enterTime != null) {
                    if (first == null) {
                        first = enterTime;
                    } else {
                        first = first.after(enterTime) ? enterTime : first;
                    }
                }

            }
            res.setFirstWorkhourEntry(first != null ? first.getTime() : 0);
            res.setWorkhourSum(sum);
        } catch (SQLException e) {
            Logger.log(e);
        } finally {
            closeFullResultSet(rs);
        }
        return res;
    }
}
