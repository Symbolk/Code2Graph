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

import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.Node;
import edu.pku.code2graph.util.GraphUtil;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Registry of tree generators, using a singleton pattern.
 *
 * <p>Outermost API provider for graph generator module
 */
public class Generators extends Registry<String, Generator, Register> {

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
    Graph<Node, Edge> graph = GraphUtil.initGraph();

    // a map from generator to file paths
    Map<Generator, List<String>> g2f = new HashMap<>();

    for (String filePath : filePaths) {
      Generator generator = get(filePath);

      if (generator == null) {
        // for now just skip the file that cannot handle
        logger.warn("No generator found for file:{}", filePath);
        continue;
      }

      if (g2f.containsKey(generator)) {
        g2f.get(generator).add(filePath);
      } else {
        List<String> temp = new ArrayList<>();
        temp.add(filePath);
        g2f.put(generator, temp);
      }
    }

    for (Map.Entry<Generator, List<String>> entry : g2f.entrySet()) {
      // TODO how to link the graphs from different generators together as one? (use placeholder
      // nodes in each graph?, id should be unique)
      Graphs.addGraph(graph, entry.getKey().generateFrom().files(entry.getValue()));
    }

    return graph;
  }

  public boolean has(String generator) {
    return this.findById(generator) != null;
  }

  /** Indicate whether or not the given file path has a related tree generator */
  public boolean hasGeneratorForFile(String file) {
    return get(file) != null;
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
