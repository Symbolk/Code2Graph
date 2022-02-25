import edu.pku.code2graph.model.Language;
import edu.pku.code2graph.model.URITree;
import edu.pku.code2graph.xll.*;
import org.junit.jupiter.api.Test;
import edu.pku.code2graph.model.URI;

import java.io.IOException;

public class LinkerTest {
  @Test
  public void matchTest1() {
    URITree tree = new URITree();
    tree.add("def://main/res/layout/activity_main.xml//RelativeLayout/Button/android:id//@+id\\/button");
    tree.add("use://main/java/com/example/demo/MainActivity.java//R.id.button");

    URIPattern def = new URIPattern(false, "(&layoutName).xml");
    def.addLayer("android:id", Language.XML);
    def.addLayer("@+id\\/(name)");

    URIPattern use = new URIPattern(true, "(javaFile).java");
    use.addLayer("R.id.(name)", Language.JAVA);

    Linker linker = new Linker(tree, def, use);
    linker.link();
    System.out.println(linker.links);
    System.out.println(linker.captures);
  }

  @Test
  public void matchTest2() {
    URITree tree = new URITree();
    tree.add("def://BlogAdminController.java//.addAttribute//post-form");
    tree.add("use://blog/new.html//html/body/form/data-th-object//${postForm}");

    URIPattern def = new URIPattern(false, "*.java");
    def.addLayer(".addAttribute", Language.JAVA);
    def.addLayer("(name)");

    URIPattern use = new URIPattern(true, "*.html");
    use.addLayer("**", Language.HTML);
    use.addLayer("${(name)}");

    Linker linker = new Linker(tree, def, use);
    linker.link();
    System.out.println(linker.links);
    System.out.println(linker.captures);
  }

  @Test
  public void matchTest3() {
    URITree tree = new URITree();

    tree.add("def://res/layout/list_playlist_mini_item.xml");
    tree.add("def://res/layout/list_stream_mini_item.xml");
    tree.add("def://res/layout/list_stream_item.xml");
    tree.add("def://res/layout/list_stream_grid_item.xml//androidx.constraintlayout.widget.ConstraintLayout/TextView/android:id//@+id\\/itemUploaderView");
    tree.add("def://res/layout/list_stream_item.xml//androidx.constraintlayout.widget.ConstraintLayout/TextView/android:id//@+id\\/itemUploaderView");
    tree.add("def://res/layout/list_playlist_grid_item.xml//RelativeLayout/TextView/android:id//@+id\\/itemUploaderView");
    tree.add("def://res/layout/list_playlist_mini_item.xml//RelativeLayout/TextView/android:id//@+id\\/itemUploaderView");
    tree.add("def://res/layout/list_stream_mini_item.xml//RelativeLayout/TextView/android:id//@+id\\/itemUploaderView");
    tree.add("def://res/layout/list_playlist_item.xml//RelativeLayout/TextView/android:id//@+id\\/itemUploaderView");

    tree.add("use://java/org/schabi/newpipe/local/holder/PlaylistItemHolder.java//R.layout.list_playlist_mini_item");
    tree.add("use://java/org/schabi/newpipe/settings/SelectPlaylistFragment.java//R.layout.list_playlist_mini_item");
    tree.add("use://java/org/schabi/newpipe/info_list/holder/PlaylistMiniInfoItemHolder.java//R.layout.list_playlist_mini_item");
    tree.add("use://java/org/schabi/newpipe/local/holder/LocalStatisticStreamItemHolder.java//R.layout.list_stream_item");
    tree.add("use://java/org/schabi/newpipe/info_list/holder/StreamInfoItemHolder.java//R.layout.list_stream_item");
    tree.add("use://java/org/schabi/newpipe/info_list/holder/StreamMiniInfoItemHolder.java//R.layout.list_stream_mini_item");
    tree.add("use://java/org/schabi/newpipe/local/holder/PlaylistItemHolder.java//R.id.itemUploaderView");
    tree.add("use://java/org/schabi/newpipe/local/holder/LocalStatisticStreamItemHolder.java//R.id.itemUploaderView");
    tree.add("use://java/org/schabi/newpipe/info_list/holder/StreamMiniInfoItemHolder.java//R.id.itemUploaderView");
    tree.add("use://java/org/schabi/newpipe/info_list/holder/PlaylistMiniInfoItemHolder.java//R.id.itemUploaderView");

    URIPattern def, use;
    def = new URIPattern(false, "(layoutName).xml");
    use = new URIPattern(true, "(javaFile).java");
    use.addLayer("R.layout.(layoutName)", Language.JAVA);

    Linker linker1 = new Linker(tree, def, use);
    linker1.link();
    linker1.print();

    def = new URIPattern(false, "(&layoutName).xml");
    def.addLayer("android:id", Language.XML);
    def.addLayer("@+id\\/(name)");

    use = new URIPattern(true, "(&javaFile).java");
    use.addLayer("R.id.(name)", Language.JAVA);

    Linker linker2 = new Linker(tree, def, use);
    for (Capture variables : linker1.captures) {
      linker2.link(variables);
    }
    linker2.link();
    linker2.print();
  }

  @Test
  public void matchTest4() {
    URITree tree = new URITree();
    tree.add("def://foo/bar/baz/qux.html");
    tree.add("use://source.java//return//bar.baz");

    URIPattern def = new URIPattern(false, "(name...)/*.html");

    URIPattern use = new URIPattern(true, "*.java");
    use.addLayer("return", Language.JAVA);
    use.addLayer("(name:dot)");

    Linker linker = new Linker(tree, def, use);
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
  }
}
