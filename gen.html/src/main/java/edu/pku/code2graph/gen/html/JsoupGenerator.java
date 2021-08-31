package edu.pku.code2graph.gen.html;

import edu.pku.code2graph.gen.Generator;
import edu.pku.code2graph.gen.Register;
import edu.pku.code2graph.gen.Registry;
import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.Language;
import edu.pku.code2graph.model.Node;
import edu.pku.code2graph.util.GraphUtil;
import org.apache.commons.io.FilenameUtils;
import org.jgrapht.Graph;
import org.jsoup.nodes.Document;

import java.util.List;

@Register(id = "html-jsoup", accept = "\\.html$", priority = Registry.Priority.MAXIMUM)
public class JsoupGenerator extends Generator {

  @Override
  protected Graph<Node, Edge> generate(List<String> filePaths) {
    HtmlParser parser = new HtmlParser();
    List<Document> docs = parser.parseFiles(filePaths);

    SpringHandler hdl = new SpringHandler();
    for (int i = 0; i < docs.size(); i++) {
      hdl.setFilePath(FilenameUtils.separatorsToUnix(filePaths.get(i)));
      hdl.generateFromDoc(docs.get(i));
    }
    GraphUtil.getUriMap().put(Language.HTML, hdl.getUriMap());
    return hdl.getGraph();
  }
}
