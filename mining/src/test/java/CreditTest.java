import edu.pku.code2graph.mining.*;
import org.junit.jupiter.api.Test;

public class CreditTest {
  @Test
  public void testCredit() {
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
    System.out.println(new Comparison.Slice("Abc"));
    System.out.println(new Comparison("abc", "Abc").similarity);
    System.out.println(new Comparison("abcFoo", "qux_abc_bar").similarity);
    System.out.println(new Comparison("abcFoo", "qux_abc_bar").getPattern1());
    System.out.println(new Comparison("abcFoo", "qux_abc_bar").getPattern2());
  }

  @Test
  public void testSplit() {
    System.out.println(new Comparison.Slice("fdsa/@+id\\\\/container"));
  }

  @Test
  public void testIntersects() {
    System.out.println(new Comparison("mContainer", "@+id\\\\/container"));
  }
}
