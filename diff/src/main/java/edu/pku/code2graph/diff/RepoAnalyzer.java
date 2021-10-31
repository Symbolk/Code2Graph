package edu.pku.code2graph.diff;

import edu.pku.code2graph.diff.model.DiffFile;
import edu.pku.code2graph.diff.model.DiffHunk;
import edu.pku.code2graph.diff.util.GitService;
import edu.pku.code2graph.diff.util.GitServiceCGit;
import edu.pku.code2graph.exception.InvalidRepoException;
import edu.pku.code2graph.exception.NonexistPathException;
import edu.pku.code2graph.util.FileUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Analyze a git repo to get the changed files and textual diff results */
public class RepoAnalyzer {
  private String repoName;
  private String repoPath;
  private List<DiffFile> diffFiles;
  private List<DiffHunk> diffHunks;

  public RepoAnalyzer(String repoName, String repoPath)
      throws NonexistPathException, InvalidRepoException {
    if (!FileUtil.checkExists(repoPath)) {
      throw new NonexistPathException("Repo", repoPath);
    }
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
    try {
      GitService gitService = new GitServiceCGit(repoPath);
      ArrayList<DiffFile> diffFiles = gitService.getChangedFilesInWorkingTree();
      if (!diffFiles.isEmpty()) {
        this.diffHunks = gitService.getDiffHunksInWorkingTree(diffFiles);
        this.diffFiles = diffFiles;
      }
    } catch (NonexistPathException | IOException | InvalidRepoException e) {
      e.printStackTrace();
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
    try {
      GitServiceCGit gitService = new GitServiceCGit(repoPath);
      gitService.setIgnoreWhiteChanges(true);
      ArrayList<DiffFile> diffFiles = gitService.getChangedFilesAtCommit(commitID);
      if (!diffFiles.isEmpty()) {
        this.diffHunks = gitService.getDiffHunksAtCommit(commitID, diffFiles);
        this.diffFiles = diffFiles;
      }
    } catch (NonexistPathException | IOException | InvalidRepoException e) {
      e.printStackTrace();
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
