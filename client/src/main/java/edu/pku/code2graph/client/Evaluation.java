package edu.pku.code2graph.client;

import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.Language;
import edu.pku.code2graph.model.Node;
import edu.pku.code2graph.model.URI;
import edu.pku.code2graph.xll.Rule;
import org.apache.commons.lang3.tuple.Triple;
import org.jgrapht.Graph;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class Evaluation {

  public static void main(String[] args) {
    //
    String repoName = "";
    String repoPath = "";

    Code2Graph client = new Code2Graph(repoName, repoPath);
    client.setSupportedLanguages(
        new HashSet(Arrays.asList(Language.JAVA, Language.HTML, Language.XML)));
    testRename(client);
  }

  private static void testRename(Code2Graph client) {
    Graph<Node, Edge> graph = client.generateGraph();
    System.out.println(graph.vertexSet().size());
    System.out.println(graph.edgeSet().size());

    List<Triple<URI, URI, Rule>> xllLinks = client.getXllLinks();

    // randomly pick a uri to rename

    //      client.crossLanguageRename()

    // compare, check or evaluate
  }

  private static void testCochange(Code2Graph client) {
    // for a historical multi-lang commit

    // given changes in lang A, mask changes in lang B

    // predict changes in B that are binding with code in A

  }

  private static void testLint(Code2Graph client) {
    // for the previous version (parent commit) of an inconsistency fixing commit

    // lint

    // compare if the fixed inconsistency is reported
  }
}
