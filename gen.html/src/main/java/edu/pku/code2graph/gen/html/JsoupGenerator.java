package edu.pku.code2graph.gen.html;

import edu.pku.code2graph.gen.Generator;
import edu.pku.code2graph.gen.Register;
import edu.pku.code2graph.gen.Registry;
import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.Node;
import org.jgrapht.Graph;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.List;

@Register(id = "html-jsoup", accept = "\\.html$", priority = Registry.Priority.MAXIMUM)
public class JsoupGenerator extends Generator {

  @Override
  protected Graph<Node, Edge> generate(List<String> filePaths) {
    HtmlParser parser = new HtmlParser();
    List<Document> docs = parser.parseFiles(filePaths);

    SpringHandler hdl = new SpringHandler();
    for (int i = 0; i < docs.size(); i++) {
      hdl.setFilePath(filePaths.get(i));
      hdl.generateFromDoc(docs.get(i));
    }
    return hdl.getGraph();
  }
}
