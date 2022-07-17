import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class MiningTest {
  private static Logger logger = LoggerFactory.getLogger(MiningTest.class);

  public void test(String framework, String repoName) throws IOException {
    String cacheDir = System.getProperty("user.home") + "/coding/xll/sha-history/" + framework + "/" + repoName;
    String commitListPath = cacheDir + "/commits.txt";

    FileReader fr = new FileReader(commitListPath);
    BufferedReader br = new BufferedReader(fr);
    String line;
    while ((line = br.readLine()) != null) {
      System.out.println(line);
    }
    br.close();
    fr.close();
  }

  @Test
  public void testCloudReader() throws IOException {
    test("android", "CloudReader");
  }

  @Test
  public void testGSYVideoPlayer() throws IOException {
    test("android", "GSYVideoPlayer");
  }

  @Test
  public void testNewPipe() throws IOException {
    test("android", "NewPipe");
  }

  @Test
  public void testVirtualXposed() throws IOException {
    test("android", "VirtualXposed");
  }

  @Test
  public void testXposedInstaller() throws IOException {
    test("android", "XposedInstaller");
  }
}
