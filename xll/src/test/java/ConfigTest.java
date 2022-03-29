import edu.pku.code2graph.xll.Config;
import org.junit.jupiter.api.Test;

public class ConfigTest {
  @Test
  public void loaderTest() throws Exception {
    Config.load("src/test/resources/mybatis.yml");
  }
}
