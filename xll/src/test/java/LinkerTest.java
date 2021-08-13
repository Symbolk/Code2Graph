import edu.pku.code2graph.xll.URIPattern;
import org.junit.jupiter.api.Test;
import edu.pku.code2graph.model.URI;
import edu.pku.code2graph.xll.Rule;
import edu.pku.code2graph.xll.Config;
import edu.pku.code2graph.xll.ConfigLoader;

import java.util.*;

public class LinkerTest {
    @Test
    public void main() {
        Optional<Config> config = new ConfigLoader().load("src/main/resources/config.yml");
        config.ifPresent(value -> {
            Rule rule = value.getRules().get(0);
            URIPattern left = rule.getLeft();
            URI uri1 = new URI("def://foo/bar.java//getFooBar");
            System.out.println(left);
            System.out.println(uri1);
            System.out.println(left.match(uri1));
            System.out.println();

            URIPattern right = rule.getRight();
            URI uri2 = new URI("def://foo/baz.java//Select//#{FooBar}");
            System.out.println(right);
            System.out.println(uri2);
            System.out.println(right.match(uri2));
            System.out.println();

            List<URI> uris = new ArrayList<>();
            uris.add(uri1);
            uris.add(uri2);
            System.out.println("Total links: " + rule.link(uris));
        });
    }
}
