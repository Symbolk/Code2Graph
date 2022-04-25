package edu.pku.code2graph.client.model;

import edu.pku.code2graph.model.Range;

public class RenameInfo {
  Range range;
  String newName; // 重命名后的标识符名称
  String programmingLanguage; // 文件的编程语言，如Java，xml

  public RenameInfo(Range range, String newName, String programmingLanguage) {
    this.range = range;
    this.newName = newName;
    this.programmingLanguage = programmingLanguage;
  }

  @Override
  public String toString() {
    return "{ newName: "
        + newName
        + ", range: "
        + range.getFileName()
        + "/"
        + range.toString()
        + ", programmingLanguage: "
        + programmingLanguage
        + " }";
  }
}
