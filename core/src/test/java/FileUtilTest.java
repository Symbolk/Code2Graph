import edu.pku.code2graph.util.FileUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FileUtilTest {
  @Test
  public void testGetRelativePath() {
    String absolutePath =
        "/Users/symbolk/coding/dev/Code2Graph/core/src/test/java/FileUtilTest.java";
    String rootPath = "/Users/symbolk/coding/dev/Code2Graph";
    String relativePath = "core/src/test/java/FileUtilTest.java";
    assertThat(FileUtil.getRelativePath(absolutePath, rootPath)).isEqualTo(relativePath);
  }

  @Test
  public void testGetParentFolder() {
    String absolutePath =
        "/Users/symbolk/coding/data/repos/LeafPic/app/src/main/res/layout/activity_about.xml";
    String parentFolder = "layout";
    assertThat(FileUtil.getParentFolderName(absolutePath)).isEqualTo(parentFolder);
  }
}
