package edu.pku.code2graph.io;

import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.Node;
import edu.pku.code2graph.model.Type;
import org.jgrapht.Graph;
import org.neo4j.driver.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static org.neo4j.driver.Values.parameters;

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
      // run actual operations to create nodes
      session.writeTransaction(
          new TransactionWork<Void>() {
            @Override
            public Void execute(Transaction tx) {
              return createNodes(tx, nodes);
            }
          });
    }
  }

  private static Void createNodes(Transaction tx, Set<Node> nodes) {
    for (Node node : nodes) {
      tx.run(
          "CREATE (a:Node {id: $id, type: $type, snippet: $snippet})",
          parameters(
              "id", node.getId(), "type", node.getType().name, "snippet", node.getSnippet()));
    }
    return null;
  }

  public static Integer queryByID(Integer id) {
    var config = loadConfig("neo4j.properties");

    try (Driver driver =
            GraphDatabase.driver(
                config.getProperty("url"),
                AuthTokens.basic(config.getProperty("user"), config.getProperty("passwd")));
        Session session = driver.session()) {
      return session.readTransaction(
          new TransactionWork<Integer>() {
            @Override
            public Integer execute(Transaction tx) {
              return matchNode(tx, id);
            }
          });
    }
  }

  public static List<String> queryByType(Type type) {
    var config = loadConfig("neo4j.properties");

    try (Driver driver =
            GraphDatabase.driver(
                config.getProperty("url"),
                AuthTokens.basic(config.getProperty("user"), config.getProperty("passwd")));
        Session session = driver.session()) {
      return session.readTransaction(
          new TransactionWork<List<String>>() {
            @Override
            public List<String> execute(Transaction tx) {
              return matchNodes(tx, type.name);
            }
          });
    }
  }

  private static Integer matchNode(Transaction tx, Integer id) {
    Result result = tx.run("MATCH (a:Node {id: $value}) RETURN a.id", parameters("value", id));
    return result.single().get(0).asInt();
  }

  private static List<String> matchNodes(Transaction tx, String typeName) {
    List<String> names = new ArrayList<>();
    Result result =
        tx.run("MATCH (a:Node {type: $type}) RETURN a.snippet", parameters("type", typeName));
    while (result.hasNext()) {
      Record record = result.next();
      names.add(record.get("snippet").asString());
    }
    return names;
  }

  /**
   * Config files should be carefully located in production
   *
   * @param filePath
   * @return
   */
  private static Properties loadConfig(String filePath) {
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
}
