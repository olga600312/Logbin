package logbin;

import util.Logger;
import util.UnzipUtility;
import ws.*;

import java.util.*;
import java.sql.*;

import org.w3c.dom.Document;

import javax.xml.parsers.FactoryConfigurationError;

import org.w3c.dom.Node;

import javax.xml.xpath.XPathExpression;

import org.w3c.dom.Element;

import javax.xml.xpath.XPathConstants;

import org.w3c.dom.DOMException;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;

import updater.Utils;
import updater.Utils.*;

import java.net.URISyntaxException;
import java.net.*;

public class CommandManager {
    private transient Synchronizer mSynchronizer = new Synchronizer();
    private transient Set<Integer> waitSet = new TreeSet();
    private String homePath;
    private String BIAGENT_SERVICE = "AvivBIAgentService";

    public CommandManager() {
        homePath = getHomePath();
        waitSet = Collections.synchronizedSortedSet(new TreeSet());
        new WorkThread().start();
    }

    public void wakeup() {
        try {
            mSynchronizer.wakeUp(true);
        } catch (Exception ex) {
            util.Logger.error("Synchronizer.wakeUp(true) " + ex.getMessage());
        }
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

    private class WorkThread
            extends Thread {
        public WorkThread() {
            super("Commnad Thread");
        }

        public void run() {
            while (true) {
                util.Logger.log("Command thread started...");
                checkUnAckCommands();
                Collection<Command> arr = SQLAdmin.getInstance().getUnexecutableCommands();
                for (Iterator<Command> iter = arr.iterator(); iter.hasNext(); ) {
                    Command item = iter.next();
                    int id = item.getId();
                    if (!waitSet.contains(id)) {
                        waitSet.add(id);
                        java.util.Date execTime = new java.util.Date(item.getExecuteTime());
                        Timer t = new Timer();
                        t.schedule(new Worker(item), execTime);
                    }
                }
                util.Logger.log("Command thread stopped...");
                try {
                    mSynchronizer.wakeUp(false);
                } catch (Exception ex1) {
                    util.Logger.log("Command synchronizer was interrupted....");
                }
            }
        }
    }

    public void checkUnAckCommands() {
        ResultSet rs = null;
        try {
            rs = SQLAdmin.getInstance().executeQuery("SELECT id,status,tm_status FROM logbin_command WHERE ack=0 AND status IS NOT NULL");
            while (rs.next()) {
                int id = rs.getInt("id");
                int status = rs.getInt("status");
                Timestamp tm = rs.getTimestamp("tm_status");
                sendStatus(id, status, tm.getTime());
            }
        } catch (SQLException ex) {
            util.Logger.log(ex);
        } finally {
            SQLAdmin.closeFullResultSet(rs);
        }
    }

    private synchronized void sendStatus(int commandId, int status, long tmStatus) {
        RegisterService service = new RegisterService();
        Register port = service.getRegisterPort();
        boolean fl = port.updateCommandStatus(LogManager.getInstance().getRegId(), SQLAdmin.getInstance().getSystemValue(SQLAdmin.AVIV_ID), commandId, status, tmStatus);
        util.Logger.log("Command " + commandId + " status " + status + " was updated " + fl);
        SQLAdmin.getInstance().updateCommandAck(commandId, fl);
    }

    private class Worker
            extends TimerTask {
        private Command command;

        public Worker(Command c) {
            command = c;
        }

        public void run() {
            String name = command.getPropertyName();
            String value = command.getPropertyValue();
            int id = command.getId();
            int status = -1;
            switch (command.getType()) {
                case LogManager.COMMAND_SYSDEFINE:
                    status = SQLAdmin.getInstance().updateSysdefines(name, value);
                    break;
                case LogManager.COMMAND_CLIENTRES:
                    try {
                        status = SQLAdmin.getInstance().setSystemValue(name.toUpperCase(), value, false);
                    } catch (Exception ex) {
                        util.Logger.log(ex);
                        status = LogManager.STATUS_UPDATE_ERROR;
                    }
                    break;
                case LogManager.COMMAND_QUERY:
                    try {
                        Logger.log("Execute query command:" + value);
                        String arr[] = value.split(";");
                        if (arr.length > 0 &&SQLAdmin.getInstance().executeUpdate(arr[0])>0) {

                            status = LogManager.STATUS_OK;
                            if (SQLAdmin.getInstance().isReplicatedSystem() && arr.length > 1) {
                                String tbl = arr[1];
                                String[] pk = arr.length - 2 > 0 ? new String[arr.length - 2] : new String[]{};
                                for (int i = 2; i < arr.length; i++) {
                                    pk[i - 2] = arr[i];
                                }
                                SQLAdmin.getInstance().publicUpdates(tbl,pk);
                            }
                        } else
                            status = LogManager.STATUS_UPDATE_ERROR;

                    } catch (Exception ex) {
                        util.Logger.log(ex);
                        status = LogManager.STATUS_UPDATE_ERROR;
                    }
                    break;
                case LogManager.COMMAND_PEDIT:
                    try {
                        status = SQLAdmin.getInstance().setSystemValue(name.toUpperCase(), value, true);
                    } catch (Exception ex) {
                        util.Logger.log(ex);
                        status = LogManager.STATUS_UPDATE_ERROR;
                    }
                    break;
                case LogManager.COMMAND_FILE:
                    status = executeFileCommand(value);
                    break;
                case LogManager.COMMAND_BIAGENT:
                    status = executeBIAgentCommand(value);
                    break;

            }
            waitSet.remove(id);
            if (status >= 0 && SQLAdmin.getInstance().updateCommandStatus(id, status)) {
                sendStatus(id, status, System.currentTimeMillis());
            }
        }


    }

    private Collection<FileEntry> buildFileEntries(String value) throws IOException, SAXException, ParserConfigurationException {
        ArrayList<FileEntry> arr = new ArrayList();
        try {
            DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
            domFactory.setNamespaceAware(true);
            DocumentBuilder builder = domFactory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(value.getBytes()));
            XPath xpath = XPathFactory.newInstance().newXPath();
            // XPath Query for showing all nodes value
            XPathExpression expr = xpath.compile("//File");
            Object result = expr.evaluate(doc, XPathConstants.NODESET);
            NodeList nodes = (NodeList) result;
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node instanceof Element) {
                    FileEntry fe = new FileEntry();
                    fe.src = getValue(doc, xpath, i, "Src");
                    fe.dest = getValue(doc, xpath, i, "Dest");
                    fe.isExecutable = "1".equalsIgnoreCase(getValue(doc, xpath, i, "Executable"));
                    fe.md5 = getValue(doc, xpath, i, "MD5");
                    String param = getValue(doc, xpath, i, "Parameters");
                    fe.param = param != null ? parseDestination(param).split("[;]") : new String[]{};
                    arr.add(fe);
                }
            }
        } catch (DOMException ex) {
            util.Logger.log(ex);
        } catch (XPathExpressionException ex) {
            util.Logger.log(ex);
        } catch (FactoryConfigurationError ex) {
            util.Logger.log(ex);
        }
        return arr;
    }

    private String getValue(Document doc, XPath xpath, int i, String child) throws XPathExpressionException {
        String value = null;
        XPathExpression exprParam = xpath.compile("//File[" + (i + 1) + "]/" + child);
        Object resultParam = exprParam.evaluate(doc, XPathConstants.NODESET);
        NodeList params = (NodeList) resultParam;
        if (params.getLength() > 0) {
            Node param = params.item(0);
            if (param instanceof Element) {
                Element elParam = (Element) param;
                value = elParam.getTextContent();

            }
        }
        return value;
    }

    private class FileEntry {
        String src;
        String dest;
        boolean isExecutable;
        String md5;
        String[] param;
        File tmpFile;
        File destFile;
    }

    private String parseDestination(String dest) {
        if (dest.indexOf(LogManager.PATH_JAVA) >= 0) {
            String path = System.getProperty("java.home");
            dest = dest.replace(LogManager.PATH_JAVA, path);
        } else if (dest.indexOf(LogManager.PATH_HOME) >= 0) {
            String path = homePath;
            dest = dest.replace(LogManager.PATH_HOME, path);
        } else if (dest.indexOf(LogManager.PATH_ASH) >= 0) {
            String path = SQLAdmin.getInstance().getStringParam("CREDIT_CARD_WORK_DIRECTORY", null);
            if (path == null) {
                util.Logger.warning("CREDIT_CARD_WORK_DIRECTORY variable doesnt exist.");
                path = homePath;
            }
            dest = dest.replace(LogManager.PATH_ASH, path);
        } else if (dest.indexOf(LogManager.PATH_SYSTEM) >= 0) {
            if (isWindows()) {
                try {
                    String path = Win32Utils.getEnv("SystemRoot");
                    dest = dest.replace(LogManager.PATH_SYSTEM, path);
                } catch (Exception ex) {
                    util.Logger.log(ex);
                }
            }
        }
        //HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows NT\CurrentVersion
        return dest;
    }

    private String getHomePath() {
        File jarFile = null;
        URI uri = null;
        try {
            uri = CommandManager.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            if (uri != null) {
                String scheme = uri.getScheme();
                String str = uri.toASCIIString();
                if (scheme != null) {
                    str = str.substring(scheme.length() + 1); // +1 for : (file://starscream/HOME/DATA/DEV/olga/olga/databrowse/aviv_pos.0.11.2.jar)
                }
                jarFile = new File(str);
                util.Logger.log("Executable : " + jarFile.getAbsolutePath());
            } else {
                util.Logger.warning("Can't get executable path !");
            }
        } catch (URISyntaxException ex1) {
            if (uri != null) {
                util.Logger.error(uri.toASCIIString());
            }
            util.Logger.log(ex1);
        } catch (IllegalArgumentException ex) {
            if (uri != null) {
                util.Logger.error(uri.toASCIIString());
                util.Logger.error(uri.getAuthority());
                util.Logger.error(uri.getRawAuthority());
                util.Logger.error(uri.getScheme());
            }
            util.Logger.log(ex);
        } catch (Exception ex) {
            if (uri != null) {
                util.Logger.error(uri.toASCIIString());
            }
            util.Logger.log(ex);
        }
        return jarFile != null ? jarFile.getParent() : System.getProperty("user.dir");
    }

    private boolean isWindows() {
        String osName = System.getProperty("os.name");
        return osName.toLowerCase().indexOf("windows") >= 0;
    }

    private int executeBIAgentCommand(String value) {
        final int AGENT_INSTALL = 1;
        final int AGENT_START = 2;
        int status = LogManager.STATUS_OK;
        String[] cmd = value.split(":");
        if (cmd.length < 2) {
            Logger.log("Invalid value " + value);
            return LogManager.STATUS_INVALID;
        }
        try {
            int idAgent = Integer.parseInt(cmd[1]);
            int command = Integer.parseInt(cmd[0]);
            switch (command) {
                case AGENT_INSTALL:
                    status = installAgent(idAgent);
                    break;
                case AGENT_START:
                    status = restartAgent();
                    break;
            }

        } catch (NumberFormatException e) {
            Logger.log(e);
            status = LogManager.STATUS_INVALID;
        } catch (Exception e) {
            Logger.log(e);
            status = LogManager.STATUS_INVALID;
        }
        return status;
    }

    private int restartAgent() {
        Utils.exec("net stop " + BIAGENT_SERVICE);
        try {
            Thread.sleep(30000);
        } catch (InterruptedException ex1) {
            util.Logger.log("net stop " + BIAGENT_SERVICE + " was interrupted " + ex1.getMessage());
        }
        util.Logger.log("net stop " + BIAGENT_SERVICE);
        int status = LogManager.STATUS_OK;
        util.Logger.log("net start " + BIAGENT_SERVICE);
        if (Utils.exec("net start " + BIAGENT_SERVICE) != 0) {
            util.Logger.error("Cant restart " + BIAGENT_SERVICE + " !");
            status = LogManager.STATUS_CANT_START_SERVICE;
        }
        return status;
    }

    private int installAgent(int idAgent) throws IOException, InterruptedException {
        int status;
        RandomAccessFile propRandom = null;
        String fileName = "AvivBIAgent.zip";
        String fileServerPath = LogManager.SERVER_PATH + "/biagent/" + fileName;
        File dest = new File(homePath);


        File tmpParent = new File(System.getProperty("user.dir", "./../tmp"));
        if (!tmpParent.exists() && !tmpParent.mkdirs()) {
            Logger.log("The folder " + tmpParent + " doesnt exists and could not create it");
            return LogManager.STATUS_DOWNLOAD_ERROR;

        }
        File tmpFile = new File(tmpParent, fileName);
        Logger.log("executeBIAgentCommand " + idAgent + " From " + fileServerPath + " to " + tmpFile);
        try {
            if (!tmpFile.exists() || tmpFile.delete()) {
                util.Logger.log("Downloading " + fileServerPath);
                if (Utils.download(fileServerPath, tmpFile)) {
                    UnzipUtility unzipper = new UnzipUtility();
                    try {
                        unzipper.unzip(tmpFile, dest);
                    } catch (Exception ex) {
                        Logger.log(ex);
                        return LogManager.STATUS_INVALID;
                    }
                    File prop = new File(dest, "AvivBIAgent" + File.separatorChar + "conf" + File.separatorChar + "BIAgent.properties");
                    if (!prop.exists()) {
                        Logger.error("File " + prop + " doesnt exist");
                        return LogManager.STATUS_INVALID;
                    }
                    // configure  BIAgent.properties
                    propRandom = new RandomAccessFile(prop, "rw");
                    long end = propRandom.length();
                    propRandom.seek(end);
                    propRandom.writeBytes("BI_ID_AGENT = " + idAgent);
                    propRandom.write('\n');
                    propRandom.writeBytes("DB_ADDRESS = " + InetAddress.getLocalHost().getHostAddress());
                    propRandom.write('\n');
                    propRandom.writeBytes("DB_PORT = " + SQLAdmin.getInstance().getMySQLPort());
                    propRandom.close();

                    // install the service
                    File installBatch = new File(dest, "AvivBIAgent" + File.separatorChar + "bin" + File.separatorChar + "InstallAvivBIAgentS.bat");
                    if (!installBatch.exists()) {
                        Logger.error("File " + installBatch + " doesnt exist");
                        return LogManager.STATUS_EXE_ERROR;
                    }
                    String[] commands = new String[]{installBatch.getAbsolutePath()};
                    Process child = Runtime.getRuntime().exec(commands, null, installBatch.getParentFile()); //"cmd /c start"
                    Win32Utils.StreamPumper outPump = new Win32Utils.StreamPumper(child.getInputStream());
                    Win32Utils.StreamPumper errPump = new Win32Utils.StreamPumper(child.getErrorStream());

                    outPump.start();
                    errPump.start();
                    int err = child.waitFor();
                    outPump.join();
                    errPump.join();

                    List lineList = err == 0 ? outPump.getLineList() : errPump.getLineList();
                    for (Iterator iterator = lineList.iterator(); iterator.hasNext(); ) {
                        String line = (String) iterator.next();
                        util.Logger.log(line);
                    }

                    status = err == 0 ? LogManager.STATUS_OK : LogManager.STATUS_EXE_ERROR;

                    // run the service
                    if (status == LogManager.STATUS_OK) {
                        util.Logger.log("net start " + BIAGENT_SERVICE);
                        if (Utils.exec("net start " + BIAGENT_SERVICE) != 0) {
                            util.Logger.error("Cant restart " + BIAGENT_SERVICE + " !");
                            status = LogManager.STATUS_CANT_START_SERVICE;
                        }
                    }


                } else
                    return LogManager.STATUS_DOWNLOAD_ERROR;
            } else {
                Logger.log("Cant delete the previous zip file " + tmpFile);
                return LogManager.STATUS_DOWNLOAD_ERROR;
            }
        } finally {
            if (propRandom != null) {
                try {
                    propRandom.close();
                } catch (IOException e) {
                    Logger.log(e);
                }
            }
        }
        return status;
    }

    private int executeFileCommand(String propertyValue) {
        int status = LogManager.STATUS_OK;
        Collection<FileEntry> fe = null;
        try {
            fe = buildFileEntries(propertyValue);
            for (Iterator<FileEntry> iter = fe.iterator(); iter.hasNext() && status == LogManager.STATUS_OK; ) {
                FileEntry item = iter.next();
                String fileName = item.src;
                String dest = parseDestination(item.dest);
                String fileServerPath = LogManager.SERVER_PATH + "/pnms/" + fileName;
                File tmpFile = new File(System.getProperty("user.dir", "."), fileName);
                if ((!tmpFile.exists() || tmpFile.delete())) {
                    util.Logger.log("Downloading " + fileServerPath);
                    if (Utils.download(fileServerPath, tmpFile)) {
                        if (!Utils.isEquals(tmpFile, item.md5)) {
                            util.Logger.error("Check md5 fails " + item.md5);
                            return LogManager.STATUS_MD5_ERROR;
                        }
                        File destParent = new File(dest);
                        // Check dest dir
                        if (!destParent.exists() && !destParent.mkdirs()) {
                            util.Logger.error(destParent.getAbsolutePath() + " doesnt exist and cant create it");
                            return LogManager.STATUS_UPDATE_ERROR;
                        }
                        item.destFile = new File(dest, fileName);
                        item.tmpFile = tmpFile;
                    } else {
                        status = LogManager.STATUS_DOWNLOAD_ERROR;
                    }

                }

            }
        } catch (ParserConfigurationException ex) {
            util.Logger.log(ex);
            status = LogManager.STATUS_INVALID;
        } catch (SAXException ex) {
            util.Logger.log(ex);
            status = LogManager.STATUS_INVALID;
        } catch (IOException ex) {
            util.Logger.log(ex);
            status = LogManager.STATUS_INVALID;
        }

        // send all to destination
        if (fe != null && status == LogManager.STATUS_OK) {
            for (Iterator<FileEntry> iter = fe.iterator(); iter.hasNext() && status == LogManager.STATUS_OK; ) {
                FileEntry item = iter.next();
                //Check dest file
                if (!item.destFile.exists() || item.destFile.delete()) {

                    // Move a downloaded file to dest dir
                    if (item.tmpFile.renameTo(item.destFile)) {
                        util.Logger.log("Move  " + item.tmpFile.getAbsolutePath() + " to " + item.destFile.getAbsolutePath());
                    } else {
                        util.Logger.error("Cant move the downloaded file " + item.tmpFile.getAbsolutePath() + " to " + item.destFile.getAbsolutePath());
                        return LogManager.STATUS_UPDATE_ERROR;
                    }
                } else {
                    util.Logger.log("Result !destFile.exists() || destFile.delete(): " + !item.destFile.exists() + "||" + item.destFile.delete());
                    return LogManager.STATUS_UPDATE_ERROR;
                }

                // Execute file
                if (item.isExecutable) {
                    try {
                        String[] commands = new String[1 + item.param.length];
                        commands[0] = item.destFile.getAbsolutePath();
                        for (int i = 0; i < item.param.length; i++) {
                            commands[i + 1] = item.param[i];
                        }

                        Process child = Runtime.getRuntime().exec(commands, null, item.destFile.getParentFile()); //"cmd /c start"
                        Win32Utils.StreamPumper outPump = new Win32Utils.StreamPumper(child.getInputStream());
                        Win32Utils.StreamPumper errPump = new Win32Utils.StreamPumper(child.getErrorStream());

                        outPump.start();
                        errPump.start();
                        int err = child.waitFor();
                        outPump.join();
                        errPump.join();

                        List lineList = err == 0 ? outPump.getLineList() : errPump.getLineList();
                        for (Iterator iterator = lineList.iterator(); iterator.hasNext(); ) {
                            String line = (String) iterator.next();
                            util.Logger.log(line);
                        }

                        status = err == 0 ? LogManager.STATUS_OK : LogManager.STATUS_EXE_ERROR;
                    } catch (InterruptedException ex1) {
                        util.Logger.log(ex1);
                    } catch (IOException ex) {
                        util.Logger.log(ex);
                        status = LogManager.STATUS_EXE_ERROR;
                    }

                } else {
                    status = LogManager.STATUS_OK;
                }
            }
        }

        return status;
    }

    private static int executePlainCommand(String value) {
        int status = LogManager.STATUS_INVALID;
        try {
            String[] tasks = value.split(";");
            for (String elem : tasks) {
                util.Logger.log("Run " + elem);
                String[] commands = elem.split("#");
                String[] cmd = new String[commands.length];
        /* cmd[0]="cmd";
         cmd[1]="/c";
         cmd[2]="start";*/
                for (int i = 0; i < commands.length; i++) {
                    cmd[i] = commands[i];
                }

                Process child = Runtime.getRuntime().exec(cmd, null); //"cmd /c start"
                Win32Utils.StreamPumper outPump = new Win32Utils.StreamPumper(child.getInputStream());
                Win32Utils.StreamPumper errPump = new Win32Utils.StreamPumper(child.getErrorStream());

                outPump.start();
                errPump.start();
                int err = child.waitFor();
                outPump.join();
                errPump.join();

                List lineList = err == 0 ? outPump.getLineList() : errPump.getLineList();
                for (Iterator iterator = lineList.iterator(); iterator.hasNext(); ) {
                    String line = (String) iterator.next();
                    util.Logger.log(line);
                }

                status = err == 0 ? LogManager.STATUS_OK : LogManager.STATUS_EXE_ERROR;
            }
        } catch (InterruptedException ex1) {
            util.Logger.log(ex1);
        } catch (IOException ex) {
            util.Logger.log(ex);
            status = LogManager.STATUS_EXE_ERROR;
        }

        return status;
    }

    public static void main(String[] args) {
        System.err.println("Status=" + executePlainCommand("echo#Hello world!;cmd#/c#shutdown -r -f -t 0 -c \"TEST\";cmd#/c#shutdown -a"));
    }

}
