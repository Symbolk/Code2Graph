package edu.pku.code2graph.util;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

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
   * Read the content of a file into string
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
}
