package edu.pku.code2graph.gen.html;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HtmlParser {
  public Document parseString(String dom) {
    Document doc = Jsoup.parse(dom);
    return doc;
  }

  public Document parseFile(String filename) throws IOException {
    File input = new File(filename);
    Document doc = Jsoup.parse(input, "UTF-8");

    return doc;
  }

  public List<Document> parseFiles(List<String> fileList) {
    List<Document> docs = new ArrayList<>();
    fileList.forEach(
        (filename) -> {
          try {
            docs.add(parseFile(filename));
          } catch (IOException e) {
            e.printStackTrace();
          }
        });

    return docs;
  }
}
