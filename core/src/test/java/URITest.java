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
    assertThat(uri.getLayer(2).get("varType")).isEqualTo("String");
    assertThat(uri.getLayer(2).get("language")).isEqualTo("ANY");
    assertThat(uri.getLayer(1).get("language")).isEqualTo("JAVA");
    assertThat(uri.getLayer(0).get("language")).isEqualTo("FILE");
  }

  @Test
  public void testMultiAttr() {
    String source =
        "use://zheng-cms/zheng-cms-rpc-service/target/classes/com/zheng/cms/dao/mapper/CmsTopicMapper.xml[language=FILE]//mapper/update[parameterType=com.zheng.cms.dao.model.CmsTopic,language=XML,queryId=updateByPrimaryKey]//Update/Where/=/#{topicId,jdbcType=INTEGER}[language=ANY]";
    URI uri = new URI(source);
    assertThat(uri.getLayer(1).getAttribute("parameterType")).isEqualTo("com.zheng.cms.dao.model.CmsTopic");
    assertThat(uri.getLayer(1).getAttribute("language")).isEqualTo("XML");
    assertThat(uri.getLayer(1).getAttribute("queryId")).isEqualTo("updateByPrimaryKey");
  }
}
