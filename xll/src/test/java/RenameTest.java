import edu.pku.code2graph.model.Language;
import edu.pku.code2graph.model.URI;
import edu.pku.code2graph.model.URITree;
import edu.pku.code2graph.xll.*;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.util.List;

public class RenameTest {
  @Test
  public void test1() {
    URITree tree = new URITree();
    URI uri1 = new URI("def://main/res/layout/activity_main.xml[language=FILE]"
        + "//RelativeLayout/Button/android:id[language=XML]"
        + "//@+id\\/button_login[language=ANY]");
    URI uri2 = new URI("use://main/java/com/example/demo/MainActivity.java[language=FILE]"
        + "//R.id.buttonLogin[language=JAVA]");
    tree.add(uri1);
    tree.add(uri2);

    URIPattern def = new URIPattern(false, "(&layoutName).xml");
    def.addLayer("android:id", Language.XML);
    def.addLayer("@+id\\/(name:snake)");

    URIPattern use = new URIPattern(true, "(javaFile).java");
    use.addLayer("R.id.(name:camel)", Language.JAVA);

    Project project = new Project();
    project.setTree(tree);
    project.addRule(new Rule(def, use));

    List<Pair<URI, URI>> renames;
//    project.link();
//    renames = project.rename(uri1, new URI("def://main/res/layout/activity_main.xml[language=FILE]"
//        + "//RelativeLayout/Button/android:id[language=XML]"
//        + "//@+id\\/button_logout[language=ANY]"));
//    System.out.println(renames);

    project.link();
    renames = project.rename(uri2, new URI("use://main/java/com/example/demo/MainActivity.java[language=FILE]"
        + "//R.id.buttonLogout[language=JAVA]"));
    System.out.println(renames);
  }
}
