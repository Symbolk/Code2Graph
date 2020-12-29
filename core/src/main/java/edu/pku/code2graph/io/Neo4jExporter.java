package edu.pku.code2graph.io;

import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.Node;
import org.jgrapht.Graph;
import org.neo4j.driver.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/** Connect to Neo4j database and export the graph into it */
public class Neo4jExporter {
  public static void export(Graph<Node, Edge> graph) {
    var config = loadConfig("neo4j.properties");

    Set<Node> nodes = graph.vertexSet();
    Set<Edge> edges = graph.edgeSet();
    // load config from properties file
    // connect to db instance
    try (Driver driver =
            GraphDatabase.driver(
                config.getProperty("url"),
                AuthTokens.basic(config.getProperty("user"), config.getProperty("passwd")));
        Session session = driver.session()) {
      Transaction transaction = session.beginTransaction();
      // run actual operations to create
      Map<String, Object> params = new HashMap<>();
      //      params.put("operation", node.getName());
      Result result =
          session.run(
              "CREATE (a:Action {operation: $operation, nodeType: $nodeType, label:$label,"
                  + " startLine:$startLine, endLine: $endLine})",
              params);
      while (result.hasNext()) {
        Record record = result.next();
        System.out.println(record.get("title").asString() + " " + record.get("name").asString());
      }
    }
  }

  public static Properties loadConfig(String filePath) {
    ClassLoader classloader = Thread.currentThread().getContextClassLoader();
    Properties prop = null;
    try (InputStream is = classloader.getResourceAsStream(filePath); ) {
      prop = new Properties();
      prop.load(is);
    } catch (FileNotFoundException fnfe) {
      fnfe.printStackTrace();
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
    return prop;
  }

  public static void main(String[] args) {
    export(null);
  }
}
