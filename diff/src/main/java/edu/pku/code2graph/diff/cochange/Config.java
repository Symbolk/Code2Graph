package edu.pku.code2graph.diff.cochange;

public class Config {

  public static final String rootDir = System.getProperty("user.home") + "/coding/changelint";
  public static String repoName = "AntennaPod-AntennaPod";
  public static String repoPath = rootDir + "/repos/" + repoName;

  // input: commits to test
  public static String commitsListDir = rootDir + "/input";

  // ground truth: original changes in these commits
  public static final String tempDir = rootDir + "/changes";

  // output: suggested co-changes and comparison with ground truth
  public static final String outputDir = rootDir + "/output1";

}
