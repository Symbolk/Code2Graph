import edu.pku.code2graph.mining.Confidence;
import org.junit.jupiter.api.Test;

public class ConfidenceTest {
  @Test
  public void testConfidence() {
    assert Confidence.add(0.5, 0.5) == 0.75;
    assert Confidence.add(0.5, 1) == 1;
    assert Confidence.add(0.5, -0.5) == 0;
    assert Confidence.add(-0.5, -0.5) == -0.75;
    assert Confidence.add(-0.5, -1) == -1;
    assert Confidence.add(0.5, 0) == 0.5;
    assert Confidence.add(-0.5, 0) == -0.5;
  }

  @Test
  public void testLCS() {
    assert Confidence.lcs("abc", "abcd") == 3;
  }

  @Test
  public void testIntersects() {
    System.out.println(Confidence.slices("SDCardDirUtil.java"));
  }
}
