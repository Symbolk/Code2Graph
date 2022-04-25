package edu.pku.code2graph.client;

import com.csvreader.CsvReader;
import com.google.common.collect.Sets;
import edu.pku.code2graph.client.model.RenameInfo;
import edu.pku.code2graph.client.model.RenameResult;
import edu.pku.code2graph.client.model.RenameStatusCode;
import edu.pku.code2graph.model.*;
import edu.pku.code2graph.util.FileUtil;
import edu.pku.code2graph.util.GraphUtil;
import edu.pku.code2graph.xll.Link;
import edu.pku.code2graph.xll.Project;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import java.io.File;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Rename {
  private static final Logger logger = LoggerFactory.getLogger(Rename.class);

  private static final String framework = "android";
  private static String configPath =
      System.getProperty("user.dir") + "/src/main/resources/" + framework + "/config.yml";

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
    return calcRenameResult(projectDir, cacheDir, defName, defRange, newName, configPath);
  }

  public static RenameResult calcRenameResult(
      String projectDir,
      String cacheDir,
      String defName,
      Range defRange,
      String newName,
      String configPath) {
    projectPath = projectDir;
    cachePath = cacheDir;
    RenameResult result = new RenameResult();
    URITree uriTree = GraphUtil.getUriTree();
    try {
      Pair<URITree, URI> loadRet = CacheHandler.loadCache(cachePath, uriTree, defName, defRange);
      Project project = Project.load(configPath);
      project.setTree(uriTree);

      List<Link> links = project.link();

      URI renamedURI = loadRet.getRight();
      if (renamedURI == null) {
        result.setStatus(RenameStatusCode.RENAMED_URI_NOT_FOUND);
        return result;
      }
      String lastLayer = renamedURI.getLayer(renamedURI.getLayerCount() - 1).get("identifier");
      lastLayer = replaceLast(lastLayer, defName, newName);
      // TODO: simplify
      URI newURI = new URI(renamedURI.toString());
      newURI.getLayer(newURI.getLayerCount() - 1).put("identifier", lastLayer);
      List<Pair<URI, URI>> renames = project.rename(renamedURI, newURI);

      List<RenameInfo> renameInfo = renamePairToRenameInfo(uriTree, renames);
      result.setStatus(RenameStatusCode.SUCCESS);
      result.setRenameInfoList(renameInfo);

      return result;
    } catch (IOException e) {
      result.setStatus(RenameStatusCode.IO_ERROR);
      e.printStackTrace();
      return result;
    }
  }

  private static List<RenameInfo> renamePairToRenameInfo(
      URITree uriTree, List<Pair<URI, URI>> renamePairs) {
    List<RenameInfo> renameInfos = new ArrayList<>();
    for (Pair<URI, URI> renamePair : renamePairs) {
      URI oldURI = renamePair.getLeft();
      URI newURI = renamePair.getRight();

      List<Node> nodes = uriTree.get(oldURI);
      for (Node node : nodes) {
        Range range = node.getRange();
        String symbol = getSymbolOfURI(newURI);
        if (symbol != null) {
          String language = newURI.getLayer(newURI.getLayerCount() - 1).get("language");
          RenameInfo renameInfo =
              new RenameInfo(
                  range,
                  symbol,
                  language.equals("ANY")
                      ? newURI.getLayer(newURI.getLayerCount() - 2).get("language")
                      : language);
          renameInfos.add(renameInfo);
        }
      }
    }
    return renameInfos;
  }

  public static String getSymbolOfURI(URI uri) {
    String lastLayer = uri.getLayer(uri.getLayerCount() - 1).get("identifier");
    int split = -1;
    for (int i = lastLayer.length() - 1; i >= 0; i--) {
      if (lastLayer.charAt(i) == '/') {
        if (i >= 2 && lastLayer.charAt(i - 1) == '\\') {
          continue;
        } else {
          split = i;
          break;
        }
      }
    }
    if (split == -1) return lastLayer;
    if (split + 1 >= lastLayer.length()) return null;
    else return URI.removeEscapeCh(lastLayer.substring(split + 1));
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
      URI tgtUri = identifier.getLeft();
      Range tgtRange = identifier.getRight();
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
      urisInFile.add(new MutablePair<>(recordURI, new Range(rangeCache, cacheFilePath)));

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

  public static String replaceLast(String text, String strToReplace, String replaceWithThis) {
    int split = text.lastIndexOf(strToReplace);
    return text.substring(0, split)
        + replaceWithThis
        + text.substring(split + strToReplace.length());
  }
}
