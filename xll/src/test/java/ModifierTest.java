import edu.pku.code2graph.xll.Fragment;
import org.junit.jupiter.api.Test;

public class ModifierTest {
  @Test
  public void test1() {
    Fragment frag1 = new Fragment("foo/bar", "slash");
    Fragment frag2 = new Fragment("fooBar", "camel");
    Fragment frag3 = new Fragment("fooBar", "snake");
    Fragment frag4 = new Fragment("foobar", "lower");
    assert frag1.match(frag2);
    assert !frag1.match(frag3);
    assert frag1.match(frag4);
  }
}
