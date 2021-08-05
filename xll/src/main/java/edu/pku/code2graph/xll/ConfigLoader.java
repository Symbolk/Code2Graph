package edu.pku.code2graph.xll;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Objects;

public class ConfigLoader {
  public static void main(String[] args) {
    ConfigLoader loader = new ConfigLoader();
    loader.load(
        Objects.requireNonNull(loader.getClass().getClassLoader().getResource("config.yml"))
            .getPath());
  }

  public void load(String path) {
    InputStream inputStream = null;
    try {
      inputStream = new FileInputStream(path);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }

    if (null != inputStream) {
      //      Yaml yaml = new Yaml();
      //      Map<String, Object> config = yaml.load(inputStream);
      Yaml yaml = new Yaml(new Constructor(Config.class));
      Config config = yaml.load(inputStream);
      System.out.println(config);
    } else {
      System.out.println("Error when reading file: " + path);
    }
  }
}
