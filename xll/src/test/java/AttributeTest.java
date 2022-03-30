import edu.pku.code2graph.xll.Capture;
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

    public void shouldPass(String target) {
      Capture result = match(target);
      assert result != null;
    }

    public void shouldYield(String target, String value) {
      Capture result = match(target);
      assert result != null;
      assert result.get("value") != null;
      assert result.get("value").text.equals(value);
    }
  }

  @Test
  public void wildcardTest1() {
    Matcher matcher = new Matcher("identifier", "**");
    matcher.shouldPass("foo");
    matcher.shouldPass("foo/bar");
    matcher.shouldPass("foo/bar/baz");
  }

  @Test
  public void wildcardTest2() {
    Matcher matcher = new Matcher("identifier", "**/foo");
    matcher.shouldPass("foo");
    matcher.shouldPass("bar/foo");
    matcher.shouldPass("bar/baz/foo");
    matcher.shouldFail("bar/baz/foo/qux");
  }

  @Test
  public void wildcardTest3() {
    Matcher matcher = new Matcher("identifier", "foo/**");
    matcher.shouldPass("qux/foo");
    matcher.shouldPass("qux/foo/bar");
    matcher.shouldPass("qux/foo/bar/baz");
    matcher.shouldFail("qux/bar/baz");
  }

  @Test
  public void wildcardTest4() {
    Matcher matcher = new Matcher("identifier", "foo/**/qux");
    matcher.shouldPass("foo/qux");
    matcher.shouldPass("foo/bar/qux");
    matcher.shouldPass("foo/bar/baz/qux");
    matcher.shouldFail("foo/bar/baz");
  }

  @Test
  public void wildcardTest5() {
    Matcher matcher = new Matcher("identifier", "foo/*");
    matcher.shouldFail("foo");
    matcher.shouldPass("foo/bar");
    matcher.shouldFail("foo/bar/baz");
    matcher.shouldPass("bar/foo/baz");
  }

  @Test
  public void wildcardTest6() {
    Matcher matcher = new Matcher("identifier", "foo/ba*");
    matcher.shouldPass("foo/bar");
    matcher.shouldPass("foo/baz");
    matcher.shouldFail("foo/bar/baz");
    matcher.shouldFail("bar/qux");
  }

  @Test
  public void wildcardTest7() {
    Matcher matcher = new Matcher("identifier", "foo/*/qux");
    matcher.shouldFail("foo/qux");
    matcher.shouldPass("foo/bar/qux");
    matcher.shouldFail("foo/bar/baz/qux");
    matcher.shouldFail("foo/bar/qux/baz");
  }

  @Test
  public void anchorTest() {
    Matcher matcher = new Matcher("identifier", "foo/(&name).java");
    matcher.shouldPass("foo/bar.java");
  }

  @Test
  public void slashAnchorTest() {
    Matcher matcher = new Matcher("identifier", "(&path:slash).java");
    matcher.shouldPass("foo/bar.java");
  }
}
