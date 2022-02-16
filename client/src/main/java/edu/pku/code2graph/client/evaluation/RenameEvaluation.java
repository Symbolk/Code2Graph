package edu.pku.code2graph.client.evaluation;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;
import edu.pku.code2graph.client.Code2Graph;
import edu.pku.code2graph.client.MybatisPreprocesser;
import edu.pku.code2graph.client.evaluation.model.Identifier;
import edu.pku.code2graph.diff.util.GitService;
import edu.pku.code2graph.diff.util.GitServiceCGit;
import edu.pku.code2graph.diff.util.MetricUtil;
import edu.pku.code2graph.exception.InvalidRepoException;
import edu.pku.code2graph.exception.NonexistPathException;
import edu.pku.code2graph.model.*;
import edu.pku.code2graph.xll.Link;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

class EvaluationResult {
  double precision;
  double recall;
  double f1;

  public EvaluationResult(double p, double r, double f1) {
    this.precision = p;
    this.recall = r;
    this.f1 = f1;
  }
}

public class RenameEvaluation {
  private static final Logger logger = LoggerFactory.getLogger(RenameEvaluation.class);

  // test one repo at a time
  private static final String framework = "android";
  private static final String repoName = "CloudReader";
  private static final String repoPath =
      System.getProperty("user.home") + "/coding/xll/" + framework + "/" + repoName;
  private static final String configPath =
      System.getProperty("user.dir") + "/client/src/main/resources/" + framework + "/config.yml";
  //      FileUtil.getPathFromURL(
  //          Evaluation.class.getClassLoader().getResource(framework + "/config.yml"));
  private static final String gtDir =
      System.getProperty("user.dir")
          + "/client/src/main/resources/"
          + framework
          + "/rename/groundtruth";
  private static String gtPath = gtDir + "/" + repoName + ".csv";
  private static String iptPath = gtPath.replace("groundtruth", "input");
  private static String otPath = gtPath.replace("groundtruth", "output");
  private static String metricResPath = gtPath.replace("groundtruth", "metric_stats");

  private static final String[] csvHeaders = {"caseID", "uri", "uriID"};

  private static Code2Graph c2g;
  private static GitService gitService;

  private static List<Link> xllLinks = new ArrayList<>();

  private static List<EvaluationResult> evaResults = new ArrayList<>();

  private static List<Identifier> allIdsInRepo;

  private static String iptLangThisCase;

  public static void main(String[] args) {
    BasicConfigurator.configure();
    org.apache.log4j.Logger.getRootLogger().setLevel(Level.INFO);

    // set up
    try {
      gitService = new GitServiceCGit(repoPath);
      c2g = new Code2Graph(repoName, repoPath, configPath);

      addCommitIdToPath();
      switch (framework) {
        case "springmvc":
          c2g.addSupportedLanguage(Language.JAVA);
          c2g.addSupportedLanguage(Language.HTML);
          break;
        case "android":
          c2g.addSupportedLanguage(Language.JAVA);
          c2g.addSupportedLanguage(Language.XML);
          break;
        case "mybatis":
          c2g.addSupportedLanguage(Language.JAVA);
          c2g.addSupportedLanguage(Language.XML);
          c2g.addSupportedLanguage(Language.SQL);
          MybatisPreprocesser.preprocessMapperXmlFile(repoPath);
          break;
        default:
          c2g.addSupportedLanguage(Language.JAVA);
      }

      logger.info("Generating graph for repo {}:{}", repoName, gitService.getHEADCommitId());
      // for testXLLDetection, run once and save the output, then comment
      Graph<Node, Edge> graph = c2g.generateGraph();
      xllLinks = c2g.getXllLinks();

      List<List<Identifier>> renamedIdList = getRenamed();
      List<List<Identifier>> gtIdList = getGroundtruth();
      allIdsInRepo = Identifier.getAllIdentifiers();

      if (renamedIdList == null) return;
      if (gtIdList == null) return;

      if (renamedIdList.size() != gtIdList.size()) {
        logger.error(
            "Case count of ground truth not valid, input file contains {} cases while gt file contains {}",
            renamedIdList.size(),
            gtIdList.size());
        return;
      }

      int idx = 0;
      List<List<Identifier>> allCases = new ArrayList<>();
      for (List<Identifier> renamedIds : renamedIdList) {
        iptLangThisCase = renamedIds.get(0).getLang();
        Set<String> renamedUris = new HashSet<>();
        for (Identifier renamedId : renamedIds) renamedUris.add(renamedId.getUri());
        List<Link> links = new ArrayList<>(xllLinks);
        Set<URI> uriToRename = extractURIToRename(links, renamedUris);

        List<Identifier> oneCase = testRename(uriToRename, gtIdList.get(idx++));
        allCases.add(oneCase);
      }

      exportToRename(allCases, otPath);
      exportMetricRes(evaResults, metricResPath);
    } catch (ParserConfigurationException
        | SAXException
        | NonexistPathException
        | IOException
        | InvalidRepoException e) {
      e.printStackTrace();
    }
  }

