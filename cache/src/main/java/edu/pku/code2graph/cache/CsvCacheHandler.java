package edu.pku.code2graph.cache;

import com.csvreader.CsvWriter;
import edu.pku.code2graph.model.Node;
import edu.pku.code2graph.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class CsvCacheHandler extends CacheHandler {
  private static final String[] headers = {"uri", "range"};

  CsvCacheHandler(String projectDir, String cacheDir) {
    super(projectDir, cacheDir);
  }

  @Override
  void writeCache(String file, List<Node> nodes) {
    Path cachePath = Paths.get(cacheDir, file + ".csv");
    String cacheFile = new File(String.valueOf(cachePath)).toString();
    try {
      FileUtil.createFile(cacheFile);
    } catch (IOException e) {
      e.printStackTrace();
      logger.warn("can't create file " + cacheFile);
      return;
    }
    CsvWriter writer = new CsvWriter(cacheFile, ',', StandardCharsets.UTF_8);
    try {
      writer.writeRecord(headers);
    } catch (IOException e) {
      e.printStackTrace();
    }
    nodes.forEach((node) -> {
      if (node.getUri() != null) {
        String[] record = {
            node.getUri().toString(),
            node.getRange() == null ? "" : node.getRange().toString()
        };
        try {
          writer.writeRecord(record);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    });
    writer.close();
  }
}
