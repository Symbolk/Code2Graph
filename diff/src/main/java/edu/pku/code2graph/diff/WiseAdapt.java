package edu.pku.code2graph.diff;

import com.github.gumtreediff.actions.ActionClusterFinder;
import com.github.gumtreediff.actions.ChawatheScriptGenerator;
import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.gen.jdt.JdtTreeGenerator;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.TreeContext;
import edu.pku.code2graph.diff.model.DiffFile;
//import gr.uom.java.xmi.UMLModel;
//import gr.uom.java.xmi.UMLModelASTReader;
//import gr.uom.java.xmi.diff.UMLModelDiff;
//import org.refactoringminer.api.Refactoring;
//import org.refactoringminer.api.RefactoringMinerTimedOutException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public class WiseAdapt {

  public static void main(String[] args) {
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
    for (DiffFile diffFile : diffFiles) {
      EditScript es = generateEditScript(diffFile.getAContent(), diffFile.getBContent());
      if (es != null) {
        ActionClusterFinder f = new ActionClusterFinder(es);
        for (Set<Action> cluster : f.getClusters()) {
          cluster.forEach(System.out::println);
        }
      }
    }

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

  /**
   * Compute diff and return edit script
   *
   * @param aContent
   * @param bContent
   * @return
   */
  private static EditScript generateEditScript(String aContent, String bContent) {
    //        Run.initGenerators();
    JdtTreeGenerator generator = new JdtTreeGenerator();
    //        Generators generator = Generators.getInstance();

    try {
      TreeContext oldContext = generator.generateFrom().string(aContent);
      TreeContext newContext = generator.generateFrom().string(bContent);
      Matcher matcher = Matchers.getInstance().getMatcher();

      MappingStore mappings = matcher.match(oldContext.getRoot(), newContext.getRoot());
      EditScript editScript = new ChawatheScriptGenerator().computeActions(mappings);

      return editScript;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }
}
