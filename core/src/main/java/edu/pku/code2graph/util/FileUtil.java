package edu.pku.code2graph.util;

import org.apache.commons.io.FileUtils;

import java.io.*;

public class FileUtil {
  /**
   * Write the given content in the file of the given file path.
   *
   * @param content
   * @param filePath
   * @return boolean indicating the success of the write operation.
   */
  public static boolean writeStringToFile(String content, String filePath) {
    try {
      FileUtils.writeStringToFile(new File(filePath), content, "UTF-8");
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }
  /**
   * Read the content of a file into string Actually wraps commons io API
   *
   * @return
   */
  public static String readFileToString(String filePath) {
    String content = "";
    try {
      content = FileUtils.readFileToString(new File(filePath), "UTF-8");
    } catch (IOException e) {
      e.printStackTrace();
    }
    return content;
  }

  public static void writeObjectToFile(Object object, String objectFile, boolean append) {
    try {
      ObjectOutputStream out =
          new ObjectOutputStream(
              new BufferedOutputStream(new FileOutputStream(objectFile, append)));
      out.writeObject(object);
      out.flush();
      out.close();
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(0);
    }
  }

  public static Object readObjectFromFile(String objectFile) {
    try {
      ObjectInputStream in =
          new ObjectInputStream(new BufferedInputStream(new FileInputStream(objectFile)));
      Object object = in.readObject();
      in.close();
      return object;
    } catch (Exception e) {
      // e.printStackTrace();
      return null;
    }
  }
}
