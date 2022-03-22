import edu.pku.code2graph.model.URI;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class URITest {
  @Test
  public void testBuildFromSource() {
    String source =
        "def://src/main/java/org/jeecgframework/web/system/sms/util/CMPPSenderUtil.java[language=FILE]//sendDifferenceNetMsg/return[language=JAVA]//abc[language=ANY,varType=String]";
    URI uri = new URI(source);
    assertThat(uri.getLayerCount()).isEqualTo(3);
    assertThat(uri.getLayer(2).getAttribute("varType")).isEqualTo("String");
    assertThat(uri.getLayer(2).getAttribute("language")).isEqualTo("ANY");
    assertThat(uri.getLayer(1).getAttribute("language")).isEqualTo("JAVA");
    assertThat(uri.getLayer(0).getAttribute("language")).isEqualTo("FILE");
  }
}
