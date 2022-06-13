package edu.pku.code2graph.diff.util;

import edu.pku.code2graph.diff.model.DiffFile;
import edu.pku.code2graph.diff.model.DiffHunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/** A list of helper functions related with Git */
public interface GitService {
  Logger logger = LoggerFactory.getLogger(GitService.class);

  /**
   * Get the diff files in the current working tree
   *
   * @return
   */
  ArrayList<DiffFile> getChangedFilesInWorkingTree();

  /**
   * Get the diff files between one commit and its previous commit
   *
   * @return
   */
  ArrayList<DiffFile> getChangedFilesAtCommit( String commitID);

  /**
   * Get the diff hunks in the current working tree
   *
   * 
   * @return
   */
  List<DiffHunk> getDiffHunksInWorkingTree( List<DiffFile> diffFiles);

  /**
   * Get the diff hunks between one commit and its previous commit
   *
   * 
   * @param commitID
   * @return
   */
  List<DiffHunk> getDiffHunksAtCommit( String commitID, List<DiffFile> diffFiles);

  /**
   * Get the file content at HEAD
   *
   * @param relativePath
   * @return
   */
  String getContentAtHEAD(Charset charset, String relativePath);

  /**
   * Get the file content at one specific commit
   *
   * @param relativePath
   * @returnØØ
   */
  String getContentAtCommit(Charset charset, String relativePath, String commitID);

  /**
   * Get the name of the author of a commit
   *

   * @param commitID
   * @return
   */
  String getCommitterName(String commitID);

  /**
   * Get the email of the author of a commit
   *

   * @param commitID
   * @return
   */
  String getCommitterEmail(String commitID);

  /**
   * Get commits that ever changed a specific file before a commit
   *
   * @param filePath
   * @param beforeCommit
   * @param maxNumber
   * @return
   */
  List<String> getCommitsChangedFile(
      String filePath, String beforeCommit, int... maxNumber);

  /**
   * Get commits that ever changed a specific line range before HEAD
   *

   * @param filePath
   * @return
   */
  List<String> getCommitsChangedLineRange(
   String filePath, int startLine, int endLine);

  /**
   * Get current commit id of repo
   *

   * @return
   */
  String getHEADCommitId();

  /**
   * Get complete current commit id of repo
   *

   * @return
   */
  String getLongHEADCommitId();

  /**
   * Checkout the repo to specific commitID
   *

   * @param commitID
   * @return success or not
   */
  boolean checkoutByCommitID(String commitID);

  /**
   * Checkout the repo to specific complete commitID
   *

   * @param commitID
   * @return success or not
   */
  boolean checkoutByLongCommitID(String commitID);

  /**
   * get commit history
   *

   * @return commits
   */
  List<String> getCommitHistory();

  /**
   * get file at certain commit
   *

   * @return commits
   */
  String getFileAtCommit(String filePath, String commit);
}
