package edu.pku.code2graph.diff.util;

import edu.pku.code2graph.diff.model.*;
import edu.pku.code2graph.diff.textualdiffparser.api.DiffParser;
import edu.pku.code2graph.diff.textualdiffparser.api.UnifiedDiffParser;
import edu.pku.code2graph.diff.textualdiffparser.api.model.Diff;
import edu.pku.code2graph.diff.textualdiffparser.api.model.Line;
import edu.pku.code2graph.util.FileUtil;
import edu.pku.code2graph.util.SysUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Implementation of helper functions based on the output of git commands. */
public class GitServiceCGit implements GitService {
  private boolean ignoreWhiteChanges = false;

  public void setIgnoreWhiteChanges(boolean ignoreWhiteChanges) {
    this.ignoreWhiteChanges = ignoreWhiteChanges;
  }

  /**
   * Get the diff files in the current working tree
   *
   * @return
   */
  @Override
  public ArrayList<DiffFile> getChangedFilesInWorkingTree(String repoPath) {
    // unstage the staged files first
    //    SysUtil.runSystemCommand(repoPath, "git", "restore", "--staged", ".");
    SysUtil.runSystemCommand(repoPath, StandardCharsets.UTF_8, "git", "reset", "HEAD", ".");

    ArrayList<DiffFile> diffFileList = new ArrayList<>();
    // run git status --porcelain to get changeset
    String output =
        SysUtil.runSystemCommand(
            repoPath, StandardCharsets.UTF_8, "git", "status", "--porcelain", "-uall");
    // early return
    if (output.isEmpty()) {
      // working tree clean
      return new ArrayList<>();
    }
    // ! use an independent incremental index to avoid index jump in case of invalid status output
    // only increment index when creating new diff file
    int fileIndex = 0;

    String[] lines = output.split("\\r?\\n");
    for (int i = 0; i < lines.length; i++) {
      String[] temp = lines[i].trim().split("\\s+");
      String symbol = temp[0];
      String relativePath = temp[1];
      FileType fileType = DiffUtil.checkFileType(repoPath, relativePath);
      String absolutePath = repoPath + File.separator + relativePath;
      FileStatus status = DiffUtil.convertSymbolToStatus(symbol);
      DiffFile DiffFile = null;
      Charset charset = StandardCharsets.UTF_8;
      switch (status) {
        case MODIFIED:
          charset = DiffUtil.detectCharset(absolutePath);
          DiffFile =
              new DiffFile(
                  fileIndex++,
                  status,
                  fileType,
                  charset,
                  relativePath,
                  relativePath,
                  (fileType == FileType.BIN
                      ? ""
                      : getContentAtHEAD(charset, repoPath, relativePath)),
                  (fileType == FileType.BIN ? "" : FileUtil.readFileToString(absolutePath)));
          break;
        case ADDED:
        case UNTRACKED:
          charset = DiffUtil.detectCharset(absolutePath);
          DiffFile =
              new DiffFile(
                  fileIndex++,
                  status,
                  fileType,
                  charset,
                  "",
                  relativePath,
                  "",
                  (fileType == FileType.BIN ? "" : FileUtil.readFileToString(absolutePath)));
          break;
        case DELETED:
          // charset and filetype of the deleted is hard to detect
          if (checkBinaryFileByDiff(repoPath, relativePath, charset)) {
            fileType = FileType.BIN;
          }
          DiffFile =
              new DiffFile(
                  fileIndex++,
                  status,
                  fileType,
                  StandardCharsets.UTF_8,
                  relativePath,
                  "",
                  (fileType == FileType.BIN
                      ? ""
                      : getContentAtHEAD(charset, repoPath, relativePath)),
                  "");
          break;
        case RENAMED:
        case COPIED:
          if (temp.length == 4) {
            // C/R aaa -> bbb
            String oldPath = temp[1];
            String newPath = temp[3];
            fileType = DiffUtil.checkFileType(repoPath, newPath);
            String newAbsPath = repoPath + File.separator + newPath;
            charset = DiffUtil.detectCharset(absolutePath);
            DiffFile =
                new DiffFile(
                    fileIndex++,
                    status,
                    fileType,
                    charset,
                    oldPath,
                    newPath,
                    (fileType == FileType.BIN ? "" : getContentAtHEAD(charset, repoPath, oldPath)),
                    (fileType == FileType.BIN ? "" : FileUtil.readFileToString(newAbsPath)));
          } else if (temp.length == 3) {
            // CXX/RXX aaa bbb
            String oldPath = temp[1];
            String newPath = temp[2];
            fileType = DiffUtil.checkFileType(repoPath, newPath);
            String newAbsPath = repoPath + File.separator + newPath;
            charset = DiffUtil.detectCharset(absolutePath);
            DiffFile =
                new DiffFile(
                    fileIndex++,
                    status,
                    fileType,
                    charset,
                    oldPath,
                    newPath,
                    (fileType == FileType.BIN ? "" : getContentAtHEAD(charset, repoPath, oldPath)),
                    (fileType == FileType.BIN ? "" : FileUtil.readFileToString(newAbsPath)));
          }
          break;
        default:
          break;
      }
      if (DiffFile != null) {
        diffFileList.add(DiffFile);
      }
    }
    // assert: diffFileList.size() == fileIndex + 1
    return diffFileList;
  }