  private static void addCommitIdToPath() {
    File dir = new File(gtDir);
    File[] files = dir.listFiles();
    String commitID = null;
    if (files != null) {
      for (File f : files) {
        String filename = f.getName();
        if (!f.isDirectory() && filename.startsWith(repoName + ":")) {
          commitID = filename.substring(0, filename.length() - 4).split(":")[1];
          gtPath = f.getPath();
        }
      }
    }

    if (commitID != null) {
      otPath = gtPath.replace("groundtruth", "output");
      iptPath = gtPath.replace("groundtruth", "input");
      metricResPath = gtPath.replace("groundtruth", "metric_stats");
      if (!gitService.checkoutByCommitID(commitID)) {
        logger.error("Failed to checkout to {}", commitID);
      } else {
        logger.info("Successfully checkout to {}", commitID);
      }
    }
  }

  private static List<List<Identifier>> getIdsFromCsv(String path) throws IOException {
    return getIdsFromCsv(path, false);
  }

  private static List<List<Identifier>> getIdsFromCsv(String path, boolean isInput)
      throws IOException {
    CsvReader iptReader = new CsvReader(path);
    iptReader.readHeaders();
    String[] iptHeaders = iptReader.getHeaders();
    if (!isInput && iptHeaders.length != 3) {
      logger.error("Ground truth header num expected 3, but got " + iptHeaders.length);
      return null;
    } else if (isInput && iptHeaders.length != 4) {
      logger.error("Input header num expected 4, but got " + iptHeaders.length);
      return null;
    }

    String caseIdHeader = iptHeaders[0], uriHeader = iptHeaders[1], uriIdHeader = iptHeaders[2];
    String langHeader = null;
    if (isInput) langHeader = iptHeaders[3];
    int caseId = 0;
    List<List<Identifier>> res = new ArrayList<>();
    while (iptReader.readRecord()) {
      if (Integer.parseInt(iptReader.get(caseIdHeader)) != caseId) {
        res.add(new ArrayList<>());
      }
      caseId = Integer.parseInt(iptReader.get(caseIdHeader));

      String language = null;
      if (langHeader != null) {
        language = iptReader.get(langHeader);
      }
      Identifier newId =
          new Identifier(
              iptReader.get(uriHeader), Integer.parseInt(iptReader.get(uriIdHeader)), language);
      res.get(caseId - 1).add(newId);
    }

    return res;
  }

  // collect renamed identifiers
  private static List<List<Identifier>> getRenamed() throws IOException {
    if (!Files.exists(Paths.get(iptPath))) {
      logger.error("Input file: {} does not exist!", iptPath);
      return null;
    }

    return getIdsFromCsv(iptPath, true);
  }

  // collect groundtruth identifiers
  private static List<List<Identifier>> getGroundtruth() throws IOException {
    if (!Files.exists(Paths.get(gtPath))) {
      logger.error("Ground truth file: {} does not exist!", gtPath);
      return null;
    }

    return getIdsFromCsv(gtPath);
  }

