import edu.pku.code2graph.mining.HistoryLoader;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MiningTest {
  private static Logger logger = LoggerFactory.getLogger(MiningTest.class);

  @Test
  public void testCloudReader() throws Exception {
    new HistoryLoader("android", "CloudReader");
  }

  @Test
  public void testGSYVideoPlayer() throws Exception {
    new HistoryLoader("android", "GSYVideoPlayer");
  }

  @Test
  public void testNewPipe() throws Exception {
    new HistoryLoader("android", "NewPipe");
  }

  @Test
  public void testVirtualXposed() throws Exception {
    new HistoryLoader("android", "VirtualXposed");
  }

  @Test
  public void testXposedInstaller() throws Exception {
    new HistoryLoader("android", "XposedInstaller");
  }
}