  /**
   * Get the diff files between one commit and its previous commit
   *
   * @return
   */
  @Override
  public ArrayList<DiffFile> getChangedFilesAtCommit(String repoPath, String commitID) {
    // git diff <start_commit> <end_commit>
    // on Windows the ~ character must be used instead of ^
    String output =
        SysUtil.runSystemCommand(
            repoPath,
            StandardCharsets.UTF_8,
            "git",
            "diff",
            "--name-status",
            getParentCommitID(repoPath, commitID),
            commitID);
    // early return
    if (output.trim().isEmpty()) {
      return new ArrayList<>();
    }
    ArrayList<DiffFile> diffFileList = new ArrayList<>();
    String[] lines = output.split("\\r?\\n");
    // ! use an independent incremental index to avoid index jump in case of invalid status output
    // only increment index when creating new diff file
    int fileIndex = 0;
    for (int i = 0; i < lines.length; i++) {
      String[] temp = lines[i].trim().split("\\s+");
      String symbol = temp[0];
      String relativePath = temp[1];
      FileType fileType = DiffUtil.checkFileType(repoPath, relativePath);
      //                        String absolutePath = repoDir + File.separator + relativePath;
      FileStatus status = DiffUtil.convertSymbolToStatus(symbol);
      DiffFile diffFile = null;
      Charset charset = StandardCharsets.UTF_8;
      switch (status) {
        case MODIFIED:
          diffFile =
              new DiffFile(
                  fileIndex++,
                  status,
                  fileType,
                  charset,
                  relativePath,
                  relativePath,
                  (fileType == FileType.BIN
                      ? ""
                      : getContentAtCommit(charset, repoPath, relativePath, commitID + "~")),
                  (fileType == FileType.BIN
                      ? ""
                      : getContentAtCommit(charset, repoPath, relativePath, commitID)));
          break;
        case ADDED:
        case UNTRACKED:
          diffFile =
              new DiffFile(
                  fileIndex++,
                  status,
                  fileType,
                  charset,
                  "",
                  relativePath,
                  "",
                  (fileType == FileType.BIN
                      ? ""
                      : getContentAtCommit(charset, repoPath, relativePath, commitID)));
          break;
        case DELETED:
          diffFile =
              new DiffFile(
                  fileIndex++,
                  status,
                  fileType,
                  charset,
                  relativePath,
                  "",
                  (fileType == FileType.BIN
                      ? ""
                      : getContentAtCommit(charset, repoPath, relativePath, commitID + "~")),
                  "");
          break;
        case RENAMED:
        case COPIED:
          if (temp.length == 4) {
            // C/R aaa -> bbb
            String oldPath = temp[1];
            String newPath = temp[3];
            fileType = DiffUtil.checkFileType(repoPath, newPath);
            diffFile =
                new DiffFile(
                    fileIndex++,
                    status,
                    fileType,
                    charset,
                    oldPath,
                    newPath,
                    (fileType == FileType.BIN
                        ? ""
                        : getContentAtCommit(charset, repoPath, oldPath, commitID + "~")),
                    (fileType == FileType.BIN
                        ? ""
                        : getContentAtCommit(charset, repoPath, newPath, commitID)));
          } else if (temp.length == 3) {
            // CXX/RXX aaa bbb
            String oldPath = temp[1];
            String newPath = temp[2];
            fileType = DiffUtil.checkFileType(repoPath, newPath);
            diffFile =
                new DiffFile(
                    fileIndex++,
                    status,
                    fileType,
                    charset,
                    oldPath,
                    newPath,
                    (fileType == FileType.BIN
                        ? ""
                        : getContentAtCommit(charset, repoPath, oldPath, commitID + "~")),
                    (fileType == FileType.BIN
                        ? ""
                        : getContentAtCommit(charset, repoPath, newPath, commitID)));
          }
          break;
        default:
          break;
      }
      if (diffFile != null) {
        diffFileList.add(diffFile);
      }
    }
    // assert: diffFileList.size() == fileIndex + 1
    return diffFileList;
  }

