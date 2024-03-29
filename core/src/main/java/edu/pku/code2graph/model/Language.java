package edu.pku.code2graph.model;

import java.io.Serializable;
import java.util.Arrays;

public enum Language  implements Serializable {
  C(".c"),
  CPP(".cpp"),
  HPP(".h"),
  JAVA(".java"),
  XML(".xml"),
  HTML(".html"),
  JSP(".jsp"),
  FTL(".ftl"),
  SQL(".sql"),
  FILE(".file"),
  ANY("*"),
  OTHER("?");

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
        .orElse(Language.FILE);
  }
}
