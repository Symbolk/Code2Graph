package edu.pku.code2graph.client;

import edu.pku.code2graph.diff.RepoAnalyzer;
import edu.pku.code2graph.diff.model.DiffFile;
import edu.pku.code2graph.exception.InvalidRepoException;
import edu.pku.code2graph.exception.NonexistPathException;
import org.apache.log4j.PropertyConfigurator;

import java.util.List;

public class WiseAdapt {

  public static void main(String[] args) throws NonexistPathException, InvalidRepoException {
    PropertyConfigurator.configure("log4j.properties");

    // input commit id
    String repoPath = System.getProperty("user.home") + "/coding/data/repos/cxf";
    String repoName = "cxf";
    String commitID = "ed4faad";
    String tempDir = System.getProperty("user.home") + "/coding/data/wa";
    // or input a workspace
    // clone repo if not exists
    // get all changed files and imported files

    RepoAnalyzer repoAnalyzer = new RepoAnalyzer(repoName, repoPath);
    List<DiffFile> diffFiles = repoAnalyzer.analyzeCommit(commitID);
    //    DataCollector dataCollector = new DataCollector(tempDir);
    //    Pair<List<String>, List<String>> tempFilePaths = dataCollector.collect(diffFiles);

    // compare changed files to get diff elements

    // detect refactorings

    // locate changed nodes

    // find defs and all uses

    // get contexts and encode embeddings

    // cluster embeddings

    // resolve defs/uses of only changed element nodes (types/methods/controls)

    // get all connected/induced subgraphs

    // check if all nodes in the quick fix commit involved in the subgraphs

    // find in changes itself

    // find in all involved scope

    // find co-change in history with blame

  }

  //  private void detectRefactorings(String aDir, String bDir) {
  //
  //    try {
  //      File rootFolder1 = new File(aDir);
  //      File rootFolder2 = new File(bDir);
  //
  //      UMLModel model1 = new UMLModelASTReader(rootFolder1).getUmlModel();
  //      UMLModel model2 = new UMLModelASTReader(rootFolder2).getUmlModel();
  //      UMLModelDiff modelDiff = model1.diff(model2);
  //
  //      List<Refactoring> refactorings = modelDiff.getRefactorings();
  //
  //      // for each refactoring, find the corresponding diff hunk
  //      for (Refactoring refactoring : refactorings) {
  //        // greedy style: put all refactorings into one group
  //      }
  //    } catch (RefactoringMinerTimedOutException | IOException e) {
  //      e.printStackTrace();
  //    }
  //  }

}