  @Override
  public List<DiffHunk> getDiffHunksInWorkingTree(String repoPath, List<DiffFile> diffFiles) {
    // unstage the staged files first
    //    SysUtil.runSystemCommand(repoPath, "git", "reset", "--mixed");
    SysUtil.runSystemCommand(repoPath, StandardCharsets.UTF_8, "git", "reset", "HEAD", ".");
    // diff once for all
    // git diff + git diff --cached/staged == git diff HEAD (show all the changes since last commit
    // String diffOutput = SysUtil.runSystemCommand(repoPath, "git", "diff", "HEAD", "-U0");

    // diff per file
    StringBuilder diffOutput = new StringBuilder();
    for (DiffFile diffFile : diffFiles) {
      if (null != diffFile.getARelativePath() && !"".equals(diffFile.getARelativePath())) {
        // generate diff hunks for modified or deleted binary files (that cannot be parsed)
        if (diffFile.getFileType().equals(FileType.BIN)) {
          DiffHunk diffHunk = createDiffHunkForBinaryFile(diffFile);
          // bidirectional binding
          diffHunk.setFileIndex(diffFile.getIndex());
          List<DiffHunk> diffHunksInFile = new ArrayList<>();
          diffHunksInFile.add(diffHunk);
          diffFile.setDiffHunks(diffHunksInFile);
        }
        if (ignoreWhiteChanges) {
          diffOutput.append(
              SysUtil.runSystemCommand(
                  repoPath,
                  diffFile.getCharset(),
                  "git",
                  "diff",
                  "--ignore-cr-at-eol",
                  "--ignore-all-space",
                  "--ignore-blank-lines",
                  "--ignore-space-change",
                  "-U0",
                  "--",
                  diffFile.getARelativePath()));
        } else {
          diffOutput.append(
              SysUtil.runSystemCommand(
                  repoPath,
                  diffFile.getCharset(),
                  "git",
                  "diff",
                  "-U0",
                  "--",
                  diffFile.getARelativePath()));
        }
      }
    }
    List<Diff> diffs = new ArrayList<>();
    if (!diffOutput.toString().trim().isEmpty()) {
      // with -U0 (no context lines), the generated patch cannot be applied successfully
      DiffParser parser = new UnifiedDiffParser();
      diffs = parser.parse(new ByteArrayInputStream(diffOutput.toString().getBytes()));
    }

    return generateDiffHunks(repoPath, diffs, diffFiles);
  }

  private DiffHunk createDiffHunkForBinaryFile(DiffFile diffFile) {
    ChangeType changeType =
        diffFile.getStatus().equals(FileStatus.DELETED) ? ChangeType.DELETED : ChangeType.UPDATED;
    DiffHunk diffHunk =
        new DiffHunk(
            0,
            diffFile.getFileType(),
            changeType,
            new Hunk(
                Version.A,
                diffFile.getARelativePath(),
                0,
                0,
                ContentType.BINARY,
                new ArrayList<>()),
            new Hunk(
                Version.B,
                diffFile.getBRelativePath(),
                0,
                0,
                ContentType.BINARY,
                new ArrayList<>()),
            changeType.label
                + " "
                + diffFile.getFileType().label
                + " File:"
                + diffFile.getARelativePath());
    diffHunk.addASTAction(
        new Action(
            (changeType.equals(ChangeType.DELETED) ? Operation.DEL : Operation.UPD),
            "Binary",
            "",
            "File",
            diffFile.getBRelativePath()));
    return diffHunk;
  }

