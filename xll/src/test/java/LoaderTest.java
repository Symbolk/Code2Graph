import edu.pku.code2graph.xll.Project;
import org.junit.jupiter.api.Test;

public class LoaderTest {
  @Test
  public void loaderTest() throws Exception {
    Project config = Project.load("src/test/resources/mybatis.yml");
    System.out.println(config);
  }
}
