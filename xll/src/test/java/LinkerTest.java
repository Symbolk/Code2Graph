import edu.pku.code2graph.xll.URIPattern;
import org.junit.jupiter.api.Test;
import edu.pku.code2graph.model.URI;
import edu.pku.code2graph.xll.Rule;
import edu.pku.code2graph.xll.Config;
import edu.pku.code2graph.xll.ConfigLoader;

import java.util.Optional;

public class LinkerTest {
    @Test
    public void main() {
        Optional<Config> config = new ConfigLoader().load("src/main/resources/config.yml");
        config.ifPresent(value -> {
            Rule rule = value.getRules().get(0);
            URI uri = new URI("def://foo/bar/baz.java//getFooBar");
            URIPattern left = rule.getLeft();
            System.out.println(left);
            System.out.println(uri);
            System.out.println(left.match(uri));
        });
    }
}
