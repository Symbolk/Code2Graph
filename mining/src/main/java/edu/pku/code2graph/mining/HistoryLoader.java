package edu.pku.code2graph.mining;

import edu.pku.code2graph.cache.CommitCache;
import edu.pku.code2graph.model.URITree;

import java.io.*;
import java.util.*;

import static edu.pku.code2graph.cache.CacheHandler.loadCacheToSet;

public class HistoryLoader extends ArrayList<URITree> {
  String cacheDir;
  LinkedHashMap<String, CommitCache> commits = new LinkedHashMap<>();
  Map<String, Set<String>> fileCache = new HashMap<>();
  public Analyzer analyzer = new Analyzer();

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
      CommitCache commit = new CommitCache(parent, line);
      parent = commit.hash;
      commits.put(parent, commit);
    }
    br.close();
    fr.close();
    System.out.println("total commits: " + commits.size());
  }

  public void loadAll() throws IOException {
    int age = 0;
    for (String current : commits.keySet()) {
      diffCommit(current, ++age);
    }
  }

  private Set<String> loadFile(String hash) throws IOException {
    Set<String> result = fileCache.get(hash);
    if (result != null) return result;
    result = new HashSet<>();
    fileCache.put(hash, result);
    loadCacheToSet(cacheDir + "/" + hash + ".csv", result);
    return result;
  }

  public void diffCommit(String hash, int age) throws IOException {
    CommitCache head = commits.get(hash);
    CommitCache base = commits.getOrDefault(head.parent, new CommitCache());
    CommitDiff diff = new CommitDiff(age);
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
    System.out.println(hash + ": " + diff.additions.size());
    analyzer.addAll(diff.additions);
    System.out.println(hash + ": " + diff.deletions.size());
    analyzer.addAll(diff.deletions);
  }

  public class CommitDiff {
    public final Set<String> additions = new HashSet<>();
    public final Set<String> deletions = new HashSet<>();
    public final int age;

    CommitDiff(int age) {
      this.age = age;
    }
  }
}
