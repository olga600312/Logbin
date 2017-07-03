package updater;

import java.io.*;
import java.net.*;

import java.awt.*;
import java.util.Random;
import java.text.*;
import java.util.*;
import util.ResourceManager;

public class VersionUpdater {
  private static String SERVER_PATH = "http://ftp.aviv-pos.co.il/logbin/";
  private static long timeout;
  public static void main(String[] args) {
    // installOptionalPackage();
    SERVER_PATH = args.length > 0 ? args[0] : "http://ftp.aviv-pos.co.il/logbin/";
    try {
      timeout = args.length > 1 ? Long.parseLong(args[1]) : 600000;
    }
    catch (NumberFormatException ex) {
      util.Logger.log(ex);
      timeout = 600000;
    }
    new VersionUpdater().process();
  }

  /*public static ImageIcon getImageIcon(String key) {
    return new ImageIcon( (URL) getResource(key)) {
      public void paintIcon(Component c, Graphics g, int x, int y) {
        g.drawImage(getImage(), 0, 0, c.getWidth(), c.getHeight(), null);
      }
    };
     }

     public static Object getResource(String key) {

    URL url = null;
    String name = key;

    if (name != null) {

      try {
        Class c = Class.forName("updater.VersionUpdater");
        url = c.getResource(name);
      }
      catch (ClassNotFoundException cnfe) {
        util.Logger.log("Unable to find VersionUpdater class");
      }
      return url;
    }
    else {
      return null;
    }
     }*/
  private void sleep() {
    try {
      long sleep = new Random(System.currentTimeMillis()).nextInt(3600000);
      util.Logger.log("Sleep for " + (sleep / 1000 / 60) + " min ");
      Thread.sleep(sleep);
    }
    catch (InterruptedException ex1) {
    }
    catch (NumberFormatException ex1) {
      try {
        Thread.sleep(new Random(System.currentTimeMillis()).nextInt(3600000));
      }
      catch (InterruptedException ex) {}
    }
  }

  public void process() {
    util.Logger.log("Log bin process started.");
    while (true) {
      long t = updateVersion();
      try {
        util.Logger.log("Sleep for " + (t / 1000 / 60 / 60) + " hour . Next Run Time is " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(System.currentTimeMillis() + t)));
        Thread.sleep(t);
      }
      catch (InterruptedException ex) {}
    }
  }

  private static boolean isEquals(File f, String md5String) {
    boolean fl = false;
    if (md5String != null) {
      byte[] md5 = null;
      try {
        md5 = Utils.md5(f);
        fl = md5String.equalsIgnoreCase(md5 != null ? Utils.convertToHex(md5) : null);
      }
      catch (Exception ex) {
        util.Logger.log(ex);
      }
    }
    return fl;
  }

