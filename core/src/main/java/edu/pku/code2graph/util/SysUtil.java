package edu.pku.code2graph.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

public class SysUtil {
  /**
   * Run system command under the given dir
   *
   * @param dir
   * @param commands
   * @return
   */
  public static String runSystemCommand(String dir, Charset charSet, String... commands) {
    if (dir.startsWith("~")) {
      dir = dir.replaceFirst("^~", System.getProperty("user.home"));
    }

    StringBuilder builder = new StringBuilder();
    try {
      Runtime rt = Runtime.getRuntime();
      Process proc = rt.exec(commands, null, new File(dir));

      BufferedReader stdInput =
          new BufferedReader(new InputStreamReader(proc.getInputStream(), charSet));

      BufferedReader stdError =
          new BufferedReader(new InputStreamReader(proc.getErrorStream(), charSet));

      String s = null;
      while ((s = stdInput.readLine()) != null) {
        builder.append(s);
        builder.append("\n");
        //                if (verbose) log(s);
      }

      while ((s = stdError.readLine()) != null) {
        builder.append(s);
        builder.append("\n");
        //                if (verbose) log(s);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return builder.toString();
  }
}
