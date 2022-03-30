package edu.pku.code2graph.client;

import com.csvreader.CsvReader;
import com.google.common.collect.Sets;
import edu.pku.code2graph.model.Language;
import edu.pku.code2graph.model.Range;
import edu.pku.code2graph.model.URI;
import edu.pku.code2graph.util.FileUtil;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.jgrapht.alg.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import java.io.File;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

  private static String projectPath = null;
  private static String cachePath = null;

  static {
    initLogger();
  }

  public static void initCache(String projectDir, String cacheDir)
      throws IOException, ParserConfigurationException, SAXException {
    projectPath = projectDir;
    cachePath = cacheDir;
    CacheHandler.initCache(framework, projectDir, cacheDir);
  }

  public static void updateCache(String projectDir, String modifiedFilePath, String cacheDir)
      throws IOException, ParserConfigurationException, SAXException {
    projectPath = projectDir;
    cachePath = cacheDir;
    CacheHandler.updateCache(framework, projectDir, modifiedFilePath, cacheDir);
  }

  public static RenameResult calcRenameResult(
      String projectDir, String cacheDir, String defName, Range defRange, String newName) {
    projectPath = projectDir;
    cachePath = cacheDir;
    return null;
  }

  public static List<Pair<URI, Range>> xmlFindRef(
      String projectDir, URI xmlIdentifier, String cacheDir) throws IOException {
    String filePath = xmlIdentifier.getLayer(0).getIdentifier();
    return xmlFindRef(projectDir, xmlIdentifier, cacheDir, filePath);
  }

  public static List<Pair<URI, Range>> xmlFindRef(
      String projectDir, URI xmlIdentifier, String cacheDir, String filePath) throws IOException {
    FileUtil.setRootPath(projectDir);
    projectPath = projectDir;
    cachePath = cacheDir;

    List<Pair<URI, Range>> matchedURIs = new ArrayList<>();
    Queue<String> cacheFiles = new LinkedList<>();
    cacheFiles.offer(cacheDir + "/" + filePath);

    if (!isAndroidIdUri(xmlIdentifier)) {
      return matchedURIs;
    }

    Pattern symbolPattern = Pattern.compile("(@\\+[a-z]+\\\\/)(\\w+)");
    String idValue = xmlIdentifier.getLayer(xmlIdentifier.getLayerCount() - 1).getIdentifier();
    Matcher matcher = symbolPattern.matcher(idValue);
    String symbol;
    if (matcher.find()) {
      symbol = matcher.group(2);
    } else {
      return matchedURIs;
    }

    List<Pair<URI, Range>> urisInFile = new ArrayList<>();

    List<String> xmlFiles =
        FileUtil.listFilePathsInLanguages(projectDir, Sets.newHashSet(Language.XML)).get("xml");
    while (!cacheFiles.isEmpty()) {
      String cacheFilePath = cacheFiles.poll() + ".csv";
      File cacheFile = new File(cacheFilePath);
      if (!cacheFile.exists()) {
        logger.error("cache file for path {} not exist", filePath);
      }
      readCacheFile(cacheFilePath, urisInFile, cacheFiles, xmlFiles);
    }

    for (Pair<URI, Range> identifier : urisInFile) {
      URI tgtUri = identifier.getFirst();
      Range tgtRange = identifier.getSecond();
      String tgtIdentifier = tgtUri.getLayer(tgtUri.getLayerCount() - 1).getIdentifier();
      if (tgtIdentifier.endsWith(symbol)) {
        matchedURIs.add(identifier);
      }
    }

    return matchedURIs;
  }

  private static boolean isAndroidIdUri(URI identifier) {
    return identifier.getLayer(1).getIdentifier().endsWith("android:id");
  }

  private static void readCacheFile(
      String cacheFilePath,
      List<Pair<URI, Range>> urisInFile,
      Queue<String> cacheFiles,
      List<String> xmlFiles)
      throws IOException {
    CsvReader reader = new CsvReader(cacheFilePath);
    reader.readHeaders();
    String[] cacheHeaders = reader.getHeaders();
    if (cacheHeaders.length <= 1) {
      logger.error("cache file {} headers not valid, please check", cacheFilePath);
    }
    String uriHeader = cacheHeaders[0], rangeHeader = cacheHeaders[1];
    while (reader.readRecord()) {
      String uriCache = reader.get(uriHeader);
      String rangeCache = reader.get(rangeHeader);
      edu.pku.code2graph.model.Range range = new edu.pku.code2graph.model.Range(rangeCache);
      URI recordURI = new URI(uriCache);
      urisInFile.add(new Pair<>(recordURI, new Range(rangeCache, cacheFilePath)));

      if (recordURI.getLayerCount() == 3
          && recordURI.getLayer(1).getIdentifier().endsWith("include/layout")) {
        String layoutID = recordURI.getLayer(2).getIdentifier();
        if (layoutID.startsWith("@layout")) {
          String[] splitID = layoutID.split("/");
          if (splitID.length != 2) {
            logger.warn("invalid layout value: {}", layoutID);
          }
          String includeFileName = splitID[1] + ".xml";
          cacheFiles.addAll(getFilePathsOf(includeFileName, xmlFiles));
        }
      }
    }
    reader.close();
  }

  private static List<String> getFilePathsOf(String fileName, List<String> xmlFiles) {
    List<String> res = new ArrayList<>();
    xmlFiles.forEach(
        file -> {
          if (file.endsWith(fileName)) {
            file = FileUtil.getRelativePath(projectPath, file);
            file = cachePath + "/" + file;
            res.add(file);
          }
        });
    return res;
  }

  private static void initLogger() {
    BasicConfigurator.configure();
    org.apache.log4j.Logger.getRootLogger().setLevel(Level.INFO);
  }
}
