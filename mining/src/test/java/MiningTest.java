import edu.pku.code2graph.exception.InvalidRepoException;
import edu.pku.code2graph.exception.NonexistPathException;
import edu.pku.code2graph.model.URITree;
import edu.pku.code2graph.cache.CacheHandler;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class MiningTest {
  private static Logger logger = LoggerFactory.getLogger(MiningTest.class);

  private void test(String framework, String repoName) throws Exception {
    String cacheDir = System.getProperty("user.home") + "/coding/xll/sha-history/" + framework + "/" + repoName;
    Map<String, Collection<String>> result = CacheHandler.loadForAll("android", "CloudReader", cacheDir);
    System.out.println(result.size());
  }

  @Test
  public void testCloudReader() throws Exception {
    test("android", "CloudReader");
  }

  @Test
  public void testGSYVideoPlayer() throws Exception {
    test("android", "GSYVideoPlayer");
  }

  @Test
  public void testNewPipe() throws Exception {
    test("android", "NewPipe");
  }

  @Test
  public void testVirtualXposed() throws Exception {
    test("android", "VirtualXposed");
  }

  @Test
  public void testXposedInstaller() throws Exception {
    test("android", "XposedInstaller");
  }
}
