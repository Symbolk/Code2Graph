package edu.pku.code2graph.gen.jdt;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.core.dom.*;

import java.util.*;
import java.lang.*;

public class JdtService {
  /**
   * Find the parent method, return its qname
   *
   * @return
   */
  public static Optional<String> findWrappedMethodName(ASTNode node) {
    ASTNode parent = node.getParent();
    while (parent != null) {
      if (parent.getNodeType() == ASTNode.METHOD_DECLARATION) {
        return Optional.of(
            getMethodQNameFromBinding(((MethodDeclaration) parent).resolveBinding()));
      }
      parent = parent.getParent();
    }
    return Optional.empty();
  }

  /**
   * Find the residing expression statement with assignment of a qualified name
   *
   * @return return the qualified name of the assigned field
   */
  public static Optional<String> findAssignedFieldName(QualifiedName node) {
    ASTNode parent = node.getParent();
    while (parent != null) {
      if (parent instanceof ExpressionStatement) {
        break;
      } else if (parent instanceof Assignment) {
        Expression exp = ((Assignment) parent).getLeftHandSide();
        if (exp instanceof SimpleName) {
          IBinding binding = ((SimpleName) exp).resolveBinding();
          if (binding instanceof IVariableBinding) {
            IVariableBinding variableBinding = (IVariableBinding) binding;
            if (variableBinding.isField() && variableBinding.getDeclaringClass() != null) {
              return Optional.of(
                  variableBinding.getDeclaringClass().getQualifiedName()
                      + "."
                      + variableBinding.getName());
            }
          }
        }
      }
      parent = parent.getParent();
    }
    return Optional.empty();
  }

  public static Set<String> processWrappedStatement(QualifiedName node) {
    ASTNode parent = node.getParent();
    while (parent != null) {
      if (parent instanceof Statement) {
        break;
      }
      parent = parent.getParent();
    }

    if (parent instanceof SwitchCase) {
      StatementVisitor visitor = new StatementVisitor();
      SwitchCase switchCase = (SwitchCase) parent;
      // handle the statements before the next switchcase
      SwitchStatement switchStatement = (SwitchStatement) parent.getParent();
      List statements = switchStatement.statements();
      for (int i = 0; i < statements.size(); ++i) {
        if (statements.get(i).equals(switchCase)) {
          i += 1;
          while (i < statements.size() && !(statements.get(i) instanceof SwitchCase)) {
            ((Statement) statements.get(i)).accept(visitor);
            i++;
          }
          break;
        }
      }
      return visitor.getQNames();
    } else {

      if (parent != null) {
        Statement statement = (Statement) parent;
        StatementVisitor visitor = new StatementVisitor();
        statement.accept(visitor);
        return visitor.getQNames();
      }
    }
    return new HashSet<>();
  }

  public static Optional<String> findWrappedEntityName(ASTNode node) {
    ASTNode parent = node.getParent();
    while (parent != null) {
      //      if (parent instanceof BodyDeclaration) {
      if (parent instanceof VariableDeclarationFragment
          && parent.getParent() instanceof FieldDeclaration) {
        IVariableBinding binding = ((VariableDeclarationFragment) parent).resolveBinding();
        String qname = ((VariableDeclarationFragment) parent).getName().getFullyQualifiedName();
        if (binding != null && binding.getDeclaringClass() != null) {
          qname = binding.getDeclaringClass().getQualifiedName() + "." + qname;
        }
        return Optional.of(qname);
      } else if (parent instanceof FieldDeclaration) {
        List<VariableDeclarationFragment> fragments = ((FieldDeclaration) parent).fragments();
        // use the first one as the binding
        if (!fragments.isEmpty()) {
          String qname = fragments.get(0).getName().getFullyQualifiedName();
          IVariableBinding binding = fragments.get(0).resolveBinding();
          if (binding != null && binding.getDeclaringClass() != null) {
            qname = binding.getDeclaringClass().getQualifiedName() + "." + qname;
          }
          return Optional.of(qname);
        }
      } else if (parent instanceof MethodDeclaration
          && !(parent.getParent()
              instanceof
              AnonymousClassDeclaration)) { // to avoid binding null when inside anonymous class
        return Optional.of(
            getMethodQNameFromBinding(((MethodDeclaration) parent).resolveBinding()));
      } else if (parent instanceof TypeDeclaration) {
        ITypeBinding tdBinding = ((TypeDeclaration) parent).resolveBinding();
        // isFromSource
        return Optional.of(tdBinding.getQualifiedName());
      } else if (parent instanceof Initializer && parent.getParent() instanceof TypeDeclaration) {
        // initializer block
        return Optional.of(getParentInitBlockName(parent) + ".INIT");
      }
      parent = parent.getParent();
    }
    return Optional.empty();
  }

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