  /**
   * Generate diff hunks from diffs parsed from git-diff output
   *
   * @param diffs
   * @return
   */
  private List<DiffHunk> generateDiffHunks(
      String repoPath, List<Diff> diffs, List<DiffFile> diffFiles) {
    List<DiffHunk> allDiffHunks = new ArrayList<>();
    // one file, one diff
    // UNTRACKED/ADDED files won't be shown in the diff
    for (DiffFile diffFile : diffFiles) {
      if (diffFile.getStatus().equals(FileStatus.ADDED)
          || diffFile.getStatus().equals(FileStatus.UNTRACKED)) {
        List<String> lines = DiffUtil.convertStringToList(diffFile.getBContent());
        DiffHunk diffHunk =
            new DiffHunk(
                0,
                DiffUtil.checkFileType(repoPath, diffFile.getBRelativePath()),
                ChangeType.ADDED,
                new Hunk(Version.A, "", 0, -1, ContentType.EMPTY, new ArrayList<>()),
                new Hunk(
                    Version.B,
                    diffFile.getBRelativePath(),
                    1,
                    lines.size(),
                    DiffUtil.checkContentType(lines),
                    lines),
                "Add " + diffFile.getFileType().label + " File:" + diffFile.getBRelativePath());
        diffHunk.addASTAction(
            new Action(Operation.ADD, "", "", "File", diffFile.getBRelativePath()));

        // bidirectional binding
        diffHunk.setFileIndex(diffFile.getIndex());
        List<DiffHunk> diffHunksInFile = new ArrayList<>();
        diffHunksInFile.add(diffHunk);
        allDiffHunks.add(diffHunk);
        diffFile.setDiffHunks(diffHunksInFile);
      }
    }

    for (Diff diff : diffs) {
      // the hunkIndex of the diff hunk in the current file diff, start from 0
      Integer hunkIndex = 0;

      String aFilePath = diff.getFromFileName();
      String bFilePath = diff.getToFileName();

      List<String> headers = diff.getHeaderLines();
      headers.add("--- " + aFilePath);
      headers.add("+++ " + bFilePath);

      // currently we only process Java files
      FileType fileType =
          aFilePath.contains("/dev/null")
              ? DiffUtil.checkFileType(repoPath, bFilePath) // ADDED/UNTRACKED
              : DiffUtil.checkFileType(repoPath, aFilePath);

      // collect and save diff hunks into diff files
      List<DiffHunk> diffHunksInFile = new ArrayList<>();
      for (edu.pku.code2graph.diff.textualdiffparser.api.model.Hunk hunk : diff.getHunks()) {
        List<List<String>> hunkLines = splitHunkLines(hunk.getLines());
        List<String> aCodeLines = hunkLines.get(1);
        List<String> bCodeLines = hunkLines.get(2);
        int leadingNeutral = hunkLines.get(0).size();
        int trailingNeutral = hunkLines.get(3).size();
        Hunk aHunk =
            new Hunk(
                Version.A,
                removeVersionLabel(aFilePath),
                // with -U0, leadingNeutral = 0 = trailingNeutral
                hunk.getFromFileRange().getLineStart() + leadingNeutral,
                hunk.getFromFileRange().getLineStart()
                    + leadingNeutral
                    + hunk.getFromFileRange().getLineCount()
                    - leadingNeutral
                    - trailingNeutral
                    - 1,
                DiffUtil.checkContentType(aCodeLines),
                aCodeLines);
        Hunk bHunk =
            new Hunk(
                Version.B,
                removeVersionLabel(bFilePath),
                hunk.getToFileRange().getLineStart() + leadingNeutral,
                hunk.getToFileRange().getLineStart()
                    + leadingNeutral
                    + hunk.getToFileRange().getLineCount()
                    - leadingNeutral
                    - trailingNeutral
                    - 1,
                DiffUtil.checkContentType(bCodeLines),
                bCodeLines);
        ChangeType changeType = ChangeType.UPDATED;
        if (aCodeLines.isEmpty()) {
          changeType = ChangeType.ADDED;
        }
        if (bCodeLines.isEmpty()) {
          changeType = ChangeType.DELETED;
        }
        DiffHunk diffHunk = new DiffHunk(hunkIndex, fileType, changeType, aHunk, bHunk);
        diffHunk.setRawDiffs(hunk.getRawLines());
        diffHunksInFile.add(diffHunk);
        allDiffHunks.add(diffHunk);
        hunkIndex++;
      }

      // bidirectional binding
      for (DiffFile diffFile : diffFiles) {
        if (removeVersionLabel(aFilePath).equals(diffFile.getARelativePath())
            && removeVersionLabel(bFilePath).equals(diffFile.getBRelativePath())) {
          diffHunksInFile.forEach(diffHunk -> diffHunk.setFileIndex(diffFile.getIndex()));
          diffFile.setDiffHunks(diffHunksInFile);
          diffFile.setRawHeaders(headers);
        }
      }
    }
    return allDiffHunks;
  }

