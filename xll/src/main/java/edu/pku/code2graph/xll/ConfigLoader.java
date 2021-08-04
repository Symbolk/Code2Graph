package edu.pku.code2graph.xll;

import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;

public class ConfigLoader {
  public static void main(String[] args) {
    ConfigLoader loader = new ConfigLoader();
    loader.load(Objects.requireNonNull(loader.getClass().getClassLoader().getResource("config.yml")).getPath());
  }

  public void load(String path) {
    InputStream inputStream = null;
    try {
      inputStream = new FileInputStream(path);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }

    if (null != inputStream) {
      Yaml yaml = new Yaml();
      Map<String, Object> data = yaml.load(inputStream);
      System.out.println(data);
    } else {
      System.out.println("Error when reading file: " + path);
    }
  }
}
