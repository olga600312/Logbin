package util;

import java.net.*;
import java.io.*;
import java.nio.channels.FileChannel;
import java.text.*;
import java.util.*;

public class ResourceManager {
  public static URL getResource(String key) {

    URL url = null;
    String name = key;

    if (name != null) {

      try {
        Class c = Class.forName("util.ResourceManager");
        url = c.getResource(name);
      }
      catch (ClassNotFoundException cnfe) {
        util.Logger.log("Unable to find util class");
      }
      return url;
    }
    else {
      return null;
    }
  }

  public static boolean copyFile(File oldF, File newF) throws IOException {
    boolean fl = false;

    if (newF.exists() || newF.createNewFile()) {
      // dont copy if newF and oldF denote the same file or directory . else We can get null length file
      if (!newF.equals(oldF)) {
        FileChannel oldChannel = new
            FileInputStream(oldF).getChannel();
        FileChannel newChannel = new
            FileOutputStream(newF).getChannel();
        try {
          // inChannel.transferTo(0, inChannel.size(),outChannel);

          // magic number for Windows, 64Mb - 32Kb)
          //On the Windows , you can't copy a file bigger than 64Mb
          int maxCount = (64 * 1024 * 1024) - (32 * 1024);
          long size = oldChannel.size();

          long position = 0;
          while (position < size) {
            position += oldChannel.transferTo(position, maxCount, newChannel);
          }
          fl = true;
        }
        catch (IOException e) {
          throw e;
        }
        finally {
          if (oldChannel != null) {
            oldChannel.close();
          }
          if (newChannel != null) {
            newChannel.close();
          }
        }
      }
    }
    return fl;
  }

  public static void saveResource(String fName, String destination) {
    URL url = ResourceManager.getResource(fName);
    InputStream in = null;
    OutputStream out = null; ;
    if (url != null) {
      try {
        File f = new File(destination);
        if (!f.exists()) {
          in = url.openStream();
          out = new FileOutputStream(f);

          // Transfer bytes from in to out
          byte[] buf = new byte[1024];
          int len;
          while ( (len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
          }

        }
      }
      catch (IOException ex) {
        util.Logger.log(ex);
      }
      finally {
        try {
          if (in != null) {
            in.close();
          }
          if (out != null) {
            out.close();
          }
        }
        catch (IOException ex1) {
          util.Logger.log(ex1);
        }
      }

    }
  }

  /*
     public static void saveResource(String fName, String destination) {
       URL url = ResourceManager.getResource(fName);
       InputStream in = null;
       OutputStream out = null; ;
       if (url != null) {
         try {
           File f = new File(destination);
           if (f.exists()) {
             File fPrev = new File(f.getParent(), "previousVersion");
             if (fPrev.exists() || fPrev.mkdirs()) {
               if (! (ResourceManager.copyFile(f, new File(fPrev, fName + "_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()))) && f.delete())) {
                 System.err.println("Cant delete the old version file " + fName);
                 System.exit(1);
               }
             }
           }
           in = url.openStream();
           out = new FileOutputStream(f);

           // Transfer bytes from in to out
           byte[] buf = new byte[1024];
           int len;
           while ( (len = in.read(buf)) > 0) {
             out.write(buf, 0, len);
           }

         }
         catch (IOException ex) {
           util.Logger.log(ex);
         }
         finally {
           try {
             if (in != null) {
               in.close();
             }
             if (out != null) {
               out.close();
             }
           }
           catch (IOException ex1) {
             util.Logger.log(ex1);
           }
         }

       }

     }

   */

  public static void main(String[] args) {
    String fName = "backup.cmd";
    ResourceManager.saveResource(fName, "d:\\!backup.cmd");
  }

}
