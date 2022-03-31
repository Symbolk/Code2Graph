package edu.pku.code2graph.client.evaluation.tools;

import com.csvreader.CsvWriter;
import edu.pku.code2graph.client.Code2Graph;
import edu.pku.code2graph.client.evaluation.model.Identifier;
import edu.pku.code2graph.diff.util.GitService;
import edu.pku.code2graph.diff.util.GitServiceCGit;
import edu.pku.code2graph.exception.InvalidRepoException;
import edu.pku.code2graph.exception.NonexistPathException;
import edu.pku.code2graph.model.Language;
import edu.pku.code2graph.model.Node;
import edu.pku.code2graph.util.GraphUtil;
import edu.pku.code2graph.xll.Link;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static edu.pku.code2graph.client.CacheHandler.initCache;
import static edu.pku.code2graph.client.CacheHandler.loadCache;

public class IdentifierSearch {
  private static final Logger logger = LoggerFactory.getLogger(IdentifierSearch.class);

  // test one repo at a time
  private static final String framework = "android";
  private static final String repoName = "GSYVideoPlayer";
  private static final String commitID = "2a88f0fb29";
  private static final String keyword = "video_item_player";
  private static final String fileName = "";
  private static final String otDir =
      System.getProperty("user.home") + "/coding/xll/" + "identifier";

  private static final String repoPath =
      System.getProperty("user.home") + "/coding/xll/" + framework + "/" + repoName;
  private static final String configPath =
      System.getProperty("user.dir") + "/client/src/main/resources/" + framework + "/config.yml";
  private static String cacheDir =
          System.getProperty("user.home") + "/coding/xll/cache/" + framework + "/" + repoName;

  private static final String otPath = otDir + "/search-in-" + repoName + "-" + commitID + ".csv";

  private static Code2Graph c2g;
  private static GitService gitService;
  private static List<Link> xllLinks = new ArrayList<>();

  public static void main(String[] args) {
    BasicConfigurator.configure();
    org.apache.log4j.Logger.getRootLogger().setLevel(Level.INFO);

    // set up
    try {
      gitService = new GitServiceCGit(repoPath);
      c2g = new Code2Graph(repoName, repoPath, configPath);

      checkCommitId();
      switch (framework) {
        case "springmvc":
          c2g.addSupportedLanguage(Language.JAVA);
          c2g.addSupportedLanguage(Language.HTML);
          c2g.addSupportedLanguage(Language.JSP);
          break;
        case "android":
          c2g.addSupportedLanguage(Language.JAVA);
          c2g.addSupportedLanguage(Language.XML);
          break;
        case "mybatis":
          c2g.addSupportedLanguage(Language.JAVA);
          c2g.addSupportedLanguage(Language.XML);
          c2g.addSupportedLanguage(Language.SQL);
//          MybatisPreprocesser.preprocessMapperXmlFile(repoPath);
          break;
        default:
          c2g.addSupportedLanguage(Language.JAVA);
      }

      logger.info("Generating graph for repo {}:{}", repoName, gitService.getHEADCommitId());
      // for testXLLDetection, run once and save the output, then comment
      if (loadCache(cacheDir, GraphUtil.getUriTree()) == null) {
        initCache(framework, repoPath, cacheDir);
      }
      c2g.generateXLL(GraphUtil.getGraph());
      xllLinks = c2g.getXllLinks();

      List<Identifier> allIds = Identifier.getAllIdentifiers();
      List<Identifier> filtered = searchByKeyword(allIds, keyword);
      System.out.println(otPath);
      exportSearchedIds(filtered, otPath);
    } catch (ParserConfigurationException
        | SAXException
        | NonexistPathException
        | IOException
        | InvalidRepoException e) {
      e.printStackTrace();
    }
  }

  private static void checkCommitId() {
    if (commitID == null || commitID.length() == 0) {
      logger.error("commitId can't be empty");
    }
    if (!gitService.checkoutByCommitID(commitID)) {
      logger.error("Failed to checkout to {}", commitID);
    } else {
      logger.info("Successfully checkout to {}", commitID);
    }
  }

  private static String toFuzzyString(String str) {
    return str.replaceAll("_", "").toLowerCase();
  }

  private static List<Identifier> searchByKeyword(List<Identifier> list, String keyword) {
    List<Pair<Integer, Identifier>> res = new ArrayList<>();
    list.forEach(
        id -> {
          String fuzzyId = toFuzzyString(id.getUri());
          String fuzzyKeyword = toFuzzyString(keyword);
          if (fuzzyId.contains(fuzzyKeyword)) {
            if (fileName != null && fileName.length() != 0) {
              if (!fuzzyId.contains(toFuzzyString(fileName))) {
                return;
              }
            }
            int index = fuzzyId.lastIndexOf(fuzzyKeyword);
            res.add(new ImmutablePair<>(fuzzyId.length() - index, id));
          }
        });

    // simple ranking
    Collections.sort(res, Comparator.comparingInt(Pair::getKey));

    List<Identifier> resIds = new ArrayList<>();
    res.forEach(id -> resIds.add(id.getValue()));
    return resIds;
  }

  private static void exportSearchedIds(List<Identifier> results, String filePath)
      throws IOException {
    File outFile = new File(filePath);
    if (!outFile.exists()) {
      outFile.createNewFile();
    }

    CsvWriter writer = new CsvWriter(filePath, ',', StandardCharsets.UTF_8);

    String[] headers = {"language", "uri", "uriId", "source", "range"};
    writer.writeRecord(headers);

    for (Identifier id : results) {
      String uri = id.getUri();
      int uriId = id.getId();
      Node node = id.getNode();

      // concat snippet of all nodes
      StringBuilder snippet = new StringBuilder();
      snippet.append(node.getSnippet());

      // append range in case it has
      String range = "";
      if (node.getRange() != null && node.getRange().isValid()) {
        range = node.getRange().toString();
      }

      String[] record = {
        id.getLang(), uri, String.valueOf(uriId), snippet.toString().split("\n")[0], range
      };
      writer.writeRecord(record);
    }
    writer.close();
  }
}
