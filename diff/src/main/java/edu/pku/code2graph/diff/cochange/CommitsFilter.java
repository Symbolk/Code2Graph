package edu.pku.code2graph.diff.cochange;

import edu.pku.code2graph.diff.RepoAnalyzer;
import edu.pku.code2graph.diff.model.DiffFile;
import edu.pku.code2graph.diff.model.FileType;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommitsFilter {
  public static void main(String[] args) throws Exception {
    String rootFolder = "/Users/symbolk/coding/data/changelint";
    String commitsListFolder = rootFolder + "/cross-lang-commits/";
    String resultsFolder = rootFolder + "/input-commits/";

    // single repo
    String repoName = "seven332-EhViewer";
    filterCommits(
        repoName,
        rootFolder + "/repos/" + "seven332-EhViewer",
        commitsListFolder + repoName + ".json",
        resultsFolder);

    // read multi-lang commits full list
    //    List<String> filePaths = FileUtil.listFilePaths(commitsListFolder, ".json");

    // one file, one repo
    //    for (String filePath : filePaths) {
    //      String repoName = FileUtil.getFileNameFromPath(filePath).replace(".json", "");
    //
    //      String repoPath = rootFolder + "/repos/" + repoName;
    //      File directory = new File(repoPath);
    //      if (!directory.exists()) {
    //        System.out.println(repoPath + " does not exist!");
    //        continue;
    //      }
    //      filterCommits(repoName, repoPath, filePath, resultsFolder);
    //    }
  }

  private static void filterCommits(
      String repoName, String repoPath, String filePath, String inputCommitsListPath)
      throws Exception {
    System.out.println("Begin to process " + repoPath);
    RepoAnalyzer repoAnalyzer = new RepoAnalyzer(repoName, repoPath);
    JSONParser parser = new JSONParser();
    JSONArray commitList = (JSONArray) parser.parse(new FileReader(filePath));
    JSONArray results = new JSONArray();

    for (JSONObject commit : (Iterable<JSONObject>) commitList) {
      // 1. filter merge commits
      // 2. filter commits with no view changes
      // 3. filter commits with only comments/format changes

      if (!isMergeCommit(commit) && hasViewChanges(commit)) {
        String testCommitID = (String) commit.get("commit_id");
        List<DiffFile> diffFiles = repoAnalyzer.analyzeCommit(testCommitID);
        Map<String, List<JavaDiff>> javaDiffs = new HashMap<>();
        int changedComponent = 0;

        for (DiffFile diffFile : diffFiles) {
          if (diffFile.getFileType().equals(FileType.XML)
              && diffFile.getARelativePath().contains("layout")) {
            List<XMLDiff> xmlDiffs = XMLDiffUtil.computeXMLChangesWithGumtree(diffFile);
            for (XMLDiff diff : xmlDiffs) {
              if (XMLDiffUtil.isIDLabel(diff.getName())) {
                changedComponent += 1;
              }
            }
          } else if (diffFile.getFileType().equals(FileType.JAVA)) {
            List<JavaDiff> changes = JavaDiffUtil.computeJavaChanges(diffFile);
            if (!changes.isEmpty()) {
              // ignore files with only format/comment changes
              javaDiffs.put(diffFile.getARelativePath(), changes);
            }
          }
        }

        if (!javaDiffs.isEmpty() || changedComponent > 0) {
          System.out.println(testCommitID);
          results.add(commit);
        }
      }
    }
    // write to a new json file
    try (FileWriter file =
        new FileWriter(inputCommitsListPath + File.separator + repoName + ".json", false)) {
      JSONArray.writeJSONString(results, file);
    }
    System.out.println(
        "Done for repo: " + repoName + " #Commits: " + results.size() + "/" + commitList.size());
  }

  private static boolean isMergeCommit(JSONObject commit) {
    if (commit == null) {
      return true;
    }
    if ((Long) commit.get("parent_commit_num") > 1) {
      return true;
    }
    return false;
  }

  private static boolean hasViewChanges(JSONObject commit) {
    if (commit == null) {
      return false;
    }
    JSONArray diffFiles = (JSONArray) commit.get("diff_files");
    for (JSONObject diffFile : (Iterable<JSONObject>) diffFiles) {
      if (((String) diffFile.get("file_path")).contains("layout")) {
        return true;
      }
    }
    return false;
  }
}
