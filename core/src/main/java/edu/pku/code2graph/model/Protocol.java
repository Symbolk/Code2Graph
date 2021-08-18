package edu.pku.code2graph.model;

import java.util.Arrays;

public enum Protocol {
  DEF("def"),
  USE("use"),
  ANY("any");

  private final String label;

  Protocol(String label) {
    this.label = label;
  }

  @Override
  public String toString() {
    return label;
  }

  /**
   * Get enum constant from label, corresponding to Enum.valueOf(name)
   *
   * @param s
   * @return
   */
  public static Protocol valueOfLabel(String s) {
    return Arrays.stream(Protocol.values())
        .filter(p -> p.label.equals(s))
        .findFirst()
        .orElse(Protocol.ANY);
  }
}
