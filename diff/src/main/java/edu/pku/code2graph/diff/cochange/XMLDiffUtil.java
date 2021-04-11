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
import edu.pku.code2graph.diff.model.DiffFile;
import edu.pku.code2graph.util.FileUtil;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.*;

public class XMLDiffUtil {
  /**
   * Compute diff and return edit script
   *
   * @return
   */
  public static List<XMLDiff> computeXMLChangesWithGumtree(DiffFile diffFile) throws IOException {

    List<XMLDiff> results = new ArrayList<>();
    String fileName = FileUtil.getFileNameFromPath(diffFile.getARelativePath());

    TreeGenerator generator = new XmlTreeGenerator();
    TreeContext aContext = generator.generateFromString(diffFile.getAContent());
    TreeContext bContext = generator.generateFromString(diffFile.getBContent());
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
        results.add(new XMLDiff(ChangeType.DELETED, fileName, iTree.getLabel()));
      }
    }
    for (ITree iTree : actionClassifier.dstAddTrees) {
      if (isIDLabel(iTree.getLabel())) {
        // the element tree
        // find other ids with the similar semantics
        List<Pair<String, Double>> ids =
            findSiblingNodeIDs(aContext.getRoot(), iTree.getParent().getParent(), 3);
        results.add(new XMLDiff(ChangeType.ADDED, fileName, iTree.getLabel(), ids));
      }
    }

    for (ITree iTree : actionClassifier.srcUpdTrees) {
      if (isIDLabel(iTree.getLabel())) {
        results.add(new XMLDiff(ChangeType.UPDATED, fileName, iTree.getLabel()));
      }
    }

    return results;
  }

  static class PairComparator implements Comparator<Pair<String, Double>> {
    @Override
    public int compare(Pair<String, Double> p1, Pair<String, Double> p2) {
      if (p1.getRight() < p2.getRight()) {
        return 1;
      } else if (p1.getRight() > p2.getRight()) {
        return -1;
      }
      return 0;
    }
  }

  /**
   * Find and return the top-k siblings
   *
   * @param root
   * @param targetTree
   * @return
   */
  private static List<Pair<String, Double>> findSiblingNodeIDs(
      ITree root, ITree targetTree, int k) {
    List<Pair<String, Double>> results = new ArrayList<>();

    PriorityQueue<Pair<String, Double>> pq = new PriorityQueue<>(new PairComparator());
    if (root == null) {
      return results;
    }

    // BFS for level traversal
    ArrayDeque<ITree> queue = new ArrayDeque<>();
    queue.add(root);
    while (!queue.isEmpty()) {
      ITree temp = queue.poll();
      if (temp.hasSameType(targetTree)) {
        double similarity = TreeSimilarityMetrics.treeSimilarity(temp, targetTree);
        pq.add(Pair.of(getIDFromTree(temp), similarity));
      }
      if (!temp.getChildren().isEmpty()) {
        queue.addAll(temp.getChildren());
      }
    }

    while (!pq.isEmpty() && k > 0) {
      results.add(pq.poll());
      k--;
    }
    return results;
  }

  private static String getIDFromTree(ITree tree) {
    for (ITree t : tree.getDescendants()) {
      if (isIDLabel(t.getLabel())) {
        return t.getLabel();
      }
    }
    return "";
  }

  public static boolean isIDLabel(String label) {
    return label != null && label.startsWith("\"@+id");
  }
}
