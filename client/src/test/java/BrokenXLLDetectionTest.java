import edu.pku.code2graph.client.model.BrokenXLL;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static edu.pku.code2graph.client.BrokenXLLDetector.*;

public class BrokenXLLDetectionTest {
  @Test
  public void testDetectCurrent() throws ParserConfigurationException, IOException, SAXException {
    String framework = "android";
    String repoName = "CloudReader";
    //  private static final String commitID = "f82d7d73e9cb5f764b305008fea6cfcc47a21ac4";
    String repoPath =
        Path.of(System.getProperty("user.home"), "coding", "xll", framework, repoName).toString();
    String configPath =
        Path.of(
                System.getProperty("user.dir"),
                "src",
                "main",
                "resources",
                framework,
                "broken",
                "config.yml")
            .toString();
    List<BrokenXLL> xlls = detectCurrent(framework, repoPath, configPath);
    System.out.println(xlls);
  }

  @Test
  public void testDetectAll() throws IOException, ParserConfigurationException, SAXException {
    String framework = "android";
    String repoName = "CloudReader";
    //  private static final String commitID = "f82d7d73e9cb5f764b305008fea6cfcc47a21ac4";
    String repoPath =
        Path.of(System.getProperty("user.home"), "coding", "xll", framework, repoName).toString();
    String configPath =
        Path.of(
                System.getProperty("user.dir"),
                "src",
                "main",
                "resources",
                framework,
                "broken",
                "config.yml")
            .toString();
    String otPath =
        Path.of(System.getProperty("user.home"), "coding", "broken", framework, repoName)
            .toString();
    //
    //    init(framework, repoPath, configPath);
    //    long startTime = System.currentTimeMillis();
    //    detectFor(framework, repoPath, "0b61e05", otPath);
    //    //    detectForAllVersion(framework, repoPath, configPath, otPath);
    //    long endTime = System.currentTimeMillis();
    //    System.out.println("Time consumption: " + (endTime - startTime));
    detectForAllVersion(framework, repoPath, configPath, otPath);
  }
}
