import edu.pku.code2graph.client.model.RenameResult;
import edu.pku.code2graph.client.model.RenameStatusCode;
import edu.pku.code2graph.model.Range;
import edu.pku.code2graph.util.GraphUtil;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

import static edu.pku.code2graph.cache.CacheHandler.initCache;
import static edu.pku.code2graph.client.Rename.calcRenameResult;

import static edu.pku.code2graph.client.Rename.updateCache;
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
  public void testInit() throws IOException, ParserConfigurationException, SAXException {
    initCache(framework, repoPath, cachePath);
  }

  @Test
  public void testUpdate() throws IOException, ParserConfigurationException, SAXException {
    updateCache(
        repoPath,
        repoPath
            + "/app/src/main/java/de/robv/android/xposed/installer/DownloadDetailsActivity.java.csv",
        cachePath);
  }

  @Test
  public void testRename1() throws IOException, ParserConfigurationException, SAXException {
    repoName = "NewPipe";
    configPath =
        System.getProperty("user.dir") + "/src/main/resources/" + framework + "/config.yml";
    repoPath = System.getProperty("user.home") + "/coding/xll/" + framework + "/" + repoName;
    cachePath = System.getProperty("user.home") + "/coding/xll/cache/" + framework + "/" + repoName;

    File cacheDir = new File(cachePath);
    if (!cacheDir.exists()) {
      initCache(framework, repoPath, cachePath);
    }

    GraphUtil.clearGraph();

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

  @Test
  public void testRename2() throws IOException, ParserConfigurationException, SAXException {
    repoName = "GSYVideoPlayer";
    configPath =
        System.getProperty("user.dir") + "/src/main/resources/" + framework + "/config.yml";
    repoPath = System.getProperty("user.home") + "/coding/xll/" + framework + "/" + repoName;
    cachePath = System.getProperty("user.home") + "/coding/xll/cache/" + framework + "/" + repoName;

    File cacheDir = new File(cachePath);
    if (!cacheDir.exists()) {
      initCache(framework, repoPath, cachePath);
    }

    GraphUtil.clearGraph();

    Range range =
        new Range(
            "51:28~51:52", "gsyVideoPlayer-java/src/main/res/layout/video_progress_dialog.xml");
    RenameResult res =
        calcRenameResult(
            repoPath,
            cachePath,
            "@+id\\/duration_progressbar",
            range,
            "@+id\\/duration_progress_bar",
            configPath);
    assertThat(res.getStatus()).isEqualTo(RenameStatusCode.SUCCESS);
    System.out.println(res.getRenameInfoList());
  }

  @Test
  public void testRename3() throws IOException, ParserConfigurationException, SAXException {
    repoName = "XposedInstaller";
    configPath =
        System.getProperty("user.dir") + "/src/main/resources/" + framework + "/config.yml";
    repoPath = System.getProperty("user.home") + "/coding/xll/" + framework + "/" + repoName;
    cachePath = System.getProperty("user.home") + "/coding/xll/cache/" + framework + "/" + repoName;

    File cacheDir = new File(cachePath);
    if (!cacheDir.exists()) {
      initCache(framework, repoPath, cachePath);
    }

    GraphUtil.clearGraph();

    Range range = new Range("9:20~9:31", "app/src/main/res/layout/toolbar.xml");
    RenameResult res =
        calcRenameResult(
            repoPath, cachePath, "@+id\\/toolbar", range, "@+id\\/tool_bar", configPath);
    assertThat(res.getStatus()).isEqualTo(RenameStatusCode.SUCCESS);
    System.out.println(res.getRenameInfoList());
  }
}
