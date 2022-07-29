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
    GraphUtil.clearGraph();

    switchFramework(framework, projectDir);

    FileUtil.setRootPath(projectDir);
    Map<String, List<String>> ext2FilePaths =
        FileUtil.listFilePathsInLanguages(projectDir, supportedLanguages);

    Graph<Node, Edge> graph = generator.generateFromFiles(ext2FilePaths);

    Map<String, String> file2SHA = null;
    if (shaPath)
      file2SHA =
          getShaPath(
              ext2FilePaths.values().stream()
                  .reduce(
                      new ArrayList<>(),
                      (acc, list) -> {
                        acc.addAll(list);
                        return acc;
                      }));

    writeUrisToCache(projectDir, cacheDir, file2SHA);
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
      throws ParserConfigurationException, SAXException, NoSuchAlgorithmException, IOException {
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

    Graph<Node, Edge> graph = generator.generateFromFiles(ext2FilePaths);

    Map<String, String> file2SHA = null;
    if (shaPath)
      file2SHA =
          getShaPath(
              ext2FilePaths.values().stream()
                  .reduce(
                      new ArrayList<>(),
                      (acc, list) -> {
                        acc.addAll(list);
                        return acc;
                      }));

    writeUrisToCache(projectDir, cacheDir, file2SHA);
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

    Graph<Node, Edge> graph = generator.generateFromFiles(ext2FilePaths);

    writeUrisToCache(projectDir, cacheDir, null);
  }

  public static Pair<URITree, URI> loadCache(String cacheDir, URITree tree) throws IOException {
    return loadCache(cacheDir, tree, null, null);
  }

  public static void loadCache(String cacheDir, Set<String> tree) throws IOException {
    File cache = new File(cacheDir);

    if (!cache.exists()) {
      logger.error("cache dir " + cacheDir + " not found");
      return;
    }

    if (!cache.isDirectory()) {
      logger.error("cache dir path " + cacheDir + " not direct to a directory");
      return;
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
          loadCacheToSet(file.getAbsolutePath(), tree);
        }
      }
    }
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

  public static Collection<String> getCacheSHA(
      String framework,
      String projectDir,
      String cacheDir)
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

  public static Map<String, Collection<String>> loadForAll(
      String framework,
      String repoName,
      String cacheDir)
      throws IOException, NonexistPathException, InvalidRepoException {
    String repoPath = System.getProperty("user.dir").substring(0, System.getProperty("user.dir").lastIndexOf("/"))
        + "/cache/src/main/resources/"
        + framework
        + "/repos/"
        + repoName;
    String commitsPath = cacheDir + "/commits.txt";
    FileReader fr = new FileReader(commitsPath);
    BufferedReader br = new BufferedReader(fr);
    GitService gitService = new GitServiceCGit(repoPath);
    String headCommit = gitService.getLongHEADCommitId();
    String line;
    Map<String, Collection<String>> result = new LinkedHashMap<>();
    while ((line = br.readLine()) != null) {
      StringTokenizer st = new StringTokenizer(line, ",");
      String commitId = st.nextToken();
      Collection<String> hashes = new HashSet<>();
      result.put(commitId, hashes);
      while (st.hasMoreTokens()) {
        hashes.add(st.nextToken());
      }
      System.out.println(commitId + " " + hashes.size());
    }
    gitService.checkoutByLongCommitID(headCommit);
    br.close();
    fr.close();
    return result;
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

  private static int writeUrisToCache(
      String projectDir, String cacheDir, Map<String, String> file2SHA) {
    AtomicInteger fileCount = new AtomicInteger();
    URITree tree = GraphUtil.getUriTree();
    Map<String, List<Node>> nodesOfFiles = getAllEleFromTree(tree);
    nodesOfFiles.forEach(
        (file, nodes) -> {
          if (nodes.isEmpty()) return;
          fileCount.getAndIncrement();
          Path cachePath = Paths.get(cacheDir, file + ".csv");
          if (file2SHA != null)
            cachePath =
                Paths.get(cacheDir, file2SHA.get(Paths.get(projectDir, file).toString()) + ".csv");

          String cacheFile = new File(String.valueOf(cachePath)).toString();
          try {
            FileUtil.createFile(cacheFile);
          } catch (IOException e) {
            e.printStackTrace();
            logger.warn("can't create file " + cacheFile);
            return;
          }
          CsvWriter writer = new CsvWriter(cacheFile, ',', StandardCharsets.UTF_8);
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

  private static Map<String, String> getShaPath(List<String> filePaths)
      throws NoSuchAlgorithmException, IOException {
    Map<String, String> file2SHA = new HashMap<>();
    for (String fileName : filePaths) {
      byte[] buffer = new byte[8192];
      int count;
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      BufferedInputStream bis = new BufferedInputStream(new FileInputStream(fileName));
      while ((count = bis.read(buffer)) > 0) {
        digest.update(buffer, 0, count);
      }
      digest.update(fileName.getBytes());
      bis.close();

      byte[] hash = digest.digest();
      StringBuilder hexString = new StringBuilder();
      for (byte b : hash) {
        final String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) hexString.append('0');
        hexString.append(hex);
      }
      file2SHA.put(fileName, hexString.toString());
    }

    return file2SHA;
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
