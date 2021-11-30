import edu.pku.code2graph.client.Code2Graph;
import edu.pku.code2graph.model.Language;
import edu.pku.code2graph.util.FileUtil;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class LoaderTest {
  private void generateGraph(String repoName, String configName) {
    try {
      String repoPath = FileUtil.getPathFromURL(this.getClass().getResource(repoName));
      String configPath = FileUtil.getPathFromURL(this.getClass().getResource(configName));
      System.out.println("RepoPath: " + repoPath);
      System.out.println("ConfigPath: " + configPath);
      Code2Graph c2g = new Code2Graph(repoName, repoPath, configPath);
      String framework = configName.split("/")[0];
      switch (framework) {
        case "springmvc":
          c2g.addSupportedLanguage(Language.JAVA);
          c2g.addSupportedLanguage(Language.HTML);
          break;
        case "android":
          c2g.addSupportedLanguage(Language.JAVA);
          c2g.addSupportedLanguage(Language.XML);
          break;
        case "mybatis":
          c2g.addSupportedLanguage(Language.JAVA);
          c2g.addSupportedLanguage(Language.XML);
          c2g.addSupportedLanguage(Language.SQL);
          break;
        default:
          c2g.addSupportedLanguage(Language.JAVA);
      }
      c2g.generateGraph();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @BeforeAll
  public static void setUpBeforeAll() {
    BasicConfigurator.configure();
    org.apache.log4j.Logger.getRootLogger().setLevel(Level.DEBUG);
  }

  @Test
  public void butterknifeTest() {
    generateGraph("android/butterknife/main", "android/config.yml");
  }

  @Test
  public void databindingTest() {
    generateGraph("android/databinding/main", "android/config.yml");
  }

  @Test
  public void findviewbyidTest() {
    generateGraph("android/findviewbyid/main", "android/config.yml");
  }

  @Test
  public void newpipeTest() {
    generateGraph("android/newpipe/main", "android/config.yml");
  }

  @Test
  public void viewbindingTest() {
    generateGraph("android/viewbinding/main", "android/config.yml");
  }

  @Test
  public void mybatisJavaTest() {
    generateGraph("mybatis/embedded_in_java/main", "mybatis/config.yml");
  }

  @Test
  public void mybatisXmlTest() {
    generateGraph("mybatis/embedded_in_xml/main", "mybatis/config.yml");
  }

  @Test
  public void saganTest() {
    generateGraph("springmvc/sagan", "springmvc/config.yml");
  }
}
