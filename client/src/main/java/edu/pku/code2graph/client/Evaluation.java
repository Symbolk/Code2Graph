package edu.pku.code2graph.client;

import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.Node;
import edu.pku.code2graph.model.URI;
import org.apache.commons.lang3.tuple.Pair;
import org.jgrapht.Graph;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Evaluation {

  public static void main(String[] args) {
    //
    String repoName = "";
    String repoPath = "";

    Code2Graph client = new Code2Graph(repoName, repoPath);
    testRename(client);
  }

  private static void testRename(Code2Graph client) {
    List<String> filePaths = new ArrayList<>();
    filePaths.add(
        client.getRepoPath()
            + File.separator
            + "client/src/main/java/edu/pku/code2graph/client/Code2Graph.java");
    Graph<Node, Edge> graph = client.generateGraph(filePaths);

    List<Pair<URI, URI>> xllLinks = client.getXllLinks();

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