//
  private String getFileMD5(String fileName) {
    String fileServerPath = SERVER_PATH + fileName;
    File f = new File(System.getProperty("user.dir", "."), fileName);
    String res = null;
    if ( (!f.exists() || f.delete()) && download(fileServerPath, f)) {
      RandomAccessFile rf = null;
      try {
        rf = new RandomAccessFile(f, "r");
        do {
          res = rf.readLine();
        }
        while (res != null && res.trim().length() == 0);
      }
      catch (FileNotFoundException ex) {
        util.Logger.log(ex);
      }
      catch (IOException ex) {
        util.Logger.log(ex);
      }
      finally {
        if (rf != null) {
          try {
            rf.close();
            f.delete();
          }
          catch (IOException ex1) {
            util.Logger.log(ex1);
          }
        }
        if (f != null) {
          f.delete();
        }
      }
    }
    return res;
  }

  private boolean download(String fileServerPath, File file) {
    boolean fl = false;
    URLConnection jarConn = null;
    InputStream is = null;
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(file);
      URL jarURL = new URL(fileServerPath);
      jarConn = jarURL.openConnection();
      is = jarConn.getInputStream();
      int q;
      while ( (q = is.read()) != -1) {
        fos.write(q);
      }

      fl = true;
    }
    catch (MalformedURLException ex) {
      util.Logger.log(ex);
    }
    catch (FileNotFoundException ex) {
      util.Logger.log(ex);
    }
    catch (IOException ex) {
      util.Logger.log(ex);
    }
    finally {
      if (is != null) {
        try {
          is.close();
        }
        catch (IOException ex1) {
          util.Logger.error("Cant close URLConnection input stream: "+fileServerPath);
        }
      }
      if (fos != null) {
        try {
          fos.close();
        }
        catch (IOException ex2) {
          util.Logger.error("Cant close File output stream: "+file.getAbsolutePath());
        }
      }

    }

    return fl;
  }

  public long updateVersion() {
    long errTimeout = 7200000;
    util.Logger.log("-------------> Check for the last version update");
    String fileName = "logbin.MD5";
    String md5 = getFileMD5(fileName);
    String logbinLocation = Utils.getRegistryValue("HKLM\\SOFTWARE\\Aviv\\LogBinBackup\\Settings", "Location");
    if (logbinLocation == null) {
      util.Logger.log("Logbin doesnt exist!");
      return timeout;
    }
    util.Logger.log("Logbin path " + logbinLocation);
    String jarName = "Logbin.jar";

    String jarServerPath = SERVER_PATH + jarName;

    File libDir = new File(logbinLocation).getParentFile();
    if (libDir == null) {
      util.Logger.log("Invalid Logbin Location " + logbinLocation);
      return timeout;
    }
    if (! (libDir.exists() || libDir.mkdirs())) {
      util.Logger.log("Cant create logbin folder : " + libDir.getAbsolutePath());
      return timeout;
    }

    File jarFile = new File(libDir, jarName);
    boolean jarExists = jarFile.exists();
    if (jarExists && isEquals(jarFile, md5)) {
      util.Logger.log(jarFile.getName() + " is the last version.");
      return timeout;
    }
    else {
      util.Logger.warning("It needs to update logbin version :" + (jarExists ? "invalid MD5 " : " jarFile doesnt exist"));
    }

    File tmpDir = new File(libDir, "downloads");
    if (! (tmpDir.exists() || tmpDir.mkdirs())) {
      util.Logger.log("Cant create temporary folder for download: " + tmpDir.getAbsolutePath());
      return errTimeout; //
    }
    long t = timeout;
    File tmpFile = new File(tmpDir, jarName);

    /*    Utils.exec("net stop AvivBackupService");
        try {
          Thread.sleep(10000);
        }
        catch (InterruptedException ex1) {
        }
        util.Logger.log("net stop AvivBackupService");*/
    boolean startService = false;
    try {
      if (tmpFile.exists() && !tmpFile.delete()) {
        util.Logger.warning("The old downloaded file already exists and cant delete it " + tmpFile.getAbsolutePath() + ". Try next time");
        return errTimeout;
      }
      if (download(jarServerPath, tmpFile)) {

        // stop Logbin service
        util.Logger.log("1 Try to stop AvivBackupService");
        Utils.exec("net stop AvivBackupService");
        util.Logger.log("2 Start net stop AvivBackupService");
        try {
          Thread.sleep(10000);
        }
        catch (InterruptedException ex1) {
        }
        util.Logger.log("3 net stop AvivBackupService");

        /*// rename jar file
               File prevJar = null;
               if (jarFile.exists()) {
        prevJar = new File(jarFile.getParent(), "logbin" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
        if (prevJar.exists() && !prevJar.delete()) {
          util.Logger.warning("Cant delete old temporary logbin jar " + prevJar.getAbsolutePath());
          prevJar = null;
        }
        else {
          if (!ResourceManager.copyFile(jarFile,prevJar)) {
            util.Logger.warning("Cant rename prev logbin jar " + jarFile.getAbsolutePath() + " to temporary file " + prevJar.getAbsolutePath());
            prevJar = null;
          }
        }
               }*/
       if (!jarFile.exists() || jarFile.delete()) {
         if (tmpFile.renameTo(jarFile)) {
           util.Logger.log("Logbin is installed " + jarFile.exists() + " " + jarFile.getAbsolutePath());
           startService = true;
           //tmpDir.delete();
         }
         else {
           util.Logger.log("Cant move the downloaded file " + tmpFile.getAbsolutePath() + " to " + jarFile.getAbsolutePath());
         }
       }
       else {
         util.Logger.log("Cant delete the old logbin jar: Result !jarFile.exists() || jarFile.delete(): " + !jarFile.exists() + "||" + jarFile.delete());
       }
      }
      else {
        t = errTimeout;
      }
    }
    catch (Throwable ex) {
      util.Logger.log(ex);
    }
    finally {
      if (startService) {
        util.Logger.log("net start AvivBackupService");
        if (Utils.exec("net start AvivBackupService") != 0) {
          util.Logger.error("Cant restart AvivBackupService !");
          t = errTimeout;
        }
      }
      else {
        util.Logger.warning("The logbin service is in undetermined status.");
      }
    }
    return t;
  }

  public static void installOptionalPackage() {
    Utils.exec("net stop AvivBackupService");
    String optionalPackagesJARDirWin = Utils.getRegistryValue("HKLM\\SOFTWARE\\Aviv\\LogBinBackup\\Settings", "Location");
    if (optionalPackagesJARDirWin == null) {
      util.Logger.log("Logbin doesbt exist!");
      return;
    }
    util.Logger.log("Logbin path " + optionalPackagesJARDirWin);
    String jarName = "Logbin.jar";
    String jarServerPath = SERVER_PATH + jarName;

    File libDir = new File(optionalPackagesJARDirWin).getParentFile();
    if (libDir == null) {
      util.Logger.log("Invalid Logbin Location " + optionalPackagesJARDirWin);
      return;
    }
    if (!libDir.exists()) {
      libDir.mkdirs();
    }
    File jarFile = new File(libDir, jarName);
    if (jarFile.exists() && isEquals(jarFile, "3838d816be715f3a50265a1264024aab")) {
      util.Logger.log(jarFile.getName() + " is currentVersion");
      return;
    }
    try {
      if (!jarFile.exists() || jarFile.delete()) {

        FileOutputStream fos = new FileOutputStream(jarFile);
        URL jarURL = new URL(jarServerPath);
        URLConnection jarConn = jarURL.openConnection();
        InputStream is = jarConn.getInputStream();
        int q;
        while ( (q = is.read()) != -1) {
          fos.write(q);
        }
        is.close();
        fos.close();
        util.Logger.log("Logbin is installed " + jarFile.exists() + " " + jarFile.getCanonicalPath());

      }
      else {
        util.Logger.log("Cant delete the old Logbin " + jarFile.getAbsolutePath());
      }
    }
    catch (Exception e) {
      util.Logger.log(e);
    }
    finally {
      if (Utils.exec("net start AvivBackupService") != 0) {
        util.Logger.log("cant start AvivBackupService");
      }
    }
  }

}
