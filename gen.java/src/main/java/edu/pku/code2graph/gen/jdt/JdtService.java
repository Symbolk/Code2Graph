package edu.pku.code2graph.gen.jdt;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.core.dom.*;

import java.util.*;
import java.lang.*;

public class JdtService {

  /**
   * Get the qname for var declaration, which can be inside a method or init block
   *
   * @param binding
   * @param node
   * @return
   */
  public static String getVariableQNameFromBinding(IVariableBinding binding, ASTNode node) {
    String qname = binding.getName();
    if (binding.getDeclaringMethod() != null) {
      qname = getMethodQNameFromBinding(binding.getDeclaringMethod()) + "." + qname;
    } else if (isInsideInitBlock(node)) {
      qname = getParentInitBlockName(node) + ".INIT." + qname;
    }
    return qname;
  }

  /**
   * Get the qname of a method from binding
   *
   * @param binding
   * @return
   */
  public static String getMethodQNameFromBinding(IMethodBinding binding) {
    if (binding != null) {
      ITypeBinding[] paraBindings = binding.getParameterTypes();
      List<String> paraTypes = new ArrayList<>();
      for (ITypeBinding b : paraBindings) {
        paraTypes.add(b.getQualifiedName());
      }

      String qname = binding.getName() + "(" + String.join(",", paraTypes).trim() + ")";
      qname = binding.getDeclaringClass().getQualifiedName() + "." + qname;
      return qname;
    }
    return "";
  }

  private static String getParentInitBlockName(ASTNode node) {
    ASTNode parent = node.getParent();
    while (parent != null && parent.getNodeType() != ASTNode.TYPE_DECLARATION) {
      parent = parent.getParent();
    }
    if (parent.getNodeType() == ASTNode.TYPE_DECLARATION) {
      TypeDeclaration typeDeclaration = (TypeDeclaration) parent;
      return typeDeclaration.resolveBinding().getQualifiedName();
    }
    return "";
  }

  private static boolean isInsideInitBlock(ASTNode node) {
    ASTNode parent = node.getParent();
    while (parent != null && parent.getNodeType() != ASTNode.TYPE_DECLARATION) {
      if (parent.getNodeType() == ASTNode.INITIALIZER) {
        return true;
      } else {
        parent = parent.getParent();
      }
    }
    return false;
  }

  public static String getTypeQNameFromParents(TypeDeclaration td) {
    String qname = td.getName().getFullyQualifiedName();
    // find wrapped type declaration or package declaration
    ASTNode parent = td.getParent();
    while (parent != null && parent.getNodeType() != ASTNode.COMPILATION_UNIT) {
      if (parent instanceof TypeDeclaration) {
        qname = ((TypeDeclaration) parent).getName().getFullyQualifiedName() + "." + qname;
      } else {
        parent = parent.getParent();
      }
    }
    if (parent instanceof CompilationUnit) {
      CompilationUnit cu = (CompilationUnit) parent;
      if (cu.getPackage() != null) {
        qname = cu.getPackage().getName().getFullyQualifiedName() + "." + qname;
      }
    }
    return qname;
  }

  public static String getIdentifier(ASTNode node) {
    List<String> list = new ArrayList<>();
    while (node != null) {
      SimpleName name = null;
      if (node instanceof TypeDeclaration) {
        name = ((TypeDeclaration) node).getName();
      } else if (node instanceof MethodDeclaration) {
        name = ((MethodDeclaration) node).getName();
      } else if (node instanceof EnumDeclaration) {
        name = ((EnumDeclaration) node).getName();
      } else if (node instanceof EnumConstantDeclaration) {
        name = ((EnumConstantDeclaration) node).getName();
      } else if (node instanceof SingleVariableDeclaration) {
        name = ((SingleVariableDeclaration) node).getName();
      } else if (node instanceof VariableDeclarationFragment) {
        name = ((VariableDeclarationFragment) node).getName();
      }else if(node instanceof AnnotationTypeDeclaration) {
        name = ((AnnotationTypeDeclaration) node).getName();
      }
      if (name != null) {
        list.add(name.getFullyQualifiedName());
      }
      node = node.getParent();
    }
    Collections.reverse(list);
    return StringUtils.join(list, "/");
  }
}
