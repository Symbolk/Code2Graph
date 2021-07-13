package edu.pku.code2graph.diff.cochange;

public class Config {

  public static final String rootDir = System.getProperty("user.home") + "/coding/changelint";
  public static String repoName = "AntennaPod-AntennaPod";
  public static String repoPath = rootDir + "/repos/" + repoName;
  public static final String tempDir = rootDir + "/changes";
  public static final String outputDir = rootDir + "/output";

  public static String commitsListDir = rootDir + "/input";
}
