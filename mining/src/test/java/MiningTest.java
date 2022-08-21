import edu.pku.code2graph.cache.HistoryLoader;
import edu.pku.code2graph.mining.Analyzer;
import edu.pku.code2graph.mining.Candidate;
import edu.pku.code2graph.mining.Credit;
import edu.pku.code2graph.mining.Exporter;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public class MiningTest {
  private static Logger logger = LoggerFactory.getLogger(MiningTest.class);

  private void testHistory(String framework, String repoName) throws IOException {
    String path = System.getProperty("user.home") + "/coding/xll/data/" + framework + "/" + repoName + "/config.yml";
    HistoryLoader history = new HistoryLoader(framework, repoName);
    Analyzer analyzer = new Analyzer(history);
    analyzer.analyzeAll();
    Exporter exporter = new Exporter(analyzer);
    exporter.exportToFile(path);
  }

  private void testHistory(String framework, String repoName, String head) throws IOException {
    HistoryLoader history = new HistoryLoader(framework, repoName);
    Analyzer analyzer = new Analyzer(history);
    analyzer.analyze(head);
    System.out.println(analyzer.positive);
  }

  @Test
  public void testCloudReader() throws Exception {
    testHistory("android", "CloudReader");
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
