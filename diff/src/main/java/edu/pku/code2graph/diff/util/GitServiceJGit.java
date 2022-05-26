package edu.pku.code2graph.diff.util;

import edu.pku.code2graph.diff.model.DiffFile;
import edu.pku.code2graph.diff.model.DiffHunk;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of helper functions based on jGit (the java implementation of Git). Might be
 * faster due to the saving for spawning OS process in shell script.
 */
public class GitServiceJGit implements GitService {
  private final String repoPath;

  public GitServiceJGit(String repoPath) {
    this.repoPath = repoPath;
  }

  @Override
  public ArrayList<DiffFile> getChangedFilesInWorkingTree() {
    return null;
  }

  @Override
  public ArrayList<DiffFile> getChangedFilesAtCommit(String commitID) {
    return null;
  }

  @Override
  public List<DiffHunk> getDiffHunksInWorkingTree(List<DiffFile> diffFiles) {
    return null;
  }

  @Override
  public List<DiffHunk> getDiffHunksAtCommit(String commitID, List<DiffFile> diffFiles) {
    return null;
  }

  @Override
  public String getContentAtHEAD(Charset charset, String relativePath) {
    return null;
  }

  @Override
  public String getContentAtCommit(Charset charset, String relativePath, String commitID) {
    return null;
  }

  @Override
  public String getCommitterName(String commitID) {
    RevCommit commit = getCommitFromID(commitID);
    if (null != commit) {
      return commit.getAuthorIdent().getName();
    }
    return "";
  }

  @Override
  public String getCommitterEmail(String commitID) {
    RevCommit commit = getCommitFromID(commitID);
    if (null != commit) {
      return commit.getAuthorIdent().getEmailAddress();
    }
    return "";
  }

  private RevCommit getCommitFromID(String commitID) {
    Path path = Paths.get(repoPath);
    try (Git git = Git.open(path.toFile())) {
      Repository repository = git.getRepository();
      RevWalk walk = new RevWalk(repository);
      return walk.parseCommit(repository.resolve(commitID));
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  public Repository cloneIfNotExists(String projectPath, String cloneUrl /*, String branch*/)
      throws Exception {
    File folder = new File(projectPath);
    Repository repository;
    if (folder.exists()) {
      RepositoryBuilder builder = new RepositoryBuilder();
      repository =
          builder.setGitDir(new File(folder, ".git")).readEnvironment().findGitDir().build();

      logger.info(
          "Project {} is already cloned, current branch is {}", cloneUrl, repository.getBranch());

    } else {
      logger.info("Cloning {} ...", cloneUrl);
      Git git =
          Git.cloneRepository()
              .setDirectory(folder)
              .setURI(cloneUrl)
              .setCloneAllBranches(true)
              .call();
      repository = git.getRepository();
      logger.info("Done cloning {}, current branch is {}", cloneUrl, repository.getBranch());
    }

    //		if (branch != null && !repository.getBranch().equals(branch)) {
    //			Git git = new Git(repository);
    //
    //			String localBranch = "refs/heads/" + branch;
    //			List<Ref> refs = git.branchList().call();
    //			boolean branchExists = false;
    //			for (Ref ref : refs) {
    //				if (ref.getName().equals(localBranch)) {
    //					branchExists = true;
    //				}
    //			}
    //
    //			if (branchExists) {
    //				git.checkout()
    //					.setName(branch)
    //					.call();
    //			} else {
    //				git.checkout()
    //					.setCreateBranch(true)
    //					.setName(branch)
    //					.setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
    //					.setStartPoint("origin/" + branch)
    //					.call();
    //			}
    //
    //			logger.info("Project {} switched to {}", cloneUrl, repository.getBranch());
    //		}
    return repository;
  }

  @Override
  public List<String> getCommitsChangedFile(
      String filePath, String beforeCommit, int... maxNumber) {
    return new ArrayList<>();
  }

  @Override
  public List<String> getCommitsChangedLineRange(String filePath, int startLine, int endLine) {
    return new ArrayList<>();
  }

  @Override
  public String getHEADCommitId() {
    return "HEAD";
  }

  @Override
  public String getLongHEADCommitId() {
    return "HEAD";
  }

  @Override
  public boolean checkoutByCommitID(String commitID) {
    return false;
  }

  @Override
  public boolean checkoutByLongCommitID(String commitID) {
    return false;
  }

  @Override
  public List<String> getCommitHistory() {
    return null;
  }
}
