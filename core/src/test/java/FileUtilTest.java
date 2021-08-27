import edu.pku.code2graph.model.Language;
import edu.pku.code2graph.util.FileUtil;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

public class FileUtilTest {
  @Test
  public void testGetRelativePath() {
    String absolutePath =
        System.getProperty("user.home")
            + "/coding/dev/Code2Graph/core/src/test/java/FileUtilTest.java";
    String rootPath = System.getProperty("user.home") + "/coding/dev/Code2Graph";
    String relativePath = "core/src/test/java/FileUtilTest.java";
    assertThat(FileUtil.getRelativePath(absolutePath, rootPath)).isEqualTo(relativePath);
  }

  @Test
  public void testGetParentFolder() {
    String absolutePath =
        System.getProperty("user.home")
            + "/coding/data/repos/LeafPic/app/src/main/res/layout/activity_about.xml";
    String parentFolder = "layout";
    assertThat(FileUtil.getParentFolderName(absolutePath)).isEqualTo(parentFolder);
  }

  @Test
  public void testListFilePathsInLanguages() {
    String path =
        System.getProperty("user.dir").replace("core", "")
            + "client/src/test/resources/android/butterknife/main";
    Set<Language> languages = new HashSet(Arrays.asList(Language.JAVA, Language.XML));
    Map<String, List<String>> result = FileUtil.listFilePathsInLanguages(path, languages);
    assertThat(result.size()).isEqualTo(2);
    assertThat(result.get("java").size()).isEqualTo(1);
    assertThat(result.get("xml").size()).isEqualTo(5);
  }
}
