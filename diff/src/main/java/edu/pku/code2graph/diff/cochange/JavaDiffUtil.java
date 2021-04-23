package edu.pku.code2graph.diff.cochange;

import com.github.gumtreediff.actions.model.*;
import edu.pku.code2graph.diff.model.ChangeType;
import edu.pku.code2graph.diff.model.DiffFile;
import gumtree.spoon.AstComparator;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.operations.Operation;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtTypeMember;
import spoon.support.reflect.declaration.CtTypeImpl;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class JavaDiffUtil {
  /**
   * Compute diff and return edit script with gumtree spoon
   *
   * @return
   */
  public static List<JavaDiff> computeJavaChanges(DiffFile diffFile) {
    Set<JavaDiff> results = new LinkedHashSet<>();

    Diff editScript = new AstComparator().compare(diffFile.getAContent(), diffFile.getBContent());
    // build GT: process Java changes to file/type/member
    for (Operation operation : editScript.getRootOperations()) { // or allOperations?
      Triple<ChangeType, String, String> change = extractEntityChange(operation);
      results.add(
          new JavaDiff(
              change.getLeft(),
              diffFile.getARelativePath().isEmpty()
                  ? diffFile.getBRelativePath()
                  : diffFile.getARelativePath(),
              change.getMiddle(),
              change.getRight()));
    }
    return new ArrayList<>(results);
  }

  private static ChangeType getChangeType(Action action) {
    if (action instanceof Insert) {
      return ChangeType.ADDED;
    } else if (action instanceof Delete) {
      return ChangeType.DELETED;
    } else if (action instanceof Update) {
      return ChangeType.UPDATED;
    } else if (action instanceof Move) {
      return ChangeType.MOVED;
    } else {
      return ChangeType.UNKNOWN;
    }
  }

  /**
   * Extract entity level change
   *
   * @return
   */
  public static Triple<ChangeType, String, String> extractEntityChange(Operation operation) {
    CtElement element = operation.getSrcNode();
    if (element instanceof CtTypeImpl) {
      return Triple.of(
          getChangeType(operation.getAction()),
          convertTypeName(((CtTypeImpl) element).getQualifiedName()),
          "");
    } else if (element instanceof CtTypeMember) {
      CtTypeMember member = (CtTypeMember) element;
      return Triple.of(
          getChangeType(operation.getAction()),
          convertTypeName(member.getDeclaringType().getQualifiedName()),
          member.getSimpleName());
    } else {
      // find parent member and type
      CtElement parentType = element.getParent();
      CtElement parentMember = element.getParent();

      while (parentType != null && !(parentType instanceof CtTypeImpl)) {
        parentType = parentType.getParent();
      }
      while (parentMember != null && !(parentMember instanceof CtTypeMember)) {
        parentMember = parentMember.getParent();
      }

      String parentTypeName =
          parentType != null ? convertTypeName(((CtTypeImpl) parentType).getQualifiedName()) : "";
      String parentMemberName =
          parentMember != null ? ((CtTypeMember) parentMember).getSimpleName() : "";
      return Triple.of(ChangeType.UPDATED, parentTypeName, parentMemberName);
    }
    //    String nodeType = element.getClass().getSimpleName();
    //    nodeType = nodeType.substring(2, nodeType.length() - 4);
  }

  /**
   * Since Gumtree has $ in its type name
   *
   * @return
   */
  private static String convertTypeName(String name) {
    return StringUtils.removeEnd(name, CtTypeImpl.INNERTTYPE_SEPARATOR)
        .replaceAll("\\" + CtTypeImpl.INNERTTYPE_SEPARATOR, ".")
        .replaceAll("\\.\\d*$", "");
  }
}
