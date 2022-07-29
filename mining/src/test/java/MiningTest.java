import edu.pku.code2graph.mining.HistoryLoader;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class MiningTest {
  private static Logger logger = LoggerFactory.getLogger(MiningTest.class);

  private void testHistory(String framework, String repoName) throws IOException {
    HistoryLoader loader = new HistoryLoader(framework, repoName);
    loader.loadAll();
    System.out.println(loader.analyzer.positive);
  }

  private void testHistory(String framework, String repoName, String head) throws IOException {
    HistoryLoader loader = new HistoryLoader(framework, repoName);
    loader.diffCommit(head, 0);
    System.out.println(loader.analyzer.positive);
  }

  @Test
  public void testCloudReader() throws Exception {
    testHistory("android", "CloudReader", "3cc129d9d39397c6732e213f4deb69832d86db3c");
  }

  @Test
  public void testGSYVideoPlayer() throws Exception {
    testHistory("android", "GSYVideoPlayer");
  }

  @Test
  public void testNewPipe() throws Exception {
    testHistory("android", "NewPipe");
  }

  @Test
  public void testVirtualXposed() throws Exception {
    testHistory("android", "VirtualXposed");
  }

  @Test
  public void testXposedInstaller() throws Exception {
    testHistory("android", "XposedInstaller");
  }
}
