import edu.pku.code2graph.model.Language;
import edu.pku.code2graph.model.URI;
import edu.pku.code2graph.model.URITree;
import edu.pku.code2graph.xll.Project;
import edu.pku.code2graph.xll.Rule;
import edu.pku.code2graph.xll.URIPattern;
import org.junit.jupiter.api.Test;

public class RenameTest {
  @Test
  public void test1() {
    URITree tree = new URITree();
    URI uri1 = new URI("def://main/res/layout/activity_main.xml[language=FILE]"
        + "//RelativeLayout/Button/android:id[language=XML]"
        + "//@+id\\/button_login[language=ANY]");
    tree.add(uri1);
    tree.add("use://main/java/com/example/demo/MainActivity.java[language=FILE]"
        + "//R.id.buttonLogin[language=JAVA]");

    URIPattern def = new URIPattern(false, "(&layoutName).xml");
    def.addLayer("android:id", Language.XML);
    def.addLayer("@+id\\/(name:snake)");

    URIPattern use = new URIPattern(true, "(javaFile).java");
    use.addLayer("R.id.(name:camel)", Language.JAVA);

    Project project = new Project();
    project.setTree(tree);
    project.addRule(new Rule(def, use));
    project.link();

    project.rename(uri1, uri1);
  }
}
