package logbin;

import java.sql.*;
import java.io.*;
import java.util.*;
import updater.*;
import java.net.URISyntaxException;
import java.net.URI;

public class Main {
  public Main(String exec) {
    if (exec != null) {
      File fExecutable = new File(exec);
      if (fExecutable.exists()) {
        util.Logger.log("Script path is " + (exec = fExecutable.getAbsolutePath()));
      }
      else {
        util.Logger.error("Script path " + fExecutable.getAbsolutePath() + " doesnt exist and setup null");
        exec = null;
      }
    }

    checkExecutableMD5("1");
    LogManager.getInstance().checkExecutable();
    checkExecutableMD5("2");
    LogManager.getInstance().setExecutable(exec);
    LogManager.getInstance().start();
  }

  public static void main(String[] args) {
    Main main = new Main(args.length > 0 ? args[0] : null);
  }

  private static void checkExecutableMD5(String prefix) {
    URI uri = null;
    File jarFile = null;
    try {
      uri = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI();
      if (uri != null) {
        String scheme = uri.getScheme();
        String str = uri.toASCIIString();
        if (scheme != null) {
          str = str.substring(scheme.length() + 1); // +1 for : (file://starscream/HOME/DATA/DEV/olga/olga/databrowse/aviv_pos.0.11.2.jar)
        }
        jarFile = new File(str);
        util.Logger.log("Executable : " + jarFile.getAbsolutePath());
      }
      else {
        util.Logger.warning("Can't get executable path !");
      }
    }
    catch (URISyntaxException ex1) {
      if (uri != null) {
        util.Logger.error(uri.toASCIIString());
      }
      util.Logger.log(ex1);
    }
    catch (IllegalArgumentException ex) {
      if (uri != null) {
        util.Logger.error(uri.toASCIIString());
        util.Logger.error(uri.getAuthority());
        util.Logger.error(uri.getRawAuthority());
        util.Logger.error(uri.getScheme());
      }
      util.Logger.log(ex);
    }
    catch (Exception ex) {
      if (uri != null) {
        util.Logger.error(uri.toASCIIString());
      }
      util.Logger.log(ex);
    }
   /* if(jarFile!=null){
      byte md5[]=Utils.md5(jarFile);
         util.Logger.log(prefix+"  " +Utils.convertToHex(md5));
    }*/

  }
}
