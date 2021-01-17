package edu.pku.code2graph.diff;

/**
 * Outermost API provider for diff module
 */
public class Differ {
  // meta info
  private String repoPath;

  public Differ(String repoPath) {
    this.repoPath = repoPath;
  }

  /** Analyze changes in working directory */
  public void computeDiff() {

    // 1. generate 2 graphs

    // for working tree: 2 graphs: old and new

    // extract changed/diff files and involved ones (optional)

    // collect the content and saves to a temp dir (optional)

    // pass the file paths (on disk)/content (in memory) to the generator

    // 2. compare and compute diffs
  }

  /**
   * Compare a specific commit with its parent
   *
   * @param commitID
   */
  public void computeDiff(String commitID) {
    // for one commit: 2 graphs: left and right

  }
}
