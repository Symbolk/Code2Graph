import edu.pku.code2graph.client.model.RenameResult;
import edu.pku.code2graph.client.model.RenameStatusCode;
import edu.pku.code2graph.model.Range;
import edu.pku.code2graph.model.URI;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static edu.pku.code2graph.client.Rename.calcRenameResult;
import static edu.pku.code2graph.client.Rename.xmlFindRef;

import static org.assertj.core.api.Assertions.assertThat;

public class RenameTest {
  private static String framework = "android";
  private static String repoName = "XposedInstaller";
  private static String configPath =
      System.getProperty("user.dir") + "/src/main/resources/" + framework + "/config.yml";
  private static String repoPath =
      System.getProperty("user.home") + "/coding/xll/" + framework + "/" + repoName;
  private static String cachePath =
      System.getProperty("user.home") + "/coding/xll/cache/" + framework + "/" + repoName;

  @Test
  public void testXmlInnerLink() throws IOException {
    URI toMatchRef =
        new URI(
            "def://app/src/main/res/layout/icon_preference_item.xml[language=FILE]"
                + "//RelativeLayout/ImageView/android:id[language=XML]//@+id\\/icon[language=ANY]");
    List<Pair<URI, Range>> resRef = xmlFindRef(repoPath, toMatchRef, cachePath);
    System.out.println(resRef);

    URI toMatchInclude =
        new URI(
            "def://app/src/main/res/layout/activity_welcome.xml[language=FILE]//android.support.v4.widget.DrawerLayout"
                + "/android.support.design.widget.NavigationView/android:id[language=XML]//@+id\\/toolbar_elevation[language=ANY]");
    List<Pair<URI, Range>> resInclude = xmlFindRef(repoPath, toMatchInclude, cachePath);
    System.out.println(resInclude);
  }

  @Test
  public void testRename() {
    repoName = "NewPipe";
    configPath =
        System.getProperty("user.dir") + "/src/main/resources/" + framework + "/config.yml";
    repoPath = System.getProperty("user.home") + "/coding/xll/" + framework + "/" + repoName;
    cachePath = System.getProperty("user.home") + "/coding/xll/cache/" + framework + "/" + repoName;

    Range range = new Range("73:20~73:45", "app/src/main/res/layout/list_stream_playlist_item.xml");
    RenameResult res =
        calcRenameResult(
            repoPath,
            cachePath,
            "@+id\\/itemAdditionalDetails",
            range,
            "@+id\\/itemAdditionalAny",
            configPath);
    assertThat(res.getStatus()).isEqualTo(RenameStatusCode.SUCCESS);
    System.out.println(res.getRenameInfoList());
  }
}
