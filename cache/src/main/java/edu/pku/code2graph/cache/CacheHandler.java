package edu.pku.code2graph.cache;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;
import edu.pku.code2graph.diff.util.GitService;
import edu.pku.code2graph.diff.util.GitServiceCGit;
import edu.pku.code2graph.exception.InvalidRepoException;
import edu.pku.code2graph.exception.NonexistPathException;
import edu.pku.code2graph.gen.Generator;
import edu.pku.code2graph.gen.Generators;
import edu.pku.code2graph.gen.Register;
import edu.pku.code2graph.gen.xml.MybatisPreprocesser;
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
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class CacheHandler {
  protected static final Logger logger = LoggerFactory.getLogger(CacheHandler.class);

  private static final Generators generator;

  protected final Set<Language> supportedLanguages = new HashSet<>();
  protected final String framework;
  protected final String projectDir;
  protected final String cacheDir;

  abstract void writeCache(String file, List<Node> nodes);

  CacheHandler(String framework, String projectDir, String cacheDir, String repoPath) throws ParserConfigurationException, SAXException {
    this.framework = framework;
    this.projectDir = projectDir;
    this.cacheDir = cacheDir;
    initLang(repoPath);
  }

  private void initLang(String repoPath)
      throws ParserConfigurationException, SAXException {
    switch (framework) {
      case "springmvc":
        supportedLanguages.add(Language.JAVA);
        supportedLanguages.add(Language.HTML);
        supportedLanguages.add(Language.JSP);
        break;
      case "android":
        supportedLanguages.add(Language.JAVA);
        supportedLanguages.add(Language.XML);
        break;
      case "mybatis":
        supportedLanguages.add(Language.JAVA);
        //        supportedLanguages.add(Language.XML);
        //        supportedLanguages.add(Language.SQL);
        MybatisPreprocesser.preprocessMapperXmlFile(repoPath);
      default:
        logger.error("framework not valid");
    }
  }

  Map<String, List<String>> prepareCache() throws IOException {
    GraphUtil.clearGraph();
    FileUtil.setRootPath(projectDir);

    Map<String, List<String>> ext2FilePaths =
        FileUtil.listFilePathsInLanguages(fileToParse, supportedLanguages);
    generator.generateFromFiles(ext2FilePaths);
    return ext2FilePaths;
  }

  Map<String, List<String>> prepareCache(List<String> modifiedFilePaths) throws IOException {
    GraphUtil.clearGraph();
    FileUtil.setRootPath(projectDir);

    List<String> fileToParse = new ArrayList<>();
    for (String modifiedFilePath : modifiedFilePaths) {
      String fullPath = Paths.get(projectDir, modifiedFilePath).toString();
      if (!new File(fullPath).exists()) {
        File file = new File(cacheDir, modifiedFilePath + ".csv");
        if (file.exists()) file.delete();
        continue;
      }
      fileToParse.add(fullPath);
    }
    Map<String, List<String>> ext2FilePaths =
        FileUtil.categorizeFilesByExtensionInLanguages(fileToParse, supportedLanguages);
    generator.generateFromFiles(ext2FilePaths);
    return ext2FilePaths;
  }

  int writeUrisToCache() {
    AtomicInteger fileCount = new AtomicInteger();
    URITree tree = GraphUtil.getUriTree();
    Map<String, List<Node>> nodesOfFiles = getAllEleFromTree(tree);
    nodesOfFiles.forEach((file, nodes) -> {
      if (nodes.isEmpty()) return;
      fileCount.getAndIncrement();
      writeCache(file, nodes);
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

  static {
    initGenerators();
    generator = Generators.getInstance();
  }

  public static void initCache(String framework, String projectDir, String cacheDir)
      throws IOException, ParserConfigurationException, SAXException {
    try {
      initCache(framework, projectDir, cacheDir, false);
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
      logger.error("no such algorithm");
    }
  }

  public static void initCache(
      String framework, String projectDir, String cacheDir, boolean shaPath)
      throws IOException, ParserConfigurationException, SAXException, NoSuchAlgorithmException {
//          getShaPath(
//              ext2FilePaths.values().stream()
//                  .reduce(
//                      new ArrayList<>(),
//                      (acc, list) -> {
//                        acc.addAll(list);
//                        return acc;
//                      }));

    CacheHandler handler = new CsvCacheHandler(projectDir, cacheDir);
    handler.prepareCache();
    handler.writeUrisToCache();
  }

  public static void initCache(
      String framework,
      String projectDir,
      String modifiedFilePath,
      String cacheDir,
      boolean shaPath)
      throws ParserConfigurationException, SAXException, NoSuchAlgorithmException, IOException {
    initCache(framework, projectDir, List.of(modifiedFilePath), cacheDir, shaPath);
  }

  public static void initCache(
      String framework,
      String projectDir,
      List<String> modifiedFilePaths,
      String cacheDir,
      boolean shaPath)
      throws NoSuchAlgorithmException, IOException {
    CacheHandler handler = new CsvCacheHandler(projectDir, cacheDir);
    handler.prepareCache(modifiedFilePaths);
    handler.writeUrisToCache();
    Map<String, List<String>> ext2FilePaths = prepareCache(cacheDir, projectDir, modifiedFilePaths);
    CacheHandler handler;
    if (shaPath) {
      handler = new TextCacheHandler(ext2FilePaths);
    } else {
      handler = new CsvCacheHandler(cacheDir);
    }
    handler.writeUrisToCache();
  }

  public static void updateCache(
      String framework, String projectDir, String modifiedFilePath, String cacheDir)
      throws IOException, ParserConfigurationException, SAXException {
    updateCache(
        framework,
        projectDir,
        List.of(Paths.get(projectDir, modifiedFilePath).toString()),
        cacheDir);
  }

  public static void updateCache(
      String framework, String projectDir, List<String> modifiedFilePaths, String cacheDir)
      throws ParserConfigurationException, SAXException, IOException {
    GraphUtil.clearGraph();
    switchFramework(framework, projectDir);
    FileUtil.setRootPath(projectDir);
    List<String> fileToParse = new ArrayList<>();
    for (String modifiedFilePath : modifiedFilePaths) {
      String fullPath = Paths.get(projectDir, modifiedFilePath).toString();
      if (!new File(fullPath).exists()) {
        File file = new File(cacheDir, modifiedFilePath + ".csv");
        if (file.exists()) file.delete();
        continue;
      }
      fileToParse.add(fullPath);
    }
    Map<String, List<String>> ext2FilePaths =
        FileUtil.categorizeFilesByExtensionInLanguages(fileToParse, supportedLanguages);

    if (ext2FilePaths.isEmpty()) {
      return;
    }

    generator.generateFromFiles(ext2FilePaths);
    CacheDumper dumper = new CacheDumper.CsvCacheDumper(cacheDir);
    dumper.writeUrisToCache();
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
          if (!file.getName().endsWith(".csv")) continue;
          URI retURI =
              loadCacheFromEachFile(file.getAbsolutePath(), tree, renamedName, renamedRange);
          renamedURI = retURI == null ? renamedURI : retURI;
        }
      }
    }
    return new MutablePair<>(tree, renamedURI);
  }

  public static Pair<URITree, URI> loadCacheSHA(
      String framework,
      String projectDir,
      String cacheDir,
      URITree tree,
      String renamedName,
      Range renamedRange)
      throws ParserConfigurationException, SAXException, NoSuchAlgorithmException, IOException {
    Collection<String> file2SHA = getCacheSHA(framework, projectDir, cacheDir);

    URI renamedURI = null;
    for (String cacheName : file2SHA) {
      Path cachePath = Paths.get(cacheDir, cacheName + ".csv");
      if (!cachePath.toFile().exists()) {
        logger.error("cache {} not found!", cachePath);
        return null;
      }
      URI retURI = loadCacheFromEachFile(cachePath.toString(), tree, renamedName, renamedRange);
      renamedURI = retURI == null ? renamedURI : retURI;
    }

    return new MutablePair<>(tree, renamedURI);
  }

  public static Collection<String> getCacheSHA(String framework, String projectDir, String cacheDir)
      throws NoSuchAlgorithmException, IOException, ParserConfigurationException, SAXException {
    GraphUtil.clearGraph();
    switchFramework(framework, projectDir);
    FileUtil.setRootPath(projectDir);

    File project = new File(projectDir);
    File cache = new File(cacheDir);

    if (!cache.exists()) {
      logger.error("cache dir " + cacheDir + " not found");
      return null;
    }
    if (!project.exists()) {
      logger.error("project dir " + projectDir + " not found");
      return null;
    }
    if (!cache.isDirectory()) {
      logger.error("cache dir path " + cacheDir + " not direct to a directory");
      return null;
    }
    if (!project.isDirectory()) {
      logger.error("project dir path " + projectDir + " not direct to a directory");
      return null;
    }

    Map<String, List<String>> ext2FilePaths =
        FileUtil.listFilePathsInLanguages(projectDir, supportedLanguages);
    List<String> fileList =
        ext2FilePaths.values().stream()
            .reduce(
                new ArrayList<>(),
                (acc, list) -> {
                  acc.addAll(list);
                  return acc;
                });
    return getShaPath(fileList).values();
  }

  public static URITree loadFor(
      String framework, String repoPath, String cacheDir, String commit, URITree tree)
      throws NonexistPathException, IOException, InvalidRepoException, ParserConfigurationException,
          NoSuchAlgorithmException, SAXException {
    GitService gitService = new GitServiceCGit(repoPath);
    String headCommit = gitService.getLongHEADCommitId();

    if (!gitService.checkoutByLongCommitID(commit)) {
      logger.error("Failed to checkout to {}", commit);
      return null;
    } else {
      logger.info("Successfully checkout to {}", commit);
    }

    loadCacheSHA(framework, repoPath, cacheDir, tree, null, null);

    gitService.checkoutByLongCommitID(headCommit);
    return tree;
  }

  public static void loadCacheToSet(String file, Set<String> tree) throws IOException {
    CsvReader reader = new CsvReader(file);
    reader.readHeaders();
    String[] cacheHeaders = reader.getHeaders();
    if (cacheHeaders.length <= 1) {
      return;
    }
    String uriHeader = cacheHeaders[0];
    while (reader.readRecord()) {
      tree.add(reader.get(uriHeader));
    }
    reader.close();
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
      String symbol = getSymbolOfURI(thisURI);
      if (renamedName != null
          && renamedRange != null
          && symbol != null
          && symbol.endsWith(renamedName)
          && range.coversInSameLine(renamedRange)) {
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
}
