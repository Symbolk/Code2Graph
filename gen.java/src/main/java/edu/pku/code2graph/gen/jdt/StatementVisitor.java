package edu.pku.code2graph.gen.jdt;

import org.eclipse.jdt.core.dom.*;

import java.util.HashSet;
import java.util.Set;

/** Find field access, enum constant, arg type, method access in statement */
public class StatementVisitor extends AbstractJdtVisitor {
  private Set<String> qNames = new HashSet<>();

  public Set<String> getQNames() {
    return qNames;
  }

  @Override
  public boolean visit(FieldAccess fa) {
    IVariableBinding binding = fa.resolveFieldBinding();
    if (binding != null && binding.isField() && binding.getDeclaringClass() != null) {
      // find wrapped parent entity node and build usage
      qNames.add(binding.getDeclaringClass().getQualifiedName() + "." + binding.getName());
    }
    return true;
  }

  @Override
  public boolean visit(SimpleName sn) {
    if (!sn.isDeclaration()) {
      IBinding binding = sn.resolveBinding();
      if (binding instanceof IVariableBinding) {
        IVariableBinding variableBinding = (IVariableBinding) binding;
        if (variableBinding.isParameter()) {
          qNames.add(variableBinding.getType().getQualifiedName());
        } else if (variableBinding.isField() && variableBinding.getDeclaringClass() != null) {
          qNames.add(
              variableBinding.getDeclaringClass().getQualifiedName()
                  + "."
                  + variableBinding.getName());
        }
      }
    }

    return false;
  }

  @Override
  public boolean visit(QualifiedName qn) {
    IBinding binding = qn.resolveBinding();
    if (binding instanceof IVariableBinding) {
      IVariableBinding variableBinding = (IVariableBinding) binding;
      if (variableBinding.getDeclaringClass() != null) {
        qNames.add(
            variableBinding.getDeclaringClass().getQualifiedName()
                + "."
                + variableBinding.getName());
      }
    }
    return false;
  }

  @Override
  public boolean visit(MethodInvocation mi) {
    IMethodBinding mdBinding = mi.resolveMethodBinding();
    // only internal invocation (or consider types, fields and local?)
    if (mdBinding != null) {
      qNames.add(JdtService.getMethodQNameFromBinding(mdBinding));
    }
    return true;
  }
}
