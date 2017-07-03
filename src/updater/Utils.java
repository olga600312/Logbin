package updater;

import java.io.*;
import java.security.MessageDigest;
import util.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;

public class Utils {

  private static final String REGQUERY_UTIL = "reg query ";
  private static final String REGSTR_TOKEN = "REG_SZ";

  public static String getRegistryValue(String fullKey, String subKey) {
    try {
      String str = REGQUERY_UTIL + " \"" + fullKey + "\" /v " + subKey;
      // System.err.println(str);
      Process process = Runtime.getRuntime().exec(str);
      StreamReader reader = new StreamReader(process.getInputStream());
      reader.start();
      process.waitFor();
      reader.join();
      String result = reader.getResult();
      int p = result.indexOf(REGSTR_TOKEN);
      if (p == -1) {
        return null;
      }
      return result.substring(p + REGSTR_TOKEN.length()).trim();
    }
    catch (Exception e) {
      return null;
    }
  }

  public static int exec(String str) {
    try {
      Process process = Runtime.getRuntime().exec(str);

      return process.waitFor();
    }
    catch (InterruptedException ex) {
      util.Logger.log(ex);
      return -1;
    }
    catch (IOException ex) {
      util.Logger.log(ex);
      return -2;
    }

  }

  static class StreamReader
      extends Thread {
    private InputStream is;
    private StringWriter sw;

    StreamReader(InputStream is) {
      this.is = is;
      sw = new StringWriter();
    }

    public void run() {
      try {
        int c;
        while ( (c = is.read()) != -1) {
          sw.write(c);
        }
      }
      catch (IOException e) {
        ;
      }
    }

    String getResult() {
      return sw.toString();
    }
  }

  public static String convertToHex(byte[] data) {
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < data.length; i++) {
      int halfbyte = (data[i] >>> 4) & 0x0F;
      int two_halfs = 0;
      do {
        if ( (0 <= halfbyte) && (halfbyte <= 9)) {
          buf.append( (char) ('0' + halfbyte));
        }
        else {
          buf.append( (char) ('a' + (halfbyte - 10)));
        }
        halfbyte = data[i] & 0x0F;
      }
      while (two_halfs++ < 1);
    }
    return buf.toString();
  }

  public static void main(String s[]) {
    System.out.println("Logbin location : " + getRegistryValue("HKLM\\SOFTWARE\\Aviv\\LogBinBackup\\Settings", "Location"));
    byte arr[] = md5(new File("D:\\SETUPS\\Logbin\\Logbin.jar"));
    System.out.println("MD5 " + convertToHex(arr));
  }

  public static byte[] md5(File f) {
    try {
      if (f != null && f.isFile()) {
        MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
        InputStream is = new FileInputStream(f);
        byte[] buffer = new byte[8192];
        int read = 0;
        try {
          while ( (read = is.read(buffer)) > 0) {
            digest.update(buffer, 0, read);
          }
          byte[] md5sum = digest.digest();
          return md5sum;

        }
        catch (IOException e) {
          throw new RuntimeException("Unable to process file for MD5", e);
        }
        finally {
          try {
            is.close();
          }
          catch (IOException e) {
            throw new RuntimeException("Unable to close input stream for MD5 calculation", e);
          }
        }
      }
      else {
        Logger.error("Cant get MD5 Sum: file " + f.getAbsolutePath() + " is directory !");
        return new byte[] {
        };
      }
    }
    catch (Exception ex) {
      Logger.log(ex);
      return new byte[] {};
    }
  }

  public static boolean isEquals(File f, String md5String) {
    boolean fl = false;
    if (md5String != null) {
      byte[] md5 = null;
      try {
        md5 = md5(f);
        fl = md5String.equalsIgnoreCase(md5 != null ? Utils.convertToHex(md5) : null);
      }
      catch (Exception ex) {
        util.Logger.log(ex);
      }
    }
    return fl;
  }

  public static boolean download(String fileServerPath, File file) {
    boolean fl = false;
    try {
      FileOutputStream fos = new FileOutputStream(file);
      URL jarURL = new URL(fileServerPath);
      URLConnection jarConn = jarURL.openConnection();
      InputStream is = jarConn.getInputStream();
      int q;
      while ( (q = is.read()) != -1) {
        fos.write(q);
      }
      is.close();
      fos.close();
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

    return fl;
  }

}
