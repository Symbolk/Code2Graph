package edu.pku.code2graph.diff.cochange;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.pku.code2graph.diff.RepoAnalyzer;
import edu.pku.code2graph.diff.model.DiffFile;
import edu.pku.code2graph.diff.model.FileType;
import edu.pku.code2graph.exception.InvalidRepoException;
import edu.pku.code2graph.exception.NonexistPathException;
import edu.pku.code2graph.util.FileUtil;
import org.apache.log4j.PropertyConfigurator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/** Collect xml and java changes/diffs in each commit as the Ground Truth */
public class ChangesCollector {

  static Logger logger = LoggerFactory.getLogger(ChangesCollector.class);
  private static final String rootFolder = Config.rootDir;
  private static String repoName = "";
  private static String repoPath = "";
  private static String commitsListDir = Config.commitsListDir;
  private static final String tempDir = Config.tempDir;

  public static void main(String[] args) {
    PropertyConfigurator.configure(
        System.getProperty("user.dir") + File.separator + "log4j.properties");
    repoName = Config.repoName;
    repoPath = Config.repoPath;
    try {
      collectChangesForRepo();
    } catch (IOException | NonexistPathException | InvalidRepoException e) {
      e.printStackTrace();
    }
  }

  private static void collectChangesForRepo()
      throws IOException, NonexistPathException, InvalidRepoException {

    List<String> focusList = new ArrayList<>();
    //    focusList.add("111a0f9f171341f2c35f1c10cdddcb9dcf53f405");

    System.out.printf("Collecting data for repo: %s at %s %n", repoName, repoPath);
    RepoAnalyzer repoAnalyzer = new RepoAnalyzer(repoName, repoPath);
    // input
    String commitListFilePath = commitsListDir + File.separator + repoName + ".json";
    if (focusList.isEmpty()) {
      FileUtil.clearDir(tempDir + File.separator + repoName);
    }

    JSONParser parser = new JSONParser();
    JSONArray commitList = new JSONArray();
    try (FileReader reader = new FileReader(commitListFilePath)) {
      commitList = (JSONArray) parser.parse(reader);
    } catch (IOException | ParseException e) {
      e.printStackTrace();
    }

    // one entry, one commit
    for (JSONObject commit : (Iterable<JSONObject>) commitList) {
      String commitID = (String) commit.get("commit_id");
      if (!focusList.isEmpty()) {
        if (!focusList.contains(commitID)) {
          continue;
        }
      }
      // output
      String outputPath = tempDir + File.separator + repoName + File.separator + commitID + ".json";
      //      if ((new File(outputPath)).exists()) {
      //        continue;
      //      }

      System.out.printf("Computing diffs for commit: %s %n", commitID);
      List<DiffFile> diffFiles = repoAnalyzer.analyzeCommit(commitID);
      //    DataCollector dataCollector = new DataCollector(tempDir);
      //    Pair<List<String>, List<String>> tempFilePaths = dataCollector.collect(diffFiles);

      Map<String, List<XMLDiff>> xmlDiffs = new HashMap<>();
      Map<String, List<JavaDiff>> javaDiffs = new HashMap<>();

      Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

      for (DiffFile diffFile : diffFiles) {
        if (diffFile.getFileType().equals(FileType.XML)
                && diffFile.getARelativePath().contains("layout")
            || diffFile.getARelativePath().contains("menu")) {
          List<XMLDiff> changes = XMLDiffUtil.computeXMLChangesWithGumtree(diffFile);
          if (!changes.isEmpty()) {
            xmlDiffs.put(diffFile.getARelativePath(), changes);
          }
          //        xmlDiffs.put(
          //            diffFile.getARelativePath(),
          //                XMLDiffUtil.computeXMLChanges(
          //                tempFilePaths, diffFile.getARelativePath(),
          // diffFile.getBRelativePath()));
        } else if (diffFile.getFileType().equals(FileType.JAVA)) {
          List<JavaDiff> changes = JavaDiffUtil.computeJavaChanges(diffFile);
          if (!changes.isEmpty()) {
            // ignore files with only format/comment changes
            javaDiffs.put(diffFile.getARelativePath(), changes);
          }
        }
      }

      // jump commits with pure move/comments/format changes
      if (xmlDiffs.isEmpty() || javaDiffs.isEmpty()) {
        continue;
      }

      System.out.printf(
          "XML diff files: %s for commit: %s %n", xmlDiffs.entrySet().size(), commitID);
      System.out.printf(
          "Java diff files: %s for commit: %s %n", javaDiffs.entrySet().size(), commitID);

      JSONObject outputJson = new JSONObject(new LinkedHashMap());
      JSONObject xmlDiffJson = new JSONObject(new LinkedHashMap());
      JSONObject javaDiffJson = new JSONObject(new LinkedHashMap());

      outputJson.put("commit_id", commitID);

      for (var entry : xmlDiffs.entrySet()) {
        JSONArray array = new JSONArray();
        for (var diff : entry.getValue()) {
          array.add(gson.toJsonTree(diff));
        }
        xmlDiffJson.put(entry.getKey(), array);
      }

      for (var entry : javaDiffs.entrySet()) {
        JSONArray array = new JSONArray();
        for (var diff : entry.getValue()) {
          array.add(gson.toJsonTree(diff));
        }
        javaDiffJson.put(entry.getKey(), array);
      }

      outputJson.put("xml_diff", xmlDiffJson);
      outputJson.put("java_diff", javaDiffJson);

      try (FileWriter file = new FileWriter(outputPath, false)) {
        JSONObject.writeJSONString(outputJson, file);
      }
    }
  }
}
