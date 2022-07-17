import com.csvreader.CsvReader;
import edu.pku.code2graph.model.URITree;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;

public class MiningTest {
  private static Logger logger = LoggerFactory.getLogger(MiningTest.class);

  private ArrayList<String> getCommits(String cacheDir, URITree tree) throws IOException {
    String commitsPath = cacheDir + "/commits.txt";

    FileReader fr = new FileReader(commitsPath);
    BufferedReader br = new BufferedReader(fr);
    ArrayList<String> commits = new ArrayList<>();
    String line;
    while ((line = br.readLine()) != null) {
      commits.add(line);
      try {
        CsvReader reader = new CsvReader(cacheDir + "/" + line + ".csv");
        reader.readHeaders();
        String[] cacheHeaders = reader.getHeaders();
        String uriHeader = cacheHeaders[0];
        while (reader.readRecord()) {
          String source = reader.get(uriHeader);
          tree.add(source);
        }
        reader.close();
        System.out.println(line);
      } catch (Exception e) {
        System.out.println("Error: " + e.getMessage());
      }
    }
    br.close();
    fr.close();
    return commits;
  }

  private void test(String framework, String repoName) throws IOException {
    String cacheDir = System.getProperty("user.home") + "/coding/xll/sha-history/" + framework + "/" + repoName;
    ArrayList<String> commits = getCommits(cacheDir, new URITree());
    System.out.println(commits.size());
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
