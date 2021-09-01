package edu.pku.code2graph.model;

import java.util.Arrays;

public enum TestEnum {
  C(".c"),
  CPP(".cpp"),
  HPP(".h"),
  JAVA(".java"),
  XML(".xml"),
  HTML(".html"),
  SQL(".sql"),
  ANY("*"),
  OTHER("");

  public String extension;

  Language(String extension) {
    this.extension = extension;
  }

  /**
   * Get enum constant from label, corresponding to Enum.valueOf(name)
   *
   * @param s
   * @return
   */
  public static Language valueOfLabel(String s) {
    return Arrays.stream(Language.values())
        .filter(
            l ->
                (l.extension.equals(s)
                    || ("*" + l.extension).equals(s)
                    || l.extension.endsWith(s)))
        .findFirst()
        .orElse(Language.OTHER);
  }
}
