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

    project.link();
    renames = project.rename(uri2, new URI("use://main/java/com/example/demo/MainActivity.java[language=FILE]"
        + "//R.id.buttonLogoutView[language=JAVA]"));
    System.out.println(renames);
  }

  @Test
  public void test2() {
    URITree tree = new URITree();
    URI uri1 = new URI("def://BlogController.java[language=FILE]"
        + "//showPost/return[language=JAVA]"
        + "//blog\\/show[language=ANY]");
    URI uri2 = new URI("def://root/blog/show.html[language=FILE]");
    tree.add(uri1);
    tree.add(uri2);

    URIPattern def = new URIPattern(false, "(htmlFile...).html");

    URIPattern use = new URIPattern(true, "(javaFile).java");
    use.addLayer("(functionName)/return", Language.JAVA);
    use.addLayer("(htmlFile)");

    Project project = new Project();
    project.setTree(tree);
    project.addRule(new Rule(def, use));

    List<Pair<URI, URI>> renames;

    project.link();
    renames = project.rename(uri1, new URI("def://BlogController.java[language=FILE]"
        + "//showPost/return[language=JAVA]"
        + "//blog\\/path\\/showLogin[language=ANY]"));
    System.out.println(renames);
  }

  @Test
  public void test3() {
    URITree tree = new URITree();
    URI uri1 = new URI("def://app/src/main/res/layout/item_everyday_three.xml[language=FILE]"
        + "//layout/LinearLayout/LinearLayout/android:id[language=XML]"
        + "//@+id\\\\/ll_three_one_three[language=ANY]");
    URI uri2 = new URI("use://app/src/main/java/com/example/jingbin/cloudreader/adapter/EverydayAdapter.java[language=FILE]"
        + "//EverydayAdapter/ThreeHolder/onBindingView/setOnClick/binding.llThreeOneThree[language=JAVA]");
    tree.add(uri1);
    tree.add(uri2);

    URIPattern def = new URIPattern(false, "(&layoutName).xml");
    def.addLayer("android:id", Language.XML);
    def.addLayer("@+id\\/(name)");

    URIPattern use = new URIPattern(true, "(&javaFile).java");
    use.addLayer("(&bindingVar).(name:camel)", Language.JAVA);

    Project project = new Project();
    project.setTree(tree);
    project.addRule(new Rule(def, use));

    List<Pair<URI, URI>> renames;

    project.link();
    renames = project.rename(uri2, new URI("use://app/src/main/java/com/example/jingbin/cloudreader/adapter/EverydayAdapter.java[language=FILE]"
        + "//EverydayAdapter/ThreeHolder/onBindingView/setOnClick/binding.llThreeOneThird[language=JAVA]"));
    System.out.println(renames);
  }

  @Test
  public void test4() {
    URITree tree = new URITree();

    URI uri0 = new URI("def://app/src/main/res/layout/fragment_description.xml[language=FILE]"
        + "//androidx.core.widget.NestedScrollView/androidx.constraintlayout.widget.ConstraintLayout/TextView/app:layout_constraintEnd_toStartOf[language=XML]"
        + "//@+id\\\\/detail_select_description_button[language=ANY]");
    URI uri1 = new URI("def://app/src/main/res/layout/fragment_description.xml[language=FILE]"
        + "//androidx.core.widget.NestedScrollView/androidx.constraintlayout.widget.ConstraintLayout/ImageView/android:id[language=XML]"
        + "//@+id\\\\/detail_select_description_button[language=ANY]");
    URI uri2 = new URI("use://app/src/main/java/org/schabi/newpipe/fragments/detail/DescriptionFragment.java[language=FILE]"
        + "//DescriptionFragment/disableDescriptionSelection/TooltipCompat.setTooltipText/binding.detailSelectDescriptionButton[language=JAVA]");
    tree.add(uri0);
    tree.add(uri1);
    tree.add(uri2);

    Project project = new Project();
    project.setTree(tree);

    URIPattern def = new URIPattern(false, "(&layoutName).xml");
    def.addLayer("android:id", Language.XML);
    def.addLayer("@+id\\/(name)");

    URIPattern use = new URIPattern(true, "(&javaFile).java");
    use.addLayer("(&bindingVar).(name:camel)", Language.JAVA);

    project.addRule(new Rule(def, use));

    def = new URIPattern(false, "(&layoutName).xml");
    def.addLayer("android:id", Language.XML);
    def.addLayer("@+id\\/(name)");

    use = new URIPattern(true, "(&layoutName).xml");
    use.addLayer("*constraint*", Language.XML);
    use.addLayer("@+id\\/(name)");

    project.addRule(new Rule(def, use));

    List<Pair<URI, URI>> renames;

    project.link();
    renames = project.rename(uri2, new URI("use://app/src/main/java/org/schabi/newpipe/fragments/detail/DescriptionFragment.java[language=FILE]"
        + "//DescriptionFragment/disableDescriptionSelection/TooltipCompat.setTooltipText/binding.detailedSelectDescriptionButton[language=JAVA]"));
    System.out.println(renames.size());
    System.out.println(renames.get(0));
    System.out.println(renames.get(1));
  }
}
