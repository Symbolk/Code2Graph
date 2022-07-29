package edu.pku.code2graph.cache;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static edu.pku.code2graph.cache.CacheHandler.loadCacheToSet;

public class HistoryLoader {
  private String cacheDir;
  public Map<String, Commit> commits = new LinkedHashMap<>();
  private Map<String, Set<String>> files = new HashMap<>();

  public HistoryLoader(String framework, String repoName) throws IOException {
    cacheDir = System.getProperty("user.home") + "/coding/xll/sha-history/" + framework + "/" + repoName;
    loadCommits();
  }

  private void loadCommits() throws IOException {
    String commitsPath = cacheDir + "/commits.txt";
    FileReader fr = new FileReader(commitsPath);
    BufferedReader br = new BufferedReader(fr);
    String line;
    String parent = null;
    while ((line = br.readLine()) != null) {
      Commit commit = new Commit(parent, line);
      parent = commit.hash;
      commits.put(parent, commit);
    }
    br.close();
    fr.close();
    System.out.println("total commits: " + commits.size());
  }

  private Set<String> loadFile(String hash) throws IOException {
    Set<String> result = files.get(hash);
    if (result != null) return result;
    result = new HashSet<>();
    files.put(hash, result);
    loadCacheToSet(cacheDir + "/" + hash + ".csv", result);
    return result;
  }

  public Diff diff(String hash) throws IOException {
    Commit head = commits.get(hash);
    Commit base = commits.getOrDefault(head.parent, new Commit());
    Diff diff = new Diff();
    for (String file : head.files) {
      if (base.files.contains(file)) continue;
      for (String uri : loadFile(file)) {
        diff.additions.add(uri);
      }
    }
    for (String file : base.files) {
      if (head.files.contains(file)) continue;
      for (String uri : loadFile(file)) {
        if (diff.additions.contains(uri)) {
          diff.additions.remove(uri);
        } else {
          diff.deletions.add(uri);
        }
      }
    }
    return diff;
  }

  public static class Diff {
    public final Set<String> additions = new HashSet<>();
    public final Set<String> deletions = new HashSet<>();
  }
}
