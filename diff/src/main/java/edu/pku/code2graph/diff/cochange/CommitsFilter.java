package edu.pku.code2graph.diff.cochange;

import edu.pku.code2graph.diff.RepoAnalyzer;
import edu.pku.code2graph.diff.model.DiffFile;
import edu.pku.code2graph.diff.model.DiffHunk;
import edu.pku.code2graph.diff.model.FileType;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Collect test commits while filtering unwanted ones (like merge commits, decorative commits, pure
 * logical commits, etc.)
 */
public class CommitsFilter {
  private static final String rootFolder = Config.rootDir;

  public static void main(String[] args) throws Exception {
    String commitsListFolder = rootFolder + "/cross-lang-commits/";
    String resultsFolder = rootFolder + "/commits/";

    // single repo
    String repoName = Config.repoName;
    String repoPath = Config.repoPath;
    filterCommits(repoName, repoPath, commitsListFolder + repoName + ".json", resultsFolder);

    // multi repos
    // read multi-lang commits full list
    //    List<String> filePaths = FileUtil.listFilePaths(commitsListFolder, ".json");
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
      // 2. filter pure logical commits with no view changes
      // 3. filter decorative commits with only comments/format changes
      if (!isMergeCommit(commit) && hasViewChanges(commit)) {
        String testCommitID = (String) commit.get("commit_id");
        List<DiffFile> diffFiles = repoAnalyzer.analyzeCommit(testCommitID);
        Set<DiffFile> javaDiffFiles = new HashSet<>();
        Set<String> changedComponents = new HashSet<>();

        // filter with diff hunks
        for (DiffFile diffFile : diffFiles) {
          if (diffFile.getFileType().equals(FileType.XML)) {
            for (DiffHunk diffHunk : diffFile.getDiffHunks()) {
              changedComponents.addAll(getInvolvedIDs(diffHunk.getAHunk().getCodeSnippet()));
              changedComponents.addAll(getInvolvedIDs(diffHunk.getBHunk().getCodeSnippet()));
            }
          } else if (diffFile.getFileType().equals(FileType.JAVA) && hasViewCoChanges(diffFile)) {
            javaDiffFiles.add(diffFile);
          }
        }

        if (changedComponents.size() > 0 && javaDiffFiles.size() > 0) {
          System.out.println(
              testCommitID + " : " + changedComponents.size() + ", " + javaDiffFiles.size());
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

  private static boolean hasViewCoChanges(DiffFile diffFile) {
    List<DiffHunk> diffHunks = diffFile.getDiffHunks();
    for (DiffHunk diffHunk : diffHunks) {
      if (diffHunk.getAHunk().getCodeSnippet().stream().anyMatch(line -> line.contains("R.id"))
          || diffHunk.getBHunk().getCodeSnippet().stream()
              .anyMatch(line -> line.contains("R.id"))) {
        return true;
      }
    }
    return false;
  }

  private static boolean isMergeCommit(JSONObject commit) {
    if (commit == null) {
      return true;
    }
    return (Long) commit.get("parent_commit_num") > 1;
  }

  private static boolean hasViewChanges(JSONObject commit) {
    if (commit == null) {
      return false;
    }
    Set<String> changeTypes = new HashSet<>();
    JSONArray diffFiles = (JSONArray) commit.get("diff_files");
    for (JSONObject diffFile : (Iterable<JSONObject>) diffFiles) {
      // contains layout files but not added ones
      String filePath = ((String) diffFile.get("file_path"));
      if (filePath.contains("layout")) {
        changeTypes.add(diffFile.get("change_type").toString().trim());
      }
    }
    if (!changeTypes.isEmpty()) {
      return !changeTypes.contains("A") && changeTypes.contains("M");
    } else {
      return false;
    }
  }

  private static Set<String> getInvolvedIDs(List<String> lines) {
    Set<String> results = new HashSet<>();
    for (String line : lines) {
      if (line.contains("android:id=\"@+id")) {
        results.add(line.trim());
      }
    }
    return results;
  }
}
