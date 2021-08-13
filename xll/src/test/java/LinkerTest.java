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
            URI uri1 = new URI("def://foo/bar.java//getFooBar");
            System.out.println(rule.getLeft().match(uri1));
            URI uri2 = new URI("def://foo/baz.java//Select//#{FooBar}");
            System.out.println(rule.getRight().match(uri2));
            List<URI> uris = new ArrayList<>();
            uris.add(uri1);
            uris.add(uri2);
            rule.link(uris);
        });
    }
}
