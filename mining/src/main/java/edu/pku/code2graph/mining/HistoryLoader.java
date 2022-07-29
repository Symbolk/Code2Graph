package edu.pku.code2graph.mining;

import edu.pku.code2graph.model.URITree;

import java.io.*;
import java.util.*;

import static edu.pku.code2graph.cache.CacheHandler.loadCacheToSet;

public class HistoryLoader extends ArrayList<URITree> {
  String cacheDir;
  Map<String, List<String>> commits = new LinkedHashMap<>();
  Map<String, Set<String>> snapshots = new HashMap<>();
  Analyzer analyzer = new Analyzer();

  public HistoryLoader(String framework, String repoName) throws IOException {
    cacheDir = System.getProperty("user.home") + "/coding/xll/sha-history/" + framework + "/" + repoName;
    loadCommits();
    loadSnapshots();
    loadAll();
    System.out.println(analyzer.positive);
  }

  private void loadCommits() throws IOException {
    String commitsPath = cacheDir + "/commits.txt";
    FileReader fr = new FileReader(commitsPath);
    BufferedReader br = new BufferedReader(fr);
    String line;
    while ((line = br.readLine()) != null) {
      StringTokenizer st = new StringTokenizer(line, ",");
      String commitId = st.nextToken();
      List<String> hashes = new ArrayList<>();
      commits.put(commitId, hashes);
      while (st.hasMoreTokens()) {
        hashes.add(st.nextToken());
      }
    }
    br.close();
    fr.close();
    System.out.println("total commits: " + commits.size());
  }

  private void loadSnapshots() throws IOException {
    File[] files = new File(cacheDir).listFiles((dir, name) -> {
      return name.endsWith(".csv");
    });
    System.out.println("total files: " + files.length);
    Set<String> uriAll = new HashSet<>();
    for (File file : files) {
      Set<String> uris = new HashSet<>();
      String name = file.getName();
      snapshots.put(name.substring(0, name.length() - 4), uris);
      loadCacheToSet(file.getAbsolutePath(), uris);
      uriAll.addAll(uris);
    }
    System.out.println("total uris: " + uriAll.size());
  }

  public void loadAll() {
    int age = 0;
    String previous = null;
    for (String current : commits.keySet()) {
      diffCommit(current, previous, ++age);
      previous = current;
    }
  }

  public void diffCommit(String head, String base, int age) {
    List<String> headFiles = commits.get(head);
    List<String> baseFiles = commits.getOrDefault(base, new ArrayList<>());
    CommitDiff diff = new CommitDiff(age);
    for (String file : headFiles) {
      if (baseFiles.contains(file)) continue;
      for (String uri : snapshots.get(file)) {
        diff.additions.add(uri);
      }
    }
    for (String file : baseFiles) {
      if (headFiles.contains(file)) continue;
      for (String uri : snapshots.get(file)) {
        if (diff.additions.contains(uri)) {
          diff.additions.remove(uri);
        } else {
          diff.deletions.add(uri);
        }
      }
    }
    System.out.println(head);
    analyzer.addAll(diff.additions);
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
