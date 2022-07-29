import edu.pku.code2graph.cache.HistoryLoader;
import edu.pku.code2graph.mining.Analyzer;
import edu.pku.code2graph.mining.Candidate;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

public class MiningTest {
  private static Logger logger = LoggerFactory.getLogger(MiningTest.class);

  private void testHistory(String framework, String repoName) throws IOException {
    HistoryLoader history = new HistoryLoader(framework, repoName);
    Analyzer analyzer = new Analyzer(history);
    analyzer.analyzeAll();
    System.out.println("cochanges: " + analyzer.cochanges);
    System.out.println("candidates: " + analyzer.graph.size());
    Optional<Map.Entry<Candidate, Double>> entry = analyzer.graph.entrySet().stream().max((o1, o2) -> {
      return (int) Math.signum(o1.getValue() - o2.getValue());
    });
    entry.ifPresent(e -> {
      System.out.println(e.getKey().toString() + ": " + e.getValue());
    });
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
