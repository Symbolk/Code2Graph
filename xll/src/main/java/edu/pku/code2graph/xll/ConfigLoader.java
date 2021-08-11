package edu.pku.code2graph.xll;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Optional;

public class ConfigLoader {
  static Logger logger = LoggerFactory.getLogger(ConfigLoader.class);

  public Optional<Config> load(String path) {
    try {
      InputStream inputStream = new FileInputStream(path);
      //      Yaml yaml = new Yaml();
      //      Map<String, Object> config = yaml.load(inputStream);
      Yaml yaml = new Yaml(new Constructor(Config.class));
      Config config = yaml.load(inputStream);
      logger.info("Successfully loaded the config from " + path);
      return Optional.of(config);
    } catch (FileNotFoundException e) {
      logger.error("Error when reading file: " + path);
      System.err.println("Error when reading file: " + path);
      e.printStackTrace();
      return Optional.empty();
    }
  }
}
