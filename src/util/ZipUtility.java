package util;

/**
 * Created by olgats on 25/05/2015 10:18 .
 */

import java.io.*;
import java.util.zip.*;

public class ZipUtility {
    static final int BUFFER = 2048;

    public static void zip(ZipOutputStream out, File dir) throws IOException {
        try {
            //out.setMethod(ZipOutputStream.DEFLATED);
            byte tmpBuf[] = new byte[BUFFER];
            // get a list of files from current directory

            File files[] = dir.listFiles();

            for (int i = 0; i < files.length; i++) {
                System.out.println("Adding: " + files[i]);
                if (files[i].isDirectory()) {
                    zip(out, files[i]);
                } else {
                    FileInputStream in = new FileInputStream(files[i].getAbsolutePath());
                    out.putNextEntry(new ZipEntry(files[i].getCanonicalPath()));
                    int len;
                    while ((len = in.read(tmpBuf)) > 0) {
                        out.write(tmpBuf, 0, len);
                    }
                    out.closeEntry();
                    in.close();
                }
            }
        } catch (Exception e) {
            Logger.log(e);
        }
    }

    public static void main(String[] args) {
        String zipFileName = "D:\\AvivBIAgent.zip";
        ZipOutputStream out = null;
        try {
            out = new ZipOutputStream(new FileOutputStream(zipFileName));
            File f = new File("D:","AvivBIAgent");
            zip(out, f);

        } catch (FileNotFoundException e) {
            Logger.log(e);
        } catch (IOException e) {
            Logger.log(e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    Logger.log(e);
                }
            }
        }

    }
}

