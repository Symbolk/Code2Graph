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
import edu.pku.code2graph.diff.util.MetricUtil;
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
      String tag = getTagForTree(iTree.getParent());
      if (isIDLabel(iTree.getLabel())) {
        results.add(new XMLDiff(ChangeType.DELETED, fileName, tag, iTree.getLabel()));
      }
    }
    for (ITree iTree : actionClassifier.dstAddTrees) {
      if (isIDLabel(iTree.getLabel())) {
        String tag = getTagForTree(iTree.getParent());
        // the element tree
        // find other contextNodes with the similar semantics
        Map<String, Double> contextNodes =
            findContextNodeIDs(
                aContext.getRoot(), iTree.getParent().getParent(), tag, iTree.getLabel(), 5);
        results.add(new XMLDiff(ChangeType.ADDED, fileName, tag, iTree.getLabel(), contextNodes));
      }
    }

    for (ITree iTree : actionClassifier.srcUpdTrees) {
      if (isIDLabel(iTree.getLabel())) {
        String tag = getTagForTree(iTree.getParent());

        results.add(
            new XMLDiff(
                ChangeType.UPDATED,
                fileName,
                tag,
                iTree.getLabel(),
                findContextNodeIDs(
                    aContext.getRoot(), iTree.getParent().getParent(), tag, iTree.getLabel(), 5)));
      }
    }

    for (ITree iTree : actionClassifier.dstUpdTrees) {
      if (isIDLabel(iTree.getLabel())) {
        String tag = getTagForTree(iTree.getParent());
        results.add(
            new XMLDiff(
                ChangeType.UPDATED,
                fileName,
               tag,
                iTree.getLabel(),
                findContextNodeIDs(
                    aContext.getRoot(), iTree.getParent().getParent(), tag, iTree.getLabel(), 5)));
      }
    }

    return results;
  }

  /**
   * Get the type of an element by finding its nearest uncle with no children
   *
   * @param iTree
   * @return
   */
  private static String getTagForTree(ITree iTree) {
    if (iTree == null || iTree.getParent() == null) {
      return "";
    }

    ITree parent = iTree.getParent();
    while (parent != null) {
      for (ITree uncle : parent.getChildren()) {
        if (uncle.getChildren().size() == 0) {
          return uncle.getLabel();
        }
      }
      parent = parent.getParent();
    }
    return "";
  }

  /**
   * Find and return the top-k siblings
   *
   * @param root
   * @param targetTree
   * @return
   */
  private static Map<String, Double> findContextNodeIDs(
      ITree root, ITree targetTree, String targetTag, String targetID, int k) {
    Map<String, Double> results = new LinkedHashMap<>();

    PriorityQueue<Pair<String, Double>> pq = new PriorityQueue<>(new PairComparator());
    if (root == null) {
      return results;
    }

    // BFS for level traversal
    ArrayDeque<ITree> queue = new ArrayDeque<>();
    queue.add(root);
    while (!queue.isEmpty()) {
      ITree temp = queue.poll();
      String tid = getIDForTree(temp);
      // do not put itself into context nodes
      if (!tid.isEmpty() && !tid.equals(targetID)) {
        double similarity =
            MetricUtil.formatDouble(
                (MetricUtil.cosineString(getTagForTree(temp), targetTag)
                        + TreeSimilarityMetrics.treeSimilarity(temp, targetTree))
                    / 2);
        pq.add(Pair.of(tid, similarity));
      }
      if (!temp.getChildren().isEmpty()) {
        queue.addAll(temp.getChildren());
      }
    }

    while (!pq.isEmpty() && k > 0) {
      Pair<String, Double> pair = pq.poll();
      results.put(pair.getLeft(), pair.getRight());
      k--;
    }
    return results;
  }

  private static String getIDForTree(ITree tree) {
    for (ITree t : tree.getChildren()) {
      if (isIDLabel(t.getLabel())) {
        return t.getLabel().replace("\"", "").replace("+", "");
      }
    }
    return "";
  }

  public static boolean isIDLabel(String label) {
    return label != null && label.startsWith("\"@+id");
  }
}
