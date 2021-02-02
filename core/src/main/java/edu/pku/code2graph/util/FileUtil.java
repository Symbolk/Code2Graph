package edu.pku.code2graph.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FileUtil {
  /**
   * Create a folder if not exists
   *
   * @param dir abs path
   * @return
   */
  public static String createDir(String dir) {
    File directory = new File(dir);
    if (!directory.exists()) {
      // create the entire directory path including parents
      directory.mkdirs();
    }
    return directory.getAbsolutePath();
  }

  /**
   * Get file name from path
   *
   * @return
   */
  public static String getFileNameFromPath(String filePath) {
    return Paths.get(filePath).getFileName().toString();
  }

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

  public static Map<String, List<String>> categorizeFilesByExtension(List<String> filePaths) {
    Map<String, List<String>> result = new LinkedHashMap<>();
    for (String path : filePaths) {
      String extension = FilenameUtils.getExtension(path);
      if (!result.containsKey(extension)) {
        List<String> temp = new ArrayList<>();
        temp.add(path);
        result.put(extension, temp);
      } else {
        result.get(extension).add(path);
      }
    }
    return result;
  }
}
