import edu.pku.code2graph.model.Language;
import edu.pku.code2graph.model.URITree;
import edu.pku.code2graph.xll.*;
import org.junit.jupiter.api.Test;
import edu.pku.code2graph.model.URI;

import java.io.IOException;
import java.util.*;

public class LinkerTest {
  @Test
  public void matchTest1() {
    URITree tree = new URITree();
    tree.add("def://main/res/layout/activity_main.xml//RelativeLayout/Button/android:id//@+id\\/button");
    tree.add("use://main/java/com/example/demo/MainActivity.java//R.id.button");

    URIPattern def = new URIPattern(false, "*.xml");
    def.addLayer("android:id", Language.XML);
    def.addLayer("@+id\\/(name)");

    URIPattern use = new URIPattern(true, "*.java");
    use.addLayer("R.id.(name)", Language.JAVA);

    Rule rule = new Rule(def, use);
    Linker linker = new Linker(rule, tree);
    linker.link();
    System.out.println(linker.links);
    System.out.println(linker.captures);
  }

  @Test
  public void matchTest2() {
    URITree tree = new URITree();
    tree.add("def://BlogAdminController.java//.addAttribute//postForm");
    tree.add("use://blog/new.html//html/body/form/data-th-object//${postForm}");

    URIPattern def = new URIPattern(false, "*.java");
    def.addLayer(".addAttribute", Language.JAVA);
    def.addLayer("(name)");

    URIPattern use = new URIPattern(true, "*.html");
    use.addLayer("**", Language.HTML);
    use.addLayer("${(name)}");

    Rule rule = new Rule(def, use);
    Linker linker = new Linker(rule, tree);
    linker.link();
    System.out.println(linker.links);
    System.out.println(linker.captures);
  }

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
  };
}
