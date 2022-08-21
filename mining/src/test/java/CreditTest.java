import edu.pku.code2graph.mining.Candidate;
import edu.pku.code2graph.mining.Credit;
import org.junit.jupiter.api.Test;

public class CreditTest {
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
    assert Credit.lcs("abc", "abcd") == 3;
  }

  @Test
  public void testSplit() {
    System.out.println(Credit.getLastSegment("fdsa/@+id\\\\/container"));
  }

  @Test
  public void testIntersects() {
    System.out.println(Candidate.similarity("mContainer", "@+id\\\\/container"));
  }
}
