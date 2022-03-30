import edu.pku.code2graph.model.URI;
import edu.pku.code2graph.xll.Config;
import edu.pku.code2graph.xll.Rule;
import edu.pku.code2graph.xll.URIPattern;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class ConfigTest {
  @Test
  public void loaderTest() throws IOException {
    Config config = Config.load("src/main/resources/config.yml");
    Rule rule = config.getRules().get("rule1");
    URIPattern left = rule.def;
    URI uri1 = new URI("def://foo/bar.java//getFooBar");
    System.out.println(left);
    System.out.println(uri1);
    System.out.println(left.match(uri1));
    System.out.println();

    URIPattern right = rule.use;
    URI uri2 = new URI("def://foo/baz.java//Select//#{FooBar}");
    System.out.println(right);
    System.out.println(uri2);
    System.out.println(right.match(uri2));
    System.out.println();
  }
}
