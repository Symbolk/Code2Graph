import edu.pku.code2graph.mining.Confidence;
import edu.pku.code2graph.mining.Credit;
import org.junit.jupiter.api.Test;

public class ConfidenceTest {
  @Test
  public void testConfidence() {
    assert Credit.add(0.5, 0.5) == 0.75;
    assert Credit.add(0.5, 1) == 1;
    assert Credit.add(0.5, -0.5) == 0;
    assert Credit.add(-0.5, -0.5) == -0.75;
    assert Credit.add(-0.5, -1) == -1;
    assert Credit.add(0.5, 0) == 0.5;
    assert Credit.add(-0.5, 0) == -0.5;
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
