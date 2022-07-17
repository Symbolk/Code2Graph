package edu.pku.code2graph.mining;

import edu.pku.code2graph.model.URITree;

import java.util.*;

public class History extends ArrayList<URITree> {
  public final Map<String, ArrayList<String>> commits = new LinkedHashMap<>();
  public final Map<String, ArrayList<String>> files = new HashMap<>();

  public void loadAll() {
    int age = 0;
    String previous = null;
    for (String current : commits.keySet()) {
      diffCommit(current, previous, ++age);
      previous = current;
    }
  }

  public Diff diffCommit(String head, String base, int age) {
    ArrayList<String> headFiles = commits.get(head);
    ArrayList<String> baseFiles = commits.getOrDefault(base, new ArrayList<>());
    Diff diff = new Diff(age);
    for (String file : headFiles) {
      if (baseFiles.contains(file)) continue;
      for (String uri : files.get(file)) {
        diff.additions.add(uri);
      }
    }
    for (String file : baseFiles) {
      if (headFiles.contains(file)) continue;
      for (String uri : files.get(file)) {
        if (diff.additions.contains(uri)) {
          diff.additions.remove(uri);
        } else {
          diff.deletions.add(uri);
        }
      }
    }
    return diff;
  }

  public class Diff {
    public final Set<String> additions = new HashSet<>();
    public final Set<String> deletions = new HashSet<>();
    public final int age;

    Diff(int age) {
      this.age = age;
    }
  }
}
