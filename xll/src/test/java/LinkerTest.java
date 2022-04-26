import edu.pku.code2graph.model.Language;
import edu.pku.code2graph.xll.Link;
import edu.pku.code2graph.model.URITree;
import edu.pku.code2graph.xll.*;
import edu.pku.code2graph.xll.URIPattern;
import org.junit.jupiter.api.Test;

public class LinkerTest {
  static void check(Linker linker, int count) {
    System.out.println(linker.rule.name);
    System.out.println("links: " + linker.links.size());
    for (Link link : linker.links) {
      System.out.println(link);
    }
    System.out.println("captures: " + linker.context.size());
    for (Capture capture : linker.context) {
      System.out.println(capture);
    }
    assert linker.links.size() == count;
  }

  /**
   * - matching algorithm
   * - anchors and symbols
   * - modifier and word slice
   */
  @Test
  public void basicTest() {
    URITree tree = new URITree();
    tree.add("def://main/res/layout/activity_main.xml[language=FILE]"
        + "//RelativeLayout/Button/android:id[language=XML]"
        + "//@+id\\/button_login[language=ANY]");
    tree.add("use://main/java/com/example/demo/MainActivity.java[language=FILE]"
        + "//R.id.buttonLogin[language=JAVA]");

    URIPattern def = new URIPattern(false, "(&layoutName).xml");
    def.addLayer("android:id", Language.XML);
    def.addLayer("@+id\\/(name:snake)");

    URIPattern use = new URIPattern(true, "(javaFile).java");
    use.addLayer("R.id.(name:camel)", Language.JAVA);

    Linker linker = new Linker(tree, def, use);
    linker.link();
    check(linker, 1);
  }

  /**
   * - wildcards
   * - embedding layer
   * - fuzzy matching
   */
  @Test
  public void wildcardTest() {
    URITree tree = new URITree();
    tree.add("def://BlogAdminController.java[language=FILE]"
        + "//model.addAttribute[language=JAVA]"
        + "//post-form[language=ANY]");
    tree.add("use://blog/new.html[language=FILE]"
        + "//html/body/form/data-th-object[language=HTML]"
        + "//${postForm}[language=ANY]");

    URIPattern def = new URIPattern(false, "*.java");
    def.addLayer("model.addAttribute", Language.JAVA);
    def.addLayer("(name)");

    URIPattern use = new URIPattern(true, "*.html");
    use.addLayer("**", Language.HTML);
    use.addLayer("${(name)}");

    Linker linker = new Linker(tree, def, use);
    linker.link();
    check(linker, 1);
  }

  /**
   * - fallback mechanism
   */
  @Test
  public void precisionTest() {
    URITree tree = new URITree();

    tree.add("def://res/layout/list_playlist_mini_item.xml[language=FILE]");
    tree.add("def://res/layout/list_stream_mini_item.xml[language=FILE]");
    tree.add("def://res/layout/list_stream_item.xml[language=FILE]");
    tree.add("def://res/layout/list_stream_grid_item.xml[language=FILE]//androidx.constraintlayout.widget.ConstraintLayout/TextView/android:id[language=XML]//@+id\\/itemUploaderView[language=ANY]");
    tree.add("def://res/layout/list_stream_item.xml[language=FILE]//androidx.constraintlayout.widget.ConstraintLayout/TextView/android:id[language=XML]//@+id\\/itemUploaderView[language=ANY]");
    tree.add("def://res/layout/list_playlist_grid_item.xml[language=FILE]//RelativeLayout/TextView/android:id[language=XML]//@+id\\/itemUploaderView[language=ANY]");
    tree.add("def://res/layout/list_playlist_mini_item.xml[language=FILE]//RelativeLayout/TextView/android:id[language=XML]//@+id\\/itemUploaderView[language=ANY]");
    tree.add("def://res/layout/list_stream_mini_item.xml[language=FILE]//RelativeLayout/TextView/android:id[language=XML]//@+id\\/itemUploaderView[language=ANY]");
    tree.add("def://res/layout/list_playlist_item.xml[language=FILE]//RelativeLayout/TextView/android:id[language=XML]//@+id\\/itemUploaderView[language=ANY]");

    tree.add("use://java/org/schabi/newpipe/local/holder/PlaylistItemHolder.java[language=FILE]//R.layout.list_playlist_mini_item[language=JAVA]");
    tree.add("use://java/org/schabi/newpipe/settings/SelectPlaylistFragment.java[language=FILE]//R.layout.list_playlist_mini_item[language=JAVA]");
    tree.add("use://java/org/schabi/newpipe/info_list/holder/PlaylistMiniInfoItemHolder.java[language=FILE]//R.layout.list_playlist_mini_item[language=JAVA]");
    tree.add("use://java/org/schabi/newpipe/local/holder/LocalStatisticStreamItemHolder.java[language=FILE]//R.layout.list_stream_item[language=JAVA]");
    tree.add("use://java/org/schabi/newpipe/info_list/holder/StreamInfoItemHolder.java[language=FILE]//R.layout.list_stream_item[language=JAVA]");
    tree.add("use://java/org/schabi/newpipe/info_list/holder/StreamMiniInfoItemHolder.java[language=FILE]//R.layout.list_stream_mini_item[language=JAVA]");
    tree.add("use://java/org/schabi/newpipe/local/holder/PlaylistItemHolder.java[language=FILE]//R.id.itemUploaderView[language=JAVA]");
    tree.add("use://java/org/schabi/newpipe/local/holder/LocalStatisticStreamItemHolder.java[language=FILE]//R.id.itemUploaderView[language=JAVA]");
    tree.add("use://java/org/schabi/newpipe/info_list/holder/StreamMiniInfoItemHolder.java[language=FILE]//R.id.itemUploaderView[language=JAVA]");
    tree.add("use://java/org/schabi/newpipe/info_list/holder/PlaylistMiniInfoItemHolder.java[language=FILE]//R.id.itemUploaderView[language=JAVA]");

    URIPattern def, use;
    def = new URIPattern(false, "(layoutName).xml");
    use = new URIPattern(true, "(javaFile).java");
    use.addLayer("R.layout.(layoutName)", Language.JAVA);

    Linker linker1 = new Linker(tree, def, use);
    linker1.link();
    check(linker1, 6);

    def = new URIPattern(false, "(&layoutName).xml");
    def.addLayer("android:id", Language.XML);
    def.addLayer("@+id\\/(name)");

    use = new URIPattern(true, "(&javaFile).java");
    use.addLayer("R.id.(name)", Language.JAVA);

    Linker linker2 = new Linker(tree, def, use);
    for (Capture variables : linker1.context) {
      linker2.link(variables);
    }
    check(linker2, 4);
  }

