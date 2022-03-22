package edu.pku.code2graph.client;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.*;

class Position {
  int line; // 行号，1-based
  int column; // 列号，1-based
}

class Range {
  String fileName; // 文件名
  Position startPos; // 起始位置
  Position endPos; // 终止位置
}

class RenameInfo {
  Range range;
  String newName; // 重命名后的标识符名称
  String programmingLanguage; // 文件的编程语言，如Java，xml
}

class RenameResult {
  int status; // 状态码，如success, failed, ...
  List<RenameInfo> renameInfoList;
}

public class Rename {
  private static final Logger logger = LoggerFactory.getLogger(Rename.class);

  private static final String framework = "android";

  static {
    initLogger();
  }

  public static void initCache(String projectDir, String cacheDir)
      throws IOException, ParserConfigurationException, SAXException {
    CacheHandler.initCache(framework, projectDir, cacheDir);
  }

  public static void updateCache(String projectDir, String modifiedFilePath, String cacheDir)
      throws IOException, ParserConfigurationException, SAXException {
    CacheHandler.updateCache(framework, projectDir, modifiedFilePath, cacheDir);
  }

  public static RenameResult calcRenameResult(String defName, Range defRange, String newName) {
    return null;
  }

  private static void initLogger() {
    BasicConfigurator.configure();
    org.apache.log4j.Logger.getRootLogger().setLevel(Level.INFO);
  }
}
