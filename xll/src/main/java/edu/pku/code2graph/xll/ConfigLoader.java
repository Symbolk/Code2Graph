package edu.pku.code2graph.xll;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

public class ConfigLoader {
  static Logger logger = LoggerFactory.getLogger(ConfigLoader.class);

  public Optional<Config> load(String path) {
    try {
      InputStream inputStream = new FileInputStream(path);
      Yaml yaml = new Yaml();
      Object config_raw = yaml.loadAll(inputStream).iterator().next();
      Config config = new Config((Map<String, Object>) config_raw);
      logger.debug("Using config from " + path);
      return Optional.of(config);
    } catch (FileNotFoundException e) {
      logger.error("Error when reading file: " + path);
      System.err.println("Error when reading file: " + path);
      e.printStackTrace();
      return Optional.empty();
    }
  }
}
