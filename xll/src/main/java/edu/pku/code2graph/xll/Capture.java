package edu.pku.code2graph.xll;

import java.util.*;

public final class Capture extends TreeMap<String, Fragment> {
  public Capture project(Collection<String> collection) {
    Capture capture = new Capture();
    for (String key : collection) {
      if (containsKey(key)) {
        capture.put(key, get(key));
      }
    }
    return capture;
  }

  public boolean match(Capture capture) {
    for (String name : keySet()) {
      Fragment local = get(name);
      Fragment foreign = capture.get(name);
      if (!local.match(foreign)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    for (Map.Entry<String, Fragment> entry : entrySet()) {
      builder
          .append(entry.getKey())
          .append(":")
          .append(entry.getValue())
          .append(";");
    }
    return builder.toString();
  }

  @Override
  public boolean equals(Object o) {
    return toString().equals(o.toString());
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  public Capture clone() {
    Capture result = (Capture) super.clone();
    result.putAll(this);
    return result;
  }
}
