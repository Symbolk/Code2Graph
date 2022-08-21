package edu.pku.code2graph.mining;

import edu.pku.code2graph.model.URI;
import edu.pku.code2graph.util.FileUtil;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Exporter {
  Analyzer analyzer;

  public Exporter(Analyzer analyzer) {
    this.analyzer = analyzer;
  }

  private Object getMeta() {
    Map<String, Object> meta = new LinkedHashMap<>();
    meta.put("commits", analyzer.history.commits.size());
    meta.put("cochanges", analyzer.cochanges);
    meta.put("candidates", analyzer.graph.size());
    meta.put("uris", analyzer.history.getUriCount());
    return meta;
  }

  private Object getPattern(String pattern) {
    URI uri = new URI(pattern);
    Map<String, Object> output = new LinkedHashMap<>();
    output.put("file", uri.layers.get(0).get("identifier"));
    Map<String, Object> node = output;
    for (int i = 1; i < uri.layers.size(); i++) {
      node.put("identifier", uri.getLayer(i).get("identifier"));
      if (i < uri.layers.size() - 1) {
        Map<String, Object> inline = new LinkedHashMap<>();
        node.put("inline", inline);
        node = inline;
      }
    }
    return output;
  }

  private Object getRules() {
    Map<String, Object> rules = new LinkedHashMap<>();
    Iterator<Map.Entry<Candidate, Credit>> it = analyzer.graph.entrySet().stream().sorted((o1, o2) -> {
      return Double.compare(o2.getValue().value, o1.getValue().value);
    }).iterator();
    for (int index = 0; it.hasNext(); ++index) {
      Map.Entry<Candidate, Credit> entry = it.next();
      Map<String, Object> rule = new LinkedHashMap<>();
      rule.put("def", getPattern(entry.getKey().pattern1));
      rule.put("use", getPattern(entry.getKey().pattern2));
      rule.put("credit", entry.getValue().value);
      if (entry.getValue().value < 0.25) break;
      List<Object> history = new LinkedList<>();
      for (Credit.Record record : entry.getValue().history) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("def", record.source1);
        item.put("use", record.source2);
        item.put("commit", record.commit);
        item.put("similarity", record.similarity);
        history.add(item);
      }
      rule.put("history", history);
      rules.put(String.valueOf(index), rule);
    }
    return rules;
  }

  public String exportToString() {
    DumperOptions options = new DumperOptions();
    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    Yaml yaml = new Yaml(options);
    Map<String, Object> data = new HashMap<>();
    data.put("meta", getMeta());
    data.put("rules", getRules());
    return yaml.dump(data);
  }

  public void exportToFile(String path) throws IOException {
    File file = new File(path);
    if (!file.exists()) FileUtil.createFile(path);
    BufferedWriter writer = new BufferedWriter(new FileWriter(file));
    writer.write(exportToString());
    writer.close();
  }
}
