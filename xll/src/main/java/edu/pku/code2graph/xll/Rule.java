package edu.pku.code2graph.xll;

import edu.pku.code2graph.model.URI;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Rule extends ArrayList<URIPattern> {
    public URIPattern left() {
        return get(0);
    }

    public URIPattern right() {
        return get(1);
    }

    public void link(List<URI> uris) throws CloneNotSupportedException {
        for (URI leftUri: uris) {
            Map<String, String> leftCaps = left().match(leftUri);
            if (leftCaps == null) continue;
            URIPattern pattern = right().applyCaptures(leftCaps);
            for (URI rightUri: uris) {
                Map<String, String> rightCaps = pattern.match(rightUri);
                if (rightCaps == null) continue;
                System.out.println(leftUri);
                System.out.println(rightUri);
            }
        }
    }
}
