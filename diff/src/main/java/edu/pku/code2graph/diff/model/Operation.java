package edu.pku.code2graph.diff.model;

/** Action operation on AST or Refactoring */
public enum Operation {
  // AST operations
  ADD("Add", 1),
  DEL("Delete", 2),
  UPD("Update", 3),
  MOV("Move", 4),

  // Refactoring operations
  // ADD("Add", 1),
  CHANGE("Change", 5),
  CONVERT("Convert", 6),
  EXTRACT("Extract", 7),
  EXTRACT_AND_MOVE("Extract And Move", 8),
  INLINE("Inline", 9),
  INTRODUCE("Introduce", 10),
  MERGE("Merge", 11),
  MODIFY("Modify", 12),
  MOVE("Move", 13),
  MOVE_AND_INLINE("Move And Inline", 14),
  MOVE_AND_RENAME("Move And Rename", 15),
  PARAMETERIZE("Parameterize", 16),
  PULL_UP("Pull Up", 17),
  PULL_DOWN("Pull Down", 18),
  REPLACE("Replace", 19),
  REORDER("Reorder", 20),
  RENAME("Rename", 21),
  REMOVE("Remove", 22),
  SPILT("Split", 23),


  UKN("Unknown", 24);

  public String label;
  public int index;

  Operation(String label, int index) {
    this.label = label;
    this.index = index;
  }

  @Override
  public String toString() {
    return label;
  }

  public int getIndex() {
    return index;
  }
}
