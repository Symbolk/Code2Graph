package edu.pku.code2graph.diff;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class Evaluation {
  public static void main(String[] args) {
    // filter commits with layout xml changes and J/K changes

    // save ground truth

    // run and compare results
    listCommits("/Users/symbolk/coding/data/repos/MLManager");
  }

  /**
   * Yet another way to list all commits
   *
   * @param repoPath
   */
  private static void listCommits(String repoPath) {
    Path path = Paths.get(repoPath);
    try (Git git = Git.open(path.toFile())) {
      Iterable<RevCommit> commits = git.log().all().call();
      Repository repository = git.getRepository();
      String branch = repository.getBranch();
      System.out.println(branch);
      int total = 0;
      int cnt = 0;
      for (Iterator<RevCommit> iter = commits.iterator(); iter.hasNext(); ) {
        RevCommit commit = iter.next();
        String commitID = commit.getId().getName();
        //        System.out.println(commit.getAuthorIdent());
        //        System.out.println(commit.getFullMessage());
        ObjectReader reader = git.getRepository().newObjectReader();
        CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
        ObjectId oldTree = git.getRepository().resolve(commitID + "~1^{tree}");
        //        ObjectId oldTree = git.getRepository().resolve("HEAD~1^{tree}");
        if (oldTree == null) {
          break;
        }
        oldTreeIter.reset(reader, oldTree);
        CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
        ObjectId newTree = git.getRepository().resolve(commitID + "^{tree}");
        //        ObjectId newTree = git.getRepository().resolve("HEAD^{tree}");
        newTreeIter.reset(reader, newTree);

        DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE);
        diffFormatter.setRepository(git.getRepository());
        List<DiffEntry> entries = diffFormatter.scan(oldTreeIter, newTreeIter);

        total += 1;
        Set<String> fileTypes = new HashSet<>();
        for (DiffEntry entry : entries) {
          fileTypes.add(FilenameUtils.getExtension(entry.getOldPath()));
          fileTypes.add(FilenameUtils.getExtension(entry.getNewPath()));
        }
        if ((fileTypes.contains("java") || fileTypes.contains("kt")) && fileTypes.contains("xml")) {
          System.out.println(commitID);
          System.out.println(fileTypes);
          cnt += 1;
        }
      }
      System.out.println(
          String.format("Total: %s, Cross: %s, Percent: %f", total, cnt, (double) cnt / total));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
