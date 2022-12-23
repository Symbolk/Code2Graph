package edu.pku.code2graph.cache;

import edu.pku.code2graph.model.Node;
import edu.pku.code2graph.util.FileUtil;
import edu.pku.code2graph.util.GraphUtil;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class TextCacheHandler extends CacheHandler {
  private final Map<String, String> file2SHA;

  TextCacheHandler(Map<String, List<String>> ext2FilePaths) throws NoSuchAlgorithmException, IOException {
    file2SHA =
        getShaPath(
            ext2FilePaths.values().stream()
                .reduce(
                    new ArrayList<>(),
                    (acc, list) -> {
                      acc.addAll(list);
                      return acc;
                    }));

  }

  @Override
  void writeCache(String file, List<Node> nodes) {}

  private static Map<String, String> getShaPath(List<String> filePaths)
      throws NoSuchAlgorithmException, IOException {
    Map<String, String> file2SHA = new HashMap<>();
    for (String fileName : filePaths) {
      byte[] buffer = new byte[8192];
      int count;
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      BufferedInputStream bis = new BufferedInputStream(new FileInputStream(fileName));
      while ((count = bis.read(buffer)) > 0) {
        digest.update(buffer, 0, count);
      }
      digest.update(fileName.getBytes());
      bis.close();

      byte[] hash = digest.digest();
      StringBuilder hexString = new StringBuilder();
      for (byte b : hash) {
        final String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) hexString.append('0');
        hexString.append(hex);
      }
      file2SHA.put(fileName, hexString.toString());
    }

    return file2SHA;
  }

  public Collection<String> getCacheSHA()
      throws NoSuchAlgorithmException, IOException {
    GraphUtil.clearGraph();
    FileUtil.setRootPath(projectDir);

    File project = new File(projectDir);
    File cache = new File(cacheDir);

    if (!cache.exists()) {
      logger.error("cache dir " + cacheDir + " not found");
      return null;
    }
    if (!project.exists()) {
      logger.error("project dir " + projectDir + " not found");
      return null;
    }
    if (!cache.isDirectory()) {
      logger.error("cache dir path " + cacheDir + " not direct to a directory");
      return null;
    }
    if (!project.isDirectory()) {
      logger.error("project dir path " + projectDir + " not direct to a directory");
      return null;
    }

    Map<String, List<String>> ext2FilePaths =
        FileUtil.listFilePathsInLanguages(projectDir, supportedLanguages);
    List<String> fileList =
        ext2FilePaths.values().stream()
            .reduce(
                new ArrayList<>(),
                (acc, list) -> {
                  acc.addAll(list);
                  return acc;
                });
    return getShaPath(fileList).values();
  }
}
