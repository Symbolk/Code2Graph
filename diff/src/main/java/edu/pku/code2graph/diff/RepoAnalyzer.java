package edu.pku.code2graph.diff;

import edu.pku.code2graph.diff.model.DiffFile;
import edu.pku.code2graph.diff.model.DiffHunk;
import edu.pku.code2graph.diff.util.GitService;
import edu.pku.code2graph.diff.util.GitServiceCGit;

import java.util.ArrayList;
import java.util.List;

/** Analyze a git repo to get the changed files and textual diff results */
public class RepoAnalyzer {
  private String repoName;
  private String repoPath;
  private List<DiffFile> diffFiles;
  private List<DiffHunk> diffHunks;

  public RepoAnalyzer(String repoName, String repoPath) {
    this.repoName = repoName;
    this.repoPath = repoPath;
    this.diffFiles = new ArrayList<>();
    this.diffHunks = new ArrayList<>();
  }

  /**
   * Analyze the current working tree to cache temp data
   *
   * @return
   */
  public List<DiffFile> analyzeWorkingTree() {
    // analyze the diff files and hunks
    GitService gitService = new GitServiceCGit();
    ArrayList<DiffFile> diffFiles = gitService.getChangedFilesInWorkingTree(this.repoPath);
    if (!diffFiles.isEmpty()) {
      this.diffHunks = gitService.getDiffHunksInWorkingTree(this.repoPath, diffFiles);
      this.diffFiles = diffFiles;
    }
    return diffFiles;
  }

  /**
   * Analyze one specific commit to cache temp data
   *
   * @param commitID
   */
  public List<DiffFile> analyzeCommit(String commitID) {
    // analyze the diff files and hunks
    GitService gitService = new GitServiceCGit();
    ArrayList<DiffFile> diffFiles = gitService.getChangedFilesAtCommit(this.repoPath, commitID);
    if (!diffFiles.isEmpty()) {
      this.diffHunks = gitService.getDiffHunksAtCommit(this.repoPath, commitID, diffFiles);
      this.diffFiles = diffFiles;
    }
    return diffFiles;
  }

  public List<DiffFile> getDiffFiles() {
    return diffFiles;
  }

  public List<DiffHunk> getDiffHunks() {
    return diffHunks;
  }
}
