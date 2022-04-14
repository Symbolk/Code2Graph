package edu.pku.code2graph.xll;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public class ConfigLoader {
  private final static Logger logger = LoggerFactory.getLogger(ConfigLoader.class);

  private final Set<String> missing = new HashSet<>();
  private final Project project = new Project();

  public ConfigLoader(String path) throws IOException {
    InputStream inputStream = new FileInputStream(path);
    Yaml yaml = new Yaml();
    Object config = yaml.loadAll(inputStream).iterator().next();
    logger.debug("load from " + path);
    parse((Map<String, Object>) config);
  }

  private void parse(Map<String, Object> config) {
    Map<String, List<String>> flowGraph = (Map<String, List<String>>) config.get("flowgraph");
    Map<String, Object> rawRules = (Map<String, Object>) config.get("rules");
    toposort(flowGraph, rawRules);
  }

  public Project getProject() {
    return project;
  }

  private void reportMissing(String name) {
    if (missing.contains(name)) return;
    logger.warn("missing rule definition {}", name);
    missing.add(name);
  }

  private void toposort(Map<String, List<String>> flowGraph, Map<String, Object> rawRules) {
    Map<String, Integer> degrees = new HashMap<>();
    for (String name : rawRules.keySet()) {
      if (!flowGraph.containsKey(name)) {
        logger.warn("unused rule definition {}", name);
      }
    }

    for (String name : flowGraph.keySet()) {
      flowGraph.put(name, flowGraph.get(name).stream().filter(x -> {
        if (rawRules.containsKey(x) || x.equals("$")) return true;
        reportMissing(x);
        return false;
      }).collect(Collectors.toList()));

      if (rawRules.containsKey(name)) {
        degrees.put(name, 0);
      } else {
        reportMissing(name);
      }
    }

    for (String name : flowGraph.keySet()) {
      for (String prev : flowGraph.get(name)) {
        if (prev.equals("$")) continue;
        degrees.put(prev, degrees.get(prev) + 1);
      }
    }

    List<String> queue = new ArrayList<>();
    for (String name : degrees.keySet()) {
      if (degrees.get(name) == 0) {
        queue.add(name);
      }
    }

    int index = 0;
    while (index < queue.size()) {
      String name = queue.get(index);
      for (String prev : flowGraph.get(name)) {
        if (prev.equals("$")) continue;
        int degree = degrees.get(prev) - 1;
        degrees.put(prev, degree);
        if (degree == 0) {
          queue.add(prev);
        }
      }
      index += 1;
    }

    index -= 1;
    while (index >= 0) {
      String name = queue.get(index);
      List<String> deps = flowGraph.get(name);
      Rule rule = new Rule((Map<String, Object>) rawRules.get(name), deps, name);
      project.addRule(rule);
      index -= 1;
    }
  }
}
