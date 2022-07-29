import edu.pku.code2graph.mining.History;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MiningTest {
  private static Logger logger = LoggerFactory.getLogger(MiningTest.class);

  @Test
  public void testCloudReader() throws Exception {
    new History("android", "CloudReader");
  }

  @Test
  public void testGSYVideoPlayer() throws Exception {
    new History("android", "GSYVideoPlayer");
  }

  @Test
  public void testNewPipe() throws Exception {
    new History("android", "NewPipe");
  }

  @Test
  public void testVirtualXposed() throws Exception {
    new History("android", "VirtualXposed");
  }

  @Test
  public void testXposedInstaller() throws Exception {
    new History("android", "XposedInstaller");
  }
}