  // extract uri of identifiers to rename
  private static Set<URI> extractURIToRename(List<Link> links, Set<String> ids) {
    Set<URI> res = new HashSet<>();
    Set<String> newIds = new HashSet<>();
    List<Link> toRemove = new ArrayList<>();
    for (Link link : links) {
      boolean defInIds = ids.contains(URI.prettified(link.def)),
          useInIds = ids.contains(URI.prettified(link.use));
      if (defInIds && !useInIds) {
        res.add(link.use);
        newIds.add(URI.prettified(link.use));
        toRemove.add(link);
      } else if (useInIds && !defInIds) {
        res.add(link.def);
        newIds.add(URI.prettified(link.def));
        toRemove.add(link);
      }
    }

    links.removeAll(toRemove);
    if (!newIds.isEmpty()) {
      Set<URI> next = extractURIToRename(links, newIds);
      res.addAll(next);
    }
    return res;
  }

  // compare results with gt and return identifiers
  private static List<Identifier> testRename(Set<URI> uriToRename, List<Identifier> gt) {
    Set<String> uriStrs = new HashSet<>();
    uriToRename.forEach((uri) -> uriStrs.add(URI.prettified(uri)));

    List<Identifier> idToRename = getIdentifiersToRename(uriStrs, allIdsInRepo);

    Set<Identifier> otSet = new HashSet<>(idToRename);
    Set<Identifier> gtSet = new HashSet<>(gt);

    int intersectionNum = MetricUtil.intersectSize(otSet, gtSet);
    // compute precision/recall
    double precision = MetricUtil.computeProportion(intersectionNum, otSet.size());
    double recall = MetricUtil.computeProportion(intersectionNum, gtSet.size());
    double f1 = MetricUtil.formatDouble((2 * precision * recall) / (precision + recall));

    evaResults.add(new EvaluationResult(precision, recall, f1));

    return idToRename;
  }

  // get identifiers to rename from specific uris
  private static List<Identifier> getIdentifiersToRename(
      Set<String> uriStrsToRename, List<Identifier> allIds) {
    List<Identifier> res = new ArrayList<>();

    for (Identifier id : allIds) {
      if (id.getLang().equals(iptLangThisCase)) continue;
      String uriStr = id.getUri();
      if (uriStrsToRename.contains(uriStr)) {
        res.add(id);
      }
    }

    return res;
  }

  private static void exportToRename(List<List<Identifier>> casesOt, String filePath)
      throws IOException {
    File outFile = new File(filePath);
    if (!outFile.exists()) {
      outFile.createNewFile();
    }

    CsvWriter writer = new CsvWriter(filePath, ',', StandardCharsets.UTF_8);

    writer.writeRecord(csvHeaders);

    int idx = 0;
    for (List<Identifier> oneCase : casesOt) {
      idx++;
      for (Identifier id : oneCase) {
        String[] record = {String.valueOf(idx), id.getUri(), id.getId().toString()};
        writer.writeRecord(record);
      }
    }

    writer.close();
  }

  private static void exportMetricRes(List<EvaluationResult> metricRes, String filePath)
      throws IOException {
    File outFile = new File(filePath);
    if (!outFile.exists()) {
      outFile.createNewFile();
    }

    CsvWriter writer = new CsvWriter(filePath, ',', StandardCharsets.UTF_8);

    String[] headers = {"caseId", "precision", "recall", "f1"};
    writer.writeRecord(headers);

    int idx = 0;
    double p_sum = 0, r_sum = 0, f1_sum = 0;
    for (EvaluationResult oneCase : metricRes) {
      idx++;
      String[] record = {
        String.valueOf(idx),
        String.valueOf(oneCase.precision),
        String.valueOf(oneCase.recall),
        String.valueOf(oneCase.f1)
      };
      p_sum = oneCase.precision + p_sum;
      r_sum = oneCase.recall + r_sum;
      f1_sum = oneCase.f1 + f1_sum;

      writer.writeRecord(record);
    }
    writer.close();

    logger.info("Avg Precision = {}%", p_sum / metricRes.size());
    logger.info("Avg Recall = {}%", r_sum / metricRes.size());
    logger.info("Avg F1 = {}%", f1_sum / metricRes.size());
  }
}
