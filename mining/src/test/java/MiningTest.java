import edu.pku.code2graph.exception.InvalidRepoException;
import edu.pku.code2graph.exception.NonexistPathException;
import edu.pku.code2graph.model.URITree;
import edu.pku.code2graph.cache.CacheHandler;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class MiningTest {
  private static Logger logger = LoggerFactory.getLogger(MiningTest.class);

  private void test(String framework, String repoName) throws Exception {
    String cacheDir = System.getProperty("user.home") + "/coding/xll/sha-history/" + framework + "/" + repoName;
    Map<String, Collection<String>> result = CacheHandler.loadForAll(framework, repoName, cacheDir);
    Set<String> files = new HashSet<>();
    for (Collection<String> hashes : result.values()) {
      files.addAll(hashes);
    }
    System.out.println(result.size());
    System.out.println(files.size());
    Set<String> tree = new HashSet<>();
    CacheHandler.loadCache(cacheDir, tree);
    System.out.println(tree.size());
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
