import edu.pku.code2graph.xll.Capture;
import edu.pku.code2graph.xll.Fragment;
import edu.pku.code2graph.xll.URIPattern;
import edu.pku.code2graph.xll.pattern.AttributePattern;
import org.junit.jupiter.api.Test;

public class AttributeTest {
  private static class Matcher {
    final URIPattern root = new URIPattern();
    final AttributePattern pattern;

    public Matcher(String key, String value) {
      this.pattern = AttributePattern.create(key, value, root);
      assert this.pattern != null;
      System.out.println("pattern: " + value);
    }

    private Capture match(String target) {
      Capture result = pattern.match(target, new Capture());
      System.out.println(target + " " + result);
      return result;
    }

    public void shouldFail(String target) {
      Capture result = match(target);
      assert result == null;
    }

    public Capture shouldPass(String target) {
      Capture result = match(target);
      assert result != null;
      return result;
    }

    public void shouldPass(String target, String[] texts) {
      Capture result = shouldPass(target);
      for (int i = 0; i < texts.length; ++i) {
        Fragment value = result.get(String.valueOf(i));
        assert value != null;
        assert value.text.equals(texts[i]);
      }
    }
  }

  @Test
  public void doubleAsteriskTest1() {
    Matcher matcher = new Matcher("identifier", "**");
    matcher.shouldPass("foo");
    matcher.shouldPass("foo/bar");
    matcher.shouldPass("foo/bar/baz");
  }

  @Test
  public void doubleAsteriskTest2() {
    Matcher matcher = new Matcher("identifier", "**/foo");
    matcher.shouldPass("foo");
    matcher.shouldPass("bar/foo");
    matcher.shouldPass("bar/baz/foo");
    matcher.shouldFail("bar/baz/foo/qux");
  }

  @Test
  public void doubleAsteriskTest3() {
    Matcher matcher = new Matcher("identifier", "foo/**");
    matcher.shouldPass("qux/foo");
    matcher.shouldPass("qux/foo/bar");
    matcher.shouldPass("qux/foo/bar/baz");
    matcher.shouldFail("qux/bar/baz");
  }

  @Test
  public void doubleAsteriskTest4() {
    Matcher matcher = new Matcher("identifier", "foo/**/qux");
    matcher.shouldPass("foo/qux");
    matcher.shouldPass("foo/bar/qux");
    matcher.shouldPass("foo/bar/baz/qux");
    matcher.shouldFail("foo/bar/baz");
  }

  @Test
  public void singleAsteriskTest5() {
    Matcher matcher = new Matcher("identifier", "foo/*");
    matcher.shouldFail("foo");
    matcher.shouldPass("foo/bar");
    matcher.shouldFail("foo/bar/baz");
    matcher.shouldPass("bar/foo/baz");
  }

  @Test
  public void singleAsteriskTest6() {
    Matcher matcher = new Matcher("identifier", "foo/ba*");
    matcher.shouldPass("foo/bar");
    matcher.shouldPass("foo/baz");
    matcher.shouldFail("foo/bar/baz");
    matcher.shouldFail("bar/qux");
  }

  @Test
  public void singleAsteriskTest7() {
    Matcher matcher = new Matcher("identifier", "foo/*/qux");
    matcher.shouldFail("foo/qux");
    matcher.shouldPass("foo/bar/qux");
    matcher.shouldFail("foo/bar/baz/qux");
    matcher.shouldFail("foo/bar/qux/baz");
  }

  @Test
  public void greedyCaptureTest1() {
    Matcher matcher = new Matcher("identifier", "(0...).html");
    matcher.shouldPass("foo.html", new String[]{"foo"});
    matcher.shouldPass("foo/bar.html", new String[]{"foo/bar"});
    matcher.shouldPass("foo/bar/baz.html", new String[]{"foo/bar/baz"});
  }

  @Test
  public void greedyCaptureTest2() {
    Matcher matcher = new Matcher("identifier", "(0...)/foo");
    matcher.shouldFail("foo"); // greedy capture do not match empty content
    matcher.shouldPass("bar/foo", new String[]{"bar"});
    matcher.shouldPass("bar/baz/foo", new String[]{"bar/baz"});
    matcher.shouldFail("bar/baz/foo/qux");
  }

  @Test
  public void greedyCaptureTest3() {
    Matcher matcher = new Matcher("identifier", "foo/(0...).html");
    matcher.shouldFail("qux/foo.html");
    matcher.shouldPass("qux/foo/bar.html", new String[]{"bar"});
    matcher.shouldPass("qux/foo/bar/baz.html", new String[]{"bar/baz"});
    matcher.shouldFail("qux/bar/baz");
  }

  @Test
  public void greedyCaptureTest4() {
    Matcher matcher = new Matcher("identifier", "foo/(0...)/qux");
    matcher.shouldFail("foo/qux"); // greedy capture do not match empty content
    matcher.shouldPass("foo/bar/qux", new String[]{"bar"});
    matcher.shouldPass("foo/bar/baz/qux", new String[]{"bar/baz"});
    matcher.shouldFail("foo/bar/baz");
  }

  @Test
  public void normalAnchorTest() {
    Matcher matcher = new Matcher("identifier", "foo/(&0).java");
    matcher.shouldPass("foo/bar.java");
  }

  @Test
  public void greedyAnchorTest() {
    Matcher matcher = new Matcher("identifier", "(&0...).java");
    matcher.shouldPass("foo/bar.java");
  }
}
