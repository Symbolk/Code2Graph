package edu.pku.code2graph.diff.cochange;

import com.github.gumtreediff.actions.model.*;
import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.ITree;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.operations.DeleteOperation;
import gumtree.spoon.diff.operations.InsertOperation;
import gumtree.spoon.diff.operations.MoveOperation;
import gumtree.spoon.diff.operations.Operation;

import java.util.*;
import java.util.stream.Collectors;

public class MyActionClassifier {
  public List<ITree> srcUpdTrees = new ArrayList();
  public List<ITree> dstUpdTrees = new ArrayList();
  public List<ITree> srcMvTrees = new ArrayList();
  public List<ITree> dstMvTrees = new ArrayList();
  public List<ITree> srcDelTrees = new ArrayList();
  public List<ITree> dstAddTrees = new ArrayList();
  public Map<ITree, Action> originalActionsSrc = new HashMap();
  public Map<ITree, Action> originalActionsDst = new HashMap();

  public MyActionClassifier(Set<Mapping> rawMappings, List<Action> actions) {
    this.clean();
    MappingStore mappings = new MappingStore(rawMappings);
    Iterator var4 = actions.iterator();

    while (var4.hasNext()) {
      Action action = (Action) var4.next();
      ITree original = action.getNode();
      if (action instanceof Delete) {
        this.srcDelTrees.add(original);
        this.originalActionsSrc.put(original, action);
      } else if (action instanceof Insert) {
        this.dstAddTrees.add(original);
        this.originalActionsDst.put(original, action);
      } else {
        ITree dest;
        if (action instanceof Update) {
          dest = mappings.getDst(original);
          original.setMetadata("spoon_object_dest", dest.getMetadata("spoon_object"));
          this.srcUpdTrees.add(original);
          this.dstUpdTrees.add(dest);
          this.originalActionsSrc.put(original, action);
        } else if (action instanceof Move) {
          dest = mappings.getDst(original);
          original.setMetadata("spoon_object_dest", dest.getMetadata("spoon_object"));
          this.srcMvTrees.add(original);
          this.dstMvTrees.add(dest);
          this.originalActionsDst.put(dest, action);
        }
      }
    }
  }

  public List<Action> getRootActions() {
    List<Action> rootActions =
        (List)
            this.srcUpdTrees.stream()
                .map(
                    (t) -> {
                      return (Action) this.originalActionsSrc.get(t);
                    })
                .collect(Collectors.toList());
    rootActions.addAll(
        (Collection)
            this.srcDelTrees.stream()
                .filter(
                    (t) -> {
                      return !this.srcDelTrees.contains(t.getParent())
                          && !this.srcUpdTrees.contains(t.getParent());
                    })
                .map(
                    (t) -> {
                      return (Action) this.originalActionsSrc.get(t);
                    })
                .collect(Collectors.toList()));
    rootActions.addAll(
        (Collection)
            this.dstAddTrees.stream()
                .filter(
                    (t) -> {
                      return !this.dstAddTrees.contains(t.getParent())
                          && !this.dstUpdTrees.contains(t.getParent());
                    })
                .map(
                    (t) -> {
                      return (Action) this.originalActionsDst.get(t);
                    })
                .collect(Collectors.toList()));
    rootActions.addAll(
        (Collection)
            this.dstMvTrees.stream()
                .filter(
                    (t) -> {
                      return !this.dstMvTrees.contains(t.getParent());
                    })
                .map(
                    (t) -> {
                      return (Action) this.originalActionsDst.get(t);
                    })
                .collect(Collectors.toList()));
    rootActions.removeAll(Collections.singleton((Object) null));
    return rootActions;
  }

  private void clean() {
    this.srcUpdTrees.clear();
    this.dstUpdTrees.clear();
    this.srcMvTrees.clear();
    this.dstMvTrees.clear();
    this.srcDelTrees.clear();
    this.dstAddTrees.clear();
    this.originalActionsSrc.clear();
    this.originalActionsDst.clear();
  }

  public static List<Operation> replaceMoveFromAll(Diff editScript) {
    return replaceMove(editScript.getMappingsComp(), editScript.getAllOperations(), true);
  }

  public static List<Operation> replaceMoveFromRoots(Diff editScript) {
    return replaceMove(editScript.getMappingsComp(), editScript.getRootOperations(), false);
  }

  public static List<Operation> replaceMove(
      MappingStore mapping, List<Operation> ops, boolean all) {
    List<Operation> newOps = new ArrayList();
    List<ITree> dels =
        (List)
            ops.stream()
                .filter(
                    (e) -> {
                      return e instanceof DeleteOperation;
                    })
                .map(
                    (e) -> {
                      return e.getAction().getNode();
                    })
                .collect(Collectors.toList());
    List<ITree> inss =
        (List)
            ops.stream()
                .filter(
                    (e) -> {
                      return e instanceof InsertOperation;
                    })
                .map(
                    (e) -> {
                      return e.getAction().getNode();
                    })
                .collect(Collectors.toList());
    Iterator var6 = ops.iterator();

    while (true) {
      ITree parentInAction;
      InsertOperation insertOp;
      label38:
      do {
        while (var6.hasNext()) {
          Operation operation = (Operation) var6.next();
          if (operation instanceof MoveOperation) {
            MoveOperation movOp = (MoveOperation) operation;
            ITree node = ((Move) movOp.getAction()).getNode();
            Delete deleteAction = new Delete(node);
            DeleteOperation delOp = new DeleteOperation(deleteAction);
            if (all || !inParent(dels, node.getParent())) {
              newOps.add(delOp);
            }

            ITree dstNode = mapping.getDst(node);
            parentInAction = ((Move) movOp.getAction()).getParent();
            ITree parent =
                mapping.hasSrc(parentInAction) ? mapping.getDst(parentInAction) : parentInAction;
            int pos = movOp.getPosition();
            Insert insertAc = new Insert(dstNode, parent, pos);
            insertOp = new InsertOperation(insertAc);
            continue label38;
          }

          newOps.add(operation);
        }

        return newOps;
      } while (!all && inParent(inss, parentInAction));

      newOps.add(insertOp);
    }
  }

  public static boolean inParent(List<ITree> trees, ITree parent) {
    if (parent == null) {
      return false;
    } else {
      return trees.contains(parent) ? true : inParent(trees, parent.getParent());
    }
  }
}
