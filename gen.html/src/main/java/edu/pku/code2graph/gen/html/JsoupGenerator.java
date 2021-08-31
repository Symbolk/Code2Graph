package edu.pku.code2graph.gen.html;

import edu.pku.code2graph.gen.Generator;
import edu.pku.code2graph.gen.Register;
import edu.pku.code2graph.gen.Registry;
import edu.pku.code2graph.model.*;
import edu.pku.code2graph.util.GraphUtil;
import org.jgrapht.Graph;
import org.jsoup.nodes.Document;

import java.io.File;
import java.util.List;

@Register(id = "html-jsoup", accept = "\\.html$", priority = Registry.Priority.MAXIMUM)
public class JsoupGenerator extends Generator {

  @Override
  protected Graph<Node, Edge> generate(List<String> filePaths) {
    HtmlParser parser = new HtmlParser();
    List<Document> docs = parser.parseFiles(filePaths);

    SpringHandler hdl = new SpringHandler();
    for (int i = 0; i < docs.size(); i++) {
      hdl.setFilePath(new File(filePaths.get(i)).getPath());
      hdl.generateFromDoc(docs.get(i));
    }
    GraphUtil.getUriMap().put(Language.HTML, hdl.getUriMap());
    return hdl.getGraph();
  }
}
