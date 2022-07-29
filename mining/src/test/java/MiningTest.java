import edu.pku.code2graph.cache.HistoryLoader;
import edu.pku.code2graph.mining.Analyzer;
import edu.pku.code2graph.mining.Candidate;
import edu.pku.code2graph.mining.Credit;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public class MiningTest {
  private static Logger logger = LoggerFactory.getLogger(MiningTest.class);

  private void testHistory(String framework, String repoName) throws IOException {
    HistoryLoader history = new HistoryLoader(framework, repoName);
    Analyzer analyzer = new Analyzer(history);
    analyzer.analyzeAll();
    System.out.println("cochanges: " + analyzer.cochanges);
    System.out.println("candidates: " + analyzer.graph.size());
    Iterator<Map.Entry<Candidate, Credit>> it = analyzer.graph.entrySet().stream().sorted((o1, o2) -> {
      return Double.compare(o2.getValue().value, o1.getValue().value);
    }).iterator();
    for (int i = 0; i < 10; ++i) {
      if (!it.hasNext()) break;
      Map.Entry<Candidate, Credit> entry = it.next();
      System.out.println("- " + entry.getKey().left);
      System.out.println("- " + entry.getKey().right);
      System.out.println("value: " + entry.getValue().value);
      for (Credit.Record record : entry.getValue().history) {
        System.out.println(record);
      }
    }
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
