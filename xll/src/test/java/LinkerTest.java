import edu.pku.code2graph.model.Language;
import edu.pku.code2graph.xll.*;
import org.junit.jupiter.api.Test;
import edu.pku.code2graph.model.URI;

import java.util.*;

public class LinkerTest {
  @Test
  public void matchTest() {
    URI uri = new URI("use:///Code2Graph/client/build/resources/test/android/butterknife/main/java/com/example/demo/MainActivity.java//R.id.button");
    Optional<Config> config = new ConfigLoader().load("src/main/resources/config.yml");
    config.ifPresent(value -> {
      Rule rule = value.getRules().get(1);
      URIPattern left = rule.getLeft();
      System.out.println(left);
      System.out.println(uri);
      System.out.println(left.match(uri));
    });
  }

  @Test
  public void generalTest() {
    Optional<Config> config = new ConfigLoader().load("src/main/resources/config.yml");
    config.ifPresent(value -> {
      Rule rule = value.getRules().get(0);
      URIPattern left = rule.getLeft();
      URI uri1 = new URI("def://foo/bar.java//getFooBar");
      System.out.println(left);
      System.out.println(uri1);
      System.out.println(left.match(uri1));
      System.out.println();

      URIPattern right = rule.getRight();
      URI uri2 = new URI("def://foo/baz.java//Select//#{FooBar}");
      System.out.println(right);
      System.out.println(uri2);
      System.out.println(right.match(uri2));
      System.out.println();

//      List<URI> uris = new ArrayList<>();
//      uris.add(uri1);
//      uris.add(uri2);
//      Map<Language, List<URI>> uriMap = new HashMap<>();
//      uriMap.put(Language.JAVA, uris);
//      System.out.println("Total links: " + new Detector(uriMap).link(rule));
    });
  }
}
