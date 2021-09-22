package edu.pku.code2graph.client.extractor;

import com.csvreader.CsvWriter;
import edu.pku.code2graph.model.URI;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

public class AbstractExtractor {
  public List<Pair<URI, URI>> uriPairs = new ArrayList<>();

  public void writeToFile(String filePath) throws IOException {
    File outFile = new File(filePath);
    if (!outFile.exists()) {
      outFile.createNewFile();
    }
    CsvWriter writer = new CsvWriter(filePath, ',', Charset.forName("UTF-8"));
    String[] headers = {"HTML", "JAVA"};
    writer.writeRecord(headers);
    for (Pair<URI, URI> pair : uriPairs) {
      String left = pair.getLeft().toString(), right = pair.getRight().toString();
      String[] record = {
        left.substring(5, left.length() - 1), right.substring(5, right.length() - 1)
      };
      writer.writeRecord(record);
    }
    writer.close();
  }

  protected static List<URI> removeDuplicateOutputField(List<URI> list) {
    Set<URI> set =
            new TreeSet<>(
                    (a, b) -> {
                      int compareToResult = 1; // ==0表示重复
                      if (StringUtils.equals(a.toString(), b.toString())) {
                        compareToResult = 0;
                      }
                      return compareToResult;
                    });
    set.addAll(list);
    return new ArrayList<>(set);
  }

  protected void findExtInRepo(String path, List<String> exts, List<String> filePaths) {
    File file = new File(path);
    LinkedList<File> list = new LinkedList<>();
    if (file.exists()) {
      if (null == file.listFiles()) {
        return;
      }
      list.addAll(Arrays.asList(file.listFiles()));
      while (!list.isEmpty()) {
        File[] files = list.removeFirst().listFiles();
        if (null == files) {
          continue;
        }
        for (File f : files) {
          if (f.isDirectory()) {
            list.add(f);
          } else {
            for (String ext : exts) {
              if (f.getName().length() > ext.length() && f.getName().endsWith(ext)) {
                filePaths.add(f.getAbsolutePath());
              }
            }
          }
        }
      }
    }
  }
}
