package edu.pku.code2graph.diff.cochange;

import com.github.gumtreediff.actions.model.*;
import edu.pku.code2graph.diff.model.ChangeType;
import edu.pku.code2graph.diff.model.DiffFile;
import edu.pku.code2graph.diff.util.DiffUtil;
import gumtree.spoon.AstComparator;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.operations.Operation;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtEnumValue;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.path.CtRole;
import spoon.support.reflect.declaration.CtTypeImpl;

import java.util.*;

public class JavaDiffUtil {
  /**
   * Compute diff and return edit script with gumtree spoon
   *
   * @return
   */
  public static List<JavaDiff> computeJavaChanges(DiffFile diffFile) {
    Set<JavaDiff> results = new LinkedHashSet<>();
    if (!diffFile.getARelativePath().isEmpty() && containsViewChanges(diffFile)) {
      Set<ChangeType> changeTypes = new HashSet<>();
      Diff editScript = new AstComparator().compare(diffFile.getAContent(), diffFile.getBContent());
      // build GT: process Java changes to file/type/member
      for (Operation operation : editScript.getRootOperations()) { // or allOperations?
        Triple<ChangeType, String, String> change = extractEntityChange(operation);
        if (change.getLeft().equals(ChangeType.NONE)) {
          continue;
        }
        changeTypes.add(change.getLeft());
        results.add(
            new JavaDiff(
                change.getLeft(),
                diffFile.getARelativePath(),
                change.getMiddle(),
                change.getRight()));
      }
      changeTypes.remove(ChangeType.ADDED);
      if (changeTypes.isEmpty()) {
        return new ArrayList<>();
      }
    }
    return new ArrayList<>(results);
  }

  private static boolean containsViewChanges(DiffFile diffFile) {
    String regex = "import\\s*([^;])*\\.R;";
    for (String line : DiffUtil.convertStringToList(diffFile.getAContent())) {
      if (line.trim().matches(regex) || line.contains("R.")) {
        return true;
      }
    }
    for (String line : DiffUtil.convertStringToList(diffFile.getBContent())) {
      if (line.trim().matches(regex) || line.contains("R.")) {
        return true;
      }
    }
    return false;
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
    if (element instanceof CtLiteral
        || element instanceof CtEnumValue
        || isIrrelevantChange(element)) {
      return Triple.of(ChangeType.NONE, "", "");
    }

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

  private static boolean isIrrelevantChange(CtElement element) {
    CtRole role = element.getRoleInParent();
    Set<CtRole> irrelevantRoles =
        new HashSet<>(
            Arrays.asList(
                CtRole.MODIFIER,
                CtRole.EMODIFIER,
                CtRole.DECLARED_IMPORT,
                CtRole.DECLARED_IMPORT,
                CtRole.COMMENT));
    return irrelevantRoles.contains(role);
  }

  /**
   * Since Gumtree has $ in its type name
   *
   * @return
   */
  private static String convertTypeName(String name) {
    return StringUtils.removeEnd(name, CtTypeImpl.INNERTTYPE_SEPARATOR)
        .replaceAll("\\" + CtTypeImpl.INNERTTYPE_SEPARATOR, ".")
        .replaceAll("[.\\d*]+$", ""); // there maybe multiple numbers at the end
  }
}