  /**
   * - dot and slash
   * - greedy matching
   */
  @Test
  public void greedyTest() {
    URITree tree = new URITree();
    tree.add("def://BlogController.java[language=FILE]"
        + "//showPost/return[language=JAVA]"
        + "//blog.show[language=ANY]");
    tree.add("def://BlogController.java[language=FILE]"
        + "//showPost/model.addAttribute[language=JAVA]"
        + "//categories[language=ANY]");
    tree.add("def://root/blog/show.html[language=FILE]");
    tree.add("use://root/blog/show.html[language=FILE]"
        + "//html/body/form/select/option/data-th-each[language=HTML]"
        + "//${categories}[language=ANY]");

    URIPattern def, use;

    // rule 1
    def = new URIPattern(false, "(htmlFile...).html");

    use = new URIPattern(true, "(javaFile).java");
    use.addLayer("(functionName)/return", Language.JAVA);
    use.addLayer("(htmlFile:dot)");

    Linker linker1 = new Linker(tree, def, use);
    linker1.link();
    check(linker1, 1);

    // rule 2
    def = new URIPattern(false, "(&javaFile).java");
    def.addLayer("(&functionName)/(&modelName).addAttribute", Language.JAVA);
    def.addLayer("(name)");

    use = new URIPattern(true, "(&htmlFile).html");
    use.addLayer("**", Language.HTML);
    use.addLayer("${(name)}");

    Linker linker2 = new Linker(tree, def, use);
    for (Capture variables : linker1.context) {
      linker2.link(variables);
    }
    check(linker2, 1);
  }

  /**
   * - self matching
   * - attribute matching
   */
  @Test
  public void attributeTest() {
    URITree tree = new URITree();
    tree.add("def://com/zheng/upms/dao/model/UpmsSystem.java[language=FILE]"
        + "//UpmsSystem/setStatus/status[language=JAVA]");
    tree.add("def://com/zheng/upms/dao/model/UpmsSystem.java[language=FILE]"
        + "//UpmsSystem/status[language=JAVA]");
    tree.add("use://com/zheng/cms/rpc/mapper/CmsArticleExtMapper.xml[language=FILE]"
        + "//mapper/select[language=XML,resultType=java.lang.Long]"
        + "//Select/Where/=/status[language=ANY]");
    tree.add("use://com/zheng/cms/rpc/mapper/CmsArticleExtMapper.xml[language=FILE]"
        + "//mapper/select[language=XML,resultType=com.zheng.upms.dao.model.UpmsSystem,queryId=countByCategoryId]"
        + "//Select/Where/=/status[language=ANY]");

    URIPattern def = new URIPattern(false, "(resTypePackage...)/(class).java");
    def.addLayer("(class)/(name)", Language.JAVA);

    URIPattern use = new URIPattern(true, "(xmlFile).xml");
    LayerPattern pattern = use.addLayer("select", Language.XML);
    pattern.put("resultType", "(resTypePackage:dot).(class)");
    pattern.put("queryId", "(queryId)");
    use.addLayer("(name)");

    Linker linker = new Linker(tree, def, use);
    linker.link();
    check(linker, 1);
  }

