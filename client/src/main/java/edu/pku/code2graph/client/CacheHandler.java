package edu.pku.code2graph.client;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;
import edu.pku.code2graph.gen.Generator;
import edu.pku.code2graph.gen.Generators;
import edu.pku.code2graph.gen.Register;
import edu.pku.code2graph.model.*;
import edu.pku.code2graph.util.FileUtil;
import edu.pku.code2graph.util.GraphUtil;
import edu.pku.code2graph.model.Range;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.atteo.classindex.ClassIndex;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CacheHandler {
  private static final Logger logger = LoggerFactory.getLogger(CacheHandler.class);

  private static String framework = "";
  private static String defaultConfigPath =
      System.getProperty("user.dir") + "/client/src/main/resources/" + framework + "/config.yml";

  private static Set<Language> supportedLanguages = new HashSet<>();

  private static final String[] headers = {"uri", "range"};

  private static final Generators generator;

  static {
    initGenerators();
    generator = Generators.getInstance();
  }

  public static void initCache(String framework, String projectDir, String cacheDir)
      throws IOException, ParserConfigurationException, SAXException {
    GraphUtil.clearGraph();

    switchFramework(framework, projectDir);

    FileUtil.setRootPath(projectDir);
    Map<String, List<String>> ext2FilePaths =
        FileUtil.listFilePathsInLanguages(projectDir, supportedLanguages);

    Graph<Node, Edge> graph = generator.generateFromFiles(ext2FilePaths);

    writeUrisToCache(cacheDir);
  }

  public static void updateCache(
      String framework, String projectDir, String modifiedFilePath, String cacheDir)
      throws IOException, ParserConfigurationException, SAXException {
    GraphUtil.clearGraph();

    switchFramework(framework, projectDir);

    FileUtil.setRootPath(projectDir);
    Map<String, List<String>> ext2FilePaths =
        FileUtil.categorizeFilesByExtensionInLanguages(
            Arrays.asList(modifiedFilePath), supportedLanguages);

    if (ext2FilePaths.isEmpty()) {
      logger.info("Modified file is in language not supported.");
      return;
    }

    Graph<Node, Edge> graph = generator.generateFromFiles(ext2FilePaths);

    int fileCnt = writeUrisToCache(cacheDir);
    assert (fileCnt <= 1);

    if (fileCnt == 0) {
      File file = new File(cacheDir, modifiedFilePath + ".csv");
      file.deleteOnExit();
    }
  }

  public static Pair<URITree, URI> loadCache(String cacheDir, URITree tree) throws IOException {
    return loadCache(cacheDir, tree, null, null);
  }

  public static Pair<URITree, URI> loadCache(
      String cacheDir, URITree tree, String renamedName, Range renamedRange) throws IOException {
    File cache = new File(cacheDir);

    if (!cache.exists()) {
      logger.error("cache dir " + cacheDir + " not found");
      return null;
    }

    if (!cache.isDirectory()) {
      logger.error("cache dir path " + cacheDir + " not direct to a directory");
      return null;
    }

    URI renamedURI = null;
    LinkedList<File> dirs = new LinkedList<>();
    dirs.add(cache);
    while (!dirs.isEmpty()) {
      File dir = dirs.removeFirst();
      File[] files = dir.listFiles();
      if (files == null) continue;
      for (File file : files) {
        if (file.isDirectory()) dirs.add(file);
        else {
          URI retURI =
              loadCacheFromEachFile(file.getAbsolutePath(), tree, renamedName, renamedRange);
          renamedURI = retURI == null ? renamedURI : retURI;
        }
      }
    }
    return new MutablePair<>(tree, renamedURI);
  }

  public static URI loadCacheFromEachFile(
      String file, URITree tree, String renamedName, Range renamedRange) throws IOException {
    CsvReader reader = new CsvReader(file);
    reader.readHeaders();
    String[] cacheHeaders = reader.getHeaders();
    if (cacheHeaders.length <= 1) {
      return null;
    }
    String uriHeader = cacheHeaders[0], rangeHeader = cacheHeaders[1];
    URI renamedURI = null;
    while (reader.readRecord()) {
      String uriCache = reader.get(uriHeader);
      URI thisURI = new URI(uriCache);
      String rangeCache = reader.get(rangeHeader);
      Range range = new Range(rangeCache, thisURI.getLayer(0).get("identifier"));
      tree.add(uriCache, range);
      String symbol = Rename.getSymbolOfURI(thisURI);
      if (renamedName != null
          && renamedRange != null
          && symbol != null
          && symbol.endsWith(renamedName)
          && renamedRange.coversInSameLine(renamedRange)) {
        renamedURI = thisURI;
      }
    }
    reader.close();
    return renamedURI;
  }

  public static void initGenerators() {
    ClassIndex.getSubclasses(Generator.class)
        .forEach(
            gen -> {
              Register a = gen.getAnnotation(Register.class);
              if (a != null) Generators.getInstance().install(gen, a);
            });
  }

  private static Set<Language> initLang(String framework, String repoPath)
      throws ParserConfigurationException, SAXException {
    Set<Language> langs = new HashSet<>();
    switch (framework) {
      case "springmvc":
        langs.add(Language.JAVA);
        langs.add(Language.HTML);
        langs.add(Language.JSP);
        break;
      case "android":
        langs.add(Language.JAVA);
        langs.add(Language.XML);
        break;
      case "mybatis":
        langs.add(Language.JAVA);
        //        langs.add(Language.XML);
        //        langs.add(Language.SQL);
        MybatisPreprocesser.preprocessMapperXmlFile(repoPath);
      default:
        logger.error("framework not valid");
    }

    return langs;
  }

  public static void switchFramework(String newFramework, String repoPath)
      throws ParserConfigurationException, SAXException {
    if (framework.equals(newFramework)) return;
    framework = newFramework;
    defaultConfigPath =
        System.getProperty("user.dir") + "/client/src/main/resources/" + framework + "/config.yml";
    supportedLanguages = initLang(newFramework, repoPath);
  }

  private static int writeUrisToCache(String cacheDir) {
    AtomicInteger fileCount = new AtomicInteger();
    URITree tree = GraphUtil.getUriTree();
    Map<String, List<Node>> nodesOfFiles = getAllEleFromTree(tree);
    nodesOfFiles.forEach(
        (file, nodes) -> {
          if (nodes.isEmpty()) return;
          fileCount.getAndIncrement();
          String cacheFile = new File(cacheDir, file + ".csv").toString();
          try {
            FileUtil.createFile(cacheFile);
          } catch (IOException e) {
            e.printStackTrace();
            logger.warn("can't create file " + cacheFile);
            return;
          }
          CsvWriter writer = new CsvWriter(cacheFile, ',', Charset.forName("UTF-8"));
          try {
            writer.writeRecord(headers);
          } catch (IOException e) {
            e.printStackTrace();
          }
          nodes.forEach(
              node -> {
                if (node.getUri() != null) {
                  String[] record = {
                    node.getUri().toString(),
                    node.getRange() == null ? "" : node.getRange().toString()
                  };
                  try {
                    writer.writeRecord(record);
                  } catch (IOException e) {
                    e.printStackTrace();
                  }
                }
              });
          writer.close();
        });

    return fileCount.get();
  }

  private static Map<String, List<Node>> getAllEleFromTree(URITree tree) {
    Map<String, List<Node>> res = new HashMap<>();
    tree.children.forEach(
        (key, value) -> {
          if (key.getLanguage() == Language.FILE) {
            assert (!res.containsKey(key.getIdentifier()));
            res.put(key.getIdentifier(), getEleForEachFile(value));
          } else {
            getAllEleFromTree(value);
          }
        });

    return res;
  }

  private static List<Node> getEleForEachFile(URITree fileTree) {
    return fileTree.getAllNodes();
  }
}
