package edu.pku.code2graph.diff.cochange;

import edu.pku.code2graph.diff.model.DiffFile;
import gumtree.spoon.AstComparator;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.operations.Operation;
import org.apache.commons.lang3.tuple.Pair;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtTypeMember;
import spoon.support.reflect.declaration.CtTypeImpl;

import java.util.HashSet;
import java.util.Set;

public class JavaDiffUtil {
    /**
     * Compute diff and return edit script with gumtree spoon
     *
     * @return
     */
    public static Set<Pair<String, String>> computeJavaChanges(DiffFile diffFile) {
        Set<Pair<String, String>> results = new HashSet<>();

        Diff editScript = new AstComparator().compare(diffFile.getAContent(), diffFile.getBContent());
        // build GT: process Java changes to file/type/member
        for (Operation operation : editScript.getRootOperations()) { // or allOperations?
            Pair<String, String> names = findWrappedTypeAndMemberNames(operation.getSrcNode());
            results.add(names);
        }
        return results;
    }

    public static Pair<String, String> findWrappedTypeAndMemberNames(CtElement element) {
        if (element instanceof CtTypeImpl) {
            return Pair.of(((CtTypeImpl) element).getQualifiedName(), "");
        } else if (element instanceof CtTypeMember) {
            CtTypeMember member = (CtTypeMember) element;
            return Pair.of(member.getDeclaringType().getQualifiedName(), member.getSimpleName());
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
                    parentType != null ? ((CtTypeImpl) parentType).getQualifiedName() : "";
            String parentMemberName =
                    parentMember != null ? ((CtTypeMember) parentMember).getSimpleName() : "";
            return Pair.of(parentTypeName, parentMemberName);
        }
        //    String nodeType = element.getClass().getSimpleName();
        //    nodeType = nodeType.substring(2, nodeType.length() - 4);
    }
}