  @Test
  public void flowGraphTest() {
    URITree tree = new URITree();

    tree.add("def://jeecg-boot/jeecg-boot-module-system/src/main/java/org/jeecg/modules/system/mapper/SysUserMapper.java[language=FILE]");
    tree.add("use://jeecg-boot/jeecg-boot-module-system/src/main/java/org/jeecg/modules/system/mapper/SysUserMapper.java[language=FILE]//SysUserMapper/getUserByName[language=JAVA]");
    tree.add("use://jeecg-boot/jeecg-boot-module-system/src/main/java/org/jeecg/modules/system/mapper/SysUserMapper.java[language=FILE]//SysUserMapper/getUserByName/username[varType=String,language=JAVA]");
    tree.add("def://jeecg-boot/jeecg-boot-module-system/src/main/java/org/jeecg/modules/system/mapper/SysUserMapper.java[language=FILE]//SysUserMapper/getUserByName/username/@Param[language=JAVA]//username[language=ANY]");
    tree.add("def://jeecg-boot/jeecg-boot-module-system/target/classes/org/jeecg/modules/system/mapper/xml/SysUserMapper.xml[language=FILE]//mapper/namespace[language=XML]//org.jeecg.modules.system.mapper.SysUserMapper[language=ANY]");
    tree.add("def://jeecg-boot/jeecg-boot-module-system/target/classes/org/jeecg/modules/system/mapper/xml/SysUserMapper.xml[language=FILE]//mapper/select/id[language=XML,resultType=org.jeecg.modules.system.entity.SysUser,queryId=getUserByName]//getUserByName[language=ANY]");
    tree.add("use://jeecg-boot/jeecg-boot-module-system/target/classes/org/jeecg/modules/system/mapper/xml/SysUserMapper.xml[language=FILE]//mapper/select[language=XML,resultType=org.jeecg.modules.system.entity.SysUser,queryId=getUserByName]//Select/Where/=/#{username}[language=ANY]");

    URIPattern def, use;

    // r-mapperBinding
    // <mapper namespace="a.b.c"> <-> a/b/c.java
    def = new URIPattern(false, "(packagePath...).java");

    use = new URIPattern(true, "(xmlFile).xml");
    use.addLayer("mapper/namespace", Language.XML);
    use.addLayer("(packagePath:dot)");

    Linker linker1 = new Linker(tree, def, use);
    linker1.link();
    check(linker1, 1);

    // r-queryId-select
    // mapper/(queryId) [func] <-> <select queryId=(queryId)>
    def = new URIPattern(false, "(&xmlFile).xml");
    def.addLayer("select/id", Language.XML);
    def.addLayer("(queryId)");

    use = new URIPattern(true, "(&packagePath:slash).java");
    use.addLayer("(mapperInterface)/(queryId)", Language.JAVA);

    Linker linker2 = new Linker(tree, def, use);
    for (Capture variables : linker1.context) {
      linker2.link(variables);
    }
    linker2.link();
    check(linker2, 1);

    // r-paramAnno-select-no-jdbc
    // <select> select * from xx=#{name,jdbcType=yy} </select> <-> @Param("name")
    def = new URIPattern(false, "(&packagePath:slash).java");
    def.addLayer("(&mapperInterface)/(&queryId)/(a)/@Param", Language.JAVA);
    def.addLayer("(name)");

    use = new URIPattern(true, "(&xmlFile).xml");
    LayerPattern layer = use.addLayer("select", Language.XML);
    layer.put("queryId", "(&queryId)");
    use.addLayer("#{(name)}", Language.SQL);

    Linker linker3 = new Linker(tree, def, use);
    for (Capture variables : linker2.context) {
      linker3.link(variables);
    }
    linker3.link();
    check(linker3, 1);

    // r-paramVarname-select-no-jdbc
    // <select> select * from xx=#{name,jdbcType=yy} </select> <-> func(int name)
    def = new URIPattern(false, "(&packagePath:slash).java");
    def.addLayer("(&mapperInterface)/(&queryId)/(name)", Language.JAVA);

    Linker linker4 = new Linker(tree, def, use);
    for (Capture variables : linker2.context) {
      linker4.link(variables);
    }
    linker4.link();
    check(linker4, 1);
  }
}
