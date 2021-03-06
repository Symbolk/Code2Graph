/*
 * This file is part of GumTree.
 *
 * GumTree is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GumTree is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GumTree.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2011-2015 Jean-Rémy Falleri <jr.falleri@gmail.com>
 * Copyright 2011-2015 Floréal Morandat <florealm@gmail.com>
 */

package edu.pku.code2graph.gen;

import edu.pku.code2graph.model.*;
import edu.pku.code2graph.util.FileUtil;
import edu.pku.code2graph.util.GraphUtil;
import org.jgrapht.Graph;
import org.jgrapht.alg.util.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Registry of tree generators, using a singleton pattern.
 *
 * <p>Outermost API provider for graph generator module
 */
public class Generators extends Registry<String, Generator, Register> {

  // implements Callable<Graph<Node, Edge>>
  //  @Override
  //  public Graph<Node, Edge> call() {
  //
  //  }

  Logger logger = LoggerFactory.getLogger(Generators.class);

  private static Generators registry;

  /** Return the tree generators registry instance (singleton pattern) */
  public static Generators getInstance() {
    if (registry == null) registry = new Generators();
    return registry;
  }

  /**
   * Dynamically assign generator according to the file type, and generate a single graph if null,
   * fallbacks to default generator
   *
   * @param filePaths
   * @return
   * @throws UnsupportedOperationException
   * @throws IOException
   */
  public Graph<Node, Edge> generateFromFiles(List<String> filePaths)
      throws UnsupportedOperationException, IOException {
    if (filePaths.isEmpty()) {
      throw new UnsupportedOperationException("The given file paths are empty");
    }

    // a map from generator to file paths
    Map<String, List<String>> filesMap = FileUtil.categorizeFilesByExtension(filePaths);

    for (Map.Entry<String, List<String>> entry : filesMap.entrySet()) {
      Generator generator = get(entry.getValue().get(0));
      if (generator == null) {
        // for now just skip the file that cannot handle
        logger.warn("No generator found for file type:{}", entry.getKey());
        continue;
      }
      generator.generateFrom().files(entry.getValue());
    }
    Graph<Node, Edge> graph = GraphUtil.getGraph();
    Set<Node> XMLNodes =
        graph.vertexSet().stream()
            .filter(v -> v.getLanguage().equals(Language.XML))
            .filter(v -> v instanceof ElementNode)
            .collect(Collectors.toSet());

    for (Triple<Node, Type, String> triple : GraphUtil.getCrossLangRefsPool()) {
      findXMLNodeByID(XMLNodes, triple.getThird())
          .ifPresent(
              node ->
                  graph.addEdge(
                      triple.getFirst(), node, new Edge(GraphUtil.eid(), triple.getSecond())));
    }

    //    GraphVizExporter.printAsDot(graph);
    return graph;
  }

  private Optional<Node> findXMLNodeByID(Set<Node> XMLNodes, String id) {
    // id: R.a.b
    String[] secs = id.split("\\.");
    if (secs.length != 3) {
      return Optional.empty();
    } else {
      String qname = "@" + secs[1] + "/" + secs[2];
      return XMLNodes.stream()
          .filter(v -> ((ElementNode) v).getQualifiedName().equals(qname))
          .findFirst();
    }
  }

  public boolean has(String generator) {
    return this.findById(generator) != null;
  }

  @Override
  protected Entry newEntry(Class<? extends Generator> clazz, Register annotation) {
    return new Entry(annotation.id(), clazz, defaultFactory(clazz), annotation.priority()) {
      final Pattern[] accept;

      {
        String[] accept = annotation.accept();
        this.accept = new Pattern[accept.length];
        for (int i = 0; i < accept.length; i++) this.accept[i] = Pattern.compile(accept[i]);
      }

      @Override
      protected boolean handle(String key) {
        for (Pattern pattern : accept) if (pattern.matcher(key).find()) return true;
        return false;
      }

      @Override
      public String toString() {
        return String.format(
            "%d\t%s\t%s: %s", priority, id, Arrays.toString(accept), clazz.getCanonicalName());
      }
    };
  }
}
