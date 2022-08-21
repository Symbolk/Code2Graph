import edu.pku.code2graph.mining.*;
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
    assert new Comparison("abc", "abcd").similarity == 0.;
    assert new Comparison("abc", "Abc").similarity == 1.;
    assert new Comparison("abcFoo", "abc_bar").similarity == 0.5;
  }

  @Test
  public void testSplit() {
    System.out.println(Credit.getLastSegment("fdsa/@+id\\\\/container"));
  }

  @Test
  public void testIntersects() {
    System.out.println(new Comparison("mContainer", "@+id\\\\/container"));
  }
}