  /**
   * Split the raw hunk lines into 0 (leading neutral), 1 (from), 2 (to), 3 (trailing neural)
   *
   * @param lines
   * @return
   */
  private List<List<String>> splitHunkLines(List<Line> lines) {
    List<List<String>> result = new ArrayList<>();
    for (int i = 0; i < 4; ++i) {
      result.add(new ArrayList<>());
    }

    boolean trailing = false;
    // to handle case where two neighboring diff hunks are merged if the lines between them are less
    // than the -Ux
    boolean isLastLineNeutral = true;
    for (int i = 0; i < lines.size(); ++i) {
      Line line = lines.get(i);
      switch (line.getLineType()) {
        case NEUTRAL:
          boolean isNextLineNeutral = true;
          if (!isLastLineNeutral) {
            // check if the neutral lies between two non-netural lines
            if (i + 1 < lines.size()) {
              Line nextLine = lines.get(i + 1);
              isNextLineNeutral = nextLine.getLineType().equals(Line.LineType.NEUTRAL);
            }
          }
          if (!isLastLineNeutral && !isNextLineNeutral) {
            isLastLineNeutral = true;
            continue;
          } else {
            if (!line.getContent().trim().equals("\\ No newline at end of file")) {
              if (trailing) {
                result.get(3).add(line.getContent());
              } else {
                result.get(0).add(line.getContent());
              }
              isLastLineNeutral = true;
            }
          }
          break;
        case FROM:
          result.get(1).add(line.getContent());
          trailing = true;
          isLastLineNeutral = false;
          break;
        case TO:
          result.get(2).add(line.getContent());
          trailing = true;
          isLastLineNeutral = false;
          break;
      }
    }
    return result;
  }

  /**
   * Get the diff hunks between one commit and its previous commit
   *
   * @param repoPath
   * @param commitID
   * @return
   */
  @Override
  public List<DiffHunk> getDiffHunksAtCommit(
      String repoPath, String commitID, List<DiffFile> diffFiles) {
    // git diff <start_commit> <end_commit>
    // on Windows the ~ character must be used instead of ^
    String diffOutput =
        ignoreWhiteChanges
            ? SysUtil.runSystemCommand(
                repoPath,
                StandardCharsets.UTF_8,
                "git",
                "diff",
                "--ignore-cr-at-eol",
                "--ignore-all-space",
                "--ignore-blank-lines",
                "--ignore-space-change",
                "-U0",
                getParentCommitID(repoPath, commitID),
                commitID)
            : SysUtil.runSystemCommand(
                repoPath, StandardCharsets.UTF_8, "git", "diff", "-U0", commitID + "~", commitID);
    List<Diff> diffs = new ArrayList<>();
    if (!diffOutput.trim().isEmpty()) {
      // with -U0 (no context lines), the generated patch cannot be applied successfully
      DiffParser parser = new UnifiedDiffParser();
      diffs = parser.parse(new ByteArrayInputStream(diffOutput.getBytes()));
    }

    return generateDiffHunks(repoPath, diffs, diffFiles);
  }

  /**
   * Get the file content at HEAD
   *
   * @param relativePath
   * @return
   */
  @Override
  public String getContentAtHEAD(Charset charset, String repoDir, String relativePath) {
    return SysUtil.runSystemCommand(repoDir, charset, "git", "show", "HEAD:" + relativePath);
  }

  /**
   * Get the file content at one specific commit
   *
   * @param relativePath
   * @return
   */
  @Override
  public String getContentAtCommit(
      Charset charset, String repoDir, String relativePath, String commitID) {
    // TOFIX: if there is any error, like not exist in this commit, error message will be content
    return SysUtil.runSystemCommand(repoDir, charset, "git", "show", commitID + ":" + relativePath);
  }

