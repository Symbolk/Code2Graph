import edu.pku.code2graph.xll.URIPattern;
import org.junit.jupiter.api.Test;
import edu.pku.code2graph.xll.Rule;
import edu.pku.code2graph.xll.Config;
import edu.pku.code2graph.xll.ConfigLoader;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.*;

import java.util.Optional;

public class LinkerTest {
    @Test
    public void main() {
        Optional<Config> config = new ConfigLoader().load("src/main/resources/config.yml");
        config.ifPresent(value -> {
            Rule rule = value.getRules().get(0);
            System.out.println(rule.left());
        });
    }
}
