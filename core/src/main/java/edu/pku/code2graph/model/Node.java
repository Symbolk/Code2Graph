package edu.pku.code2graph.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static edu.pku.code2graph.model.TypeSet.type;

public abstract class Node implements Serializable {
  private static final long serialVersionUID = -4685691468295743770L;

  private final Integer id;
  protected Type type;
  protected String snippet;
  protected Language language;
  protected Range range;
  protected URI uri;
  // optional & additional attributes
  private Map<String, Object> attrs = new HashMap<>(5);

  public Node(Integer id, Language language) {
    this.id = id;
    this.language = language;
  }

  public Node(Integer id, Language language, Type type, String snippet) {
    this.id = id;
    this.language = language;
    this.type = type;
    this.snippet = snippet;
  }

  public Integer getId() {
    return id;
  }

  public Type getType() {
    return type == null ? type("OTHER") : type;
  }

  public void setType(Type type) {
    this.type = type;
  }

  public void setSnippet(String snippet) {
    this.snippet = snippet;
  }

  public String getSnippet() {
    return snippet;
  }

  public Language getLanguage() {
    return language;
  }

  public void setLanguage(Language language) {
    this.language = language;
  }

  public Range getRange() {
    return range;
  }

  public void setRange(Range range) {
    this.range = range;
  }

  public URI getUri() {
    return uri;
  }

  public void setUri(URI uri) {
    this.uri = uri;
  }

  public abstract int hashSignature();

  public Optional<Object> getAttribute(String name) {
    if(!attrs.containsKey(name)) {
      return Optional.empty();
    }
    return Optional.of(attrs.get(name));
  }

  public void setAttribute(String name, Object obj) {
    attrs.put(name, obj);
  }
}
