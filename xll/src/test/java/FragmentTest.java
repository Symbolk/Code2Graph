import edu.pku.code2graph.xll.Fragment;
import org.junit.jupiter.api.Test;

import java.util.List;

public class FragmentTest {
  @Test
  public void consecutive() {
    Fragment fragment = new Fragment("ParseHTMLString", "camel");
    List<String> words = fragment.slice();
    System.out.println(words);
    assert words.size() == 3;
    assert words.get(0).equals("parse");
    assert words.get(1).equals("html");
    assert words.get(2).equals("string");
  }

  @Test
  public void digits() {
    Fragment fragment = new Fragment("Canvas2DContext", "camel");
    List<String> words = fragment.slice();
    System.out.println(words);
  }

  @Test
  public void digits2() {
    Fragment fragment = new Fragment("Vertex3d", "camel");
    List<String> words = fragment.slice();
    System.out.println(words);
  }

  @Test
  public void digits3() {
    Fragment fragment = new Fragment("JSON5", "camel");
    List<String> words = fragment.slice();
    System.out.println(words);
  }

  @Test
  public void digits4() {
    Fragment fragment = new Fragment("JSON5Parser", "camel");
    List<String> words = fragment.slice();
    System.out.println(words);
  }
}