  /**
   * Remove the "a/" or "b/" at the beginning of the path printed in Git
   *
   * @return
   */
  private String removeVersionLabel(String gitFilePath) {
    String trimmedPath = gitFilePath.trim();
    if (trimmedPath.startsWith("a/")) {
      return gitFilePath.replaceFirst("a/", "");
    }
    if (trimmedPath.startsWith("b/")) {
      return gitFilePath.replaceFirst("b/", "");
    }
    if (trimmedPath.equals("/dev/null")) {
      return "";
    }
    return gitFilePath;
  }

  /**
   * Make the working dir clean by dropping all the changes (which are backed up in tempDir/current)
   *
   * @param repoPath
   */
  public boolean clearWorkingTree(String repoPath) {
    SysUtil.runSystemCommand(repoPath, StandardCharsets.UTF_8, "git", "reset", "--hard");
    String status =
        SysUtil.runSystemCommand(
            repoPath, StandardCharsets.UTF_8, "git", "status", "--porcelain", "-uall");
    // working tree clean if empty
    return status.isEmpty();
  }

  @Override
  public String getCommitterName(String repoDir, String commitID) {
    // git show HEAD | grep Author
    // git log -1 --format='%an' HASH
    // git show -s --format='%an' HASH
    return SysUtil.runSystemCommand(
            repoDir, StandardCharsets.UTF_8, "git", "show", "-s", "--format='%an'", commitID)
        .trim()
        .replaceAll("'", "");
  }

  @Override
  public String getCommitterEmail(String repoDir, String commitID) {
    // git log -1 --format='%ae' HASH
    // git show -s --format='%ae' HASH
    return SysUtil.runSystemCommand(
            repoDir, StandardCharsets.UTF_8, "git", "show", "-s", "--format='%ae'", commitID)
        .trim()
        .replaceAll("'", "");
  }

  private boolean checkBinaryFileByDiff(String repoPath, String filePath, Charset charset) {
    String output =
        SysUtil.runSystemCommand(repoPath, charset, "git", "diff", "-U0", "--", filePath);
    // e.g. Binary files a/11.png and /dev/null differ
    if (output.trim().contains("Binary files")) {
      return true;
    } else {
      return false;
    }
  }

  @Override
  public List<String> getCommitsChangedFile(
      String repoDir, String filePath, String beforeCommit, int... maxNumber) {
    String output = "";
    if (maxNumber.length == 0) {
      output =
          SysUtil.runSystemCommand(
              repoDir, StandardCharsets.UTF_8, "git", "rev-list", beforeCommit, filePath);
    } else if (maxNumber.length == 1) {
      output =
          SysUtil.runSystemCommand(
              repoDir,
              StandardCharsets.UTF_8,
              "git",
              "rev-list",
              "--max-count",
              String.valueOf(maxNumber[0]),
              beforeCommit,
              filePath);
    } else {
      logger.error("The maxNumber argument should be empty or one single number!");
      return new ArrayList<>();
    }

    return DiffUtil.convertStringToList(output);
  }

  @Override
  public List<String> getCommitsChangedLineRange(
      String repoDir, String filePath, int startLine, int endLine) {
    // git log --pretty=format:"%H" -u -L <start_line_number>,<ending_line_number>:<filename>
    // --no-patch
    // git log--format=format:%H -u -L <start_line_number>,<ending_line_number>:<filename>
    // --no-patch
    String output =
        SysUtil.runSystemCommand(
            repoDir,
            StandardCharsets.UTF_8,
            "git",
            "log",
            "--format=format:%H",
            "-u",
            "-L",
            startLine + "," + endLine + ":" + filePath,
            "--no-patch");
    if (output.trim().startsWith("fatal") || output.trim().isEmpty()) {
      return new ArrayList<>();
    } else {
      return DiffUtil.convertStringToList(output);
    }
  }

  /**
   * Return magic commit id for the very first commit, or commitID + "~"
   *
   * @param commitID
   * @return
   */
  private String getParentCommitID(String repoDir, String commitID) {
    // TODO: refactor GitService to save repoDir and fixed data
    String firstCommitID =
        SysUtil.runSystemCommand(
                repoDir, StandardCharsets.UTF_8, "git", "rev-list", "--max-parents=0", "HEAD")
            .trim();
    if (commitID.equals(firstCommitID)) {
      return "4b825dc642cb6eb9a060e54bf8d69288fbee4904"; // what the fck?
    } else {
      return commitID + "~";
    }
  }
}
