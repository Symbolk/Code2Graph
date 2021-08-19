package edu.pku.code2graph.xll;

import edu.pku.code2graph.model.ElementNode;
import edu.pku.code2graph.model.Language;
import edu.pku.code2graph.model.URI;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class XLLDetector {
  public static List<Pair<URI, URI>> detect(Map<Language, Map<URI, ElementNode>> uriMap) {
    if (uriMap.isEmpty()) {
      return new ArrayList<>();
    }
    // load config
    ConfigLoader loader = new ConfigLoader();
    Optional<Config> configOpt =
        loader.load(
            Objects.requireNonNull(loader.getClass().getClassLoader().getResource("config.yml"))
                .getPath());
    configOpt.ifPresent(System.out::println);
    // create patterns and match

    return new ArrayList<>();
  }
}
