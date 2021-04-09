package edu.pku.code2graph.diff.cochange;

import com.github.gumtreediff.actions.ActionGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.gen.TreeGenerator;
import com.github.gumtreediff.gen.xml.XmlTreeGenerator;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import edu.pku.code2graph.diff.model.ChangeType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class XMLDiffUtil {
  /**
   * Compute diff and return edit script
   *
   * @param aContent
   * @param bContent
   * @return
   */
  public static List<XMLDiff> computeXMLChangesWithGumtree(String aContent, String bContent)
      throws IOException {
    List<XMLDiff> results = new ArrayList<>();

    TreeGenerator generator = new XmlTreeGenerator();
    TreeContext aContext = generator.generateFromString(aContent);
    TreeContext bContext = generator.generateFromString(bContent);
    //      Matcher matcher = Matchers.getInstance().getMatcher();
    ITree aRoot = aContext.getRoot();
    ITree bRoot = bContext.getRoot();
    Matcher matcher = Matchers.getInstance().getMatcher(aRoot, bRoot);
    aContext.importTypeLabels(bContext);
    matcher.match();
    final ActionGenerator actionGenerator =
        new ActionGenerator(aRoot, bRoot, matcher.getMappings());
    List<Action> actions = actionGenerator.generate();

    MyActionClassifier actionClassifier =
        new MyActionClassifier(matcher.getMappingsAsSet(), actions);
    List<Action> rootActions = actionClassifier.getRootActions();

    //      ActionClusterFinder actionClusterFinder =
    //          new ActionClusterFinder(aContext, bContext, actionClassifier.getRootActions());
    //      actionClusterFinder.getClusters();
    //      EditScript editScript = new ChawatheScriptGenerator().computeActions(
    // matcher.getMappings());
    for (ITree iTree : actionClassifier.srcDelTrees) {
      if (isIDLabel(iTree.getLabel())) {
        results.add(new XMLDiff(ChangeType.DELETED, iTree.getLabel()));
      }
    }
    for (ITree iTree : actionClassifier.dstAddTrees) {
      if (isIDLabel(iTree.getLabel())) {
        results.add(new XMLDiff(ChangeType.ADDED, iTree.getLabel()));
      }
    }

    for (ITree iTree : actionClassifier.srcUpdTrees) {
      if (isIDLabel(iTree.getLabel())) {
        results.add(new XMLDiff(ChangeType.UPDATED, iTree.getLabel()));
      }
    }

    return results;
  }

  public static boolean isIDLabel(String label) {
    return label != null && label.startsWith("\"@+id");
  }
}
