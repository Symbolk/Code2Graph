package edu.pku.code2graph.diff.cochange;

import org.apache.commons.lang3.tuple.Triple;

import java.util.HashMap;
import java.util.Map;

public class Binding {
  private final String id;

  private Map<String, Integer> files = new HashMap<>();
  private Map<String, Integer> types = new HashMap<>();
  private Map<String, Integer> members = new HashMap<>();

  public Binding(String id) {
    this.id = id;
  }

  public void addRefEntities(Triple<String, String, String> entities) {
    String fileName = entities.getLeft();
    String typeName = entities.getMiddle();
    String memberName = entities.getRight();

    if (!fileName.isEmpty()) {
      if (!files.containsKey(fileName)) {
        files.put(fileName, 1);
      } else {
        files.put(fileName, files.get(fileName) + 1);
      }
    }

    if (!typeName.isEmpty()) {
      if (!types.containsKey(typeName)) {
        types.put(typeName, 1);
      } else {
        types.put(typeName, types.get(typeName) + 1);
      }
    }

    if (!memberName.isEmpty()) {
      if (!members.containsKey(memberName)) {
        members.put(memberName, 1);
      } else {
        members.put(memberName, members.get(memberName) + 1);
      }
    }
  }

  public String getId() {
    return id;
  }

  public Map<String, Integer> getFiles() {
    return files;
  }

  public Map<String, Integer> getTypes() {
    return types;
  }

  public Map<String, Integer> getMembers() {
    return members;
  }
}
