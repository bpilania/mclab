package mclint.analyses;

import java.util.Set;

import mclint.AnalysisKit;
import mclint.Lint;
import mclint.LintAnalysis;
import mclint.Message;
import mclint.util.DefinitionVisitor;
import natlab.toolkits.analysis.HashSetFlowSet;
import natlab.toolkits.analysis.test.LivenessAnalysis;
import natlab.utils.NodeFinder;
import ast.ForStmt;
import ast.Function;
import ast.Name;
import ast.Stmt;

import com.google.common.collect.Sets;

public class UnusedVar extends DefinitionVisitor implements LintAnalysis {
  private LivenessAnalysis liveVar;

  /* We shouldn't warn that output parameters aren't used. */
  private Set<String> outputParams = Sets.newHashSet();

  protected Lint lint;

  private void reportUnused(Name node) {
    lint.report(Message.regarding(node, "UNUSED_VAR",
        String.format("Unused variable %s.", node.getID())));
  }

  public UnusedVar(AnalysisKit kit) {
    super(kit.getAST());
    this.liveVar = kit.getLiveVariablesAnalysis();
  }

  @Override
  public void caseFunction(Function node) {
    Set<String> outputParamsCopy = Sets.newHashSet(outputParams);
    outputParams.addAll(node.getOutParamSet());
    super.caseFunction(node);
    outputParams.retainAll(outputParamsCopy);
  }

  private boolean isLive(Name node) {
    Stmt parentStmt = NodeFinder.of(Stmt.class).findParent(node);
    HashSetFlowSet<String> setToCheck;
    if (parentStmt.getParent() instanceof ForStmt) {
      Stmt first = ((ForStmt) (parentStmt.getParent())).getStmt(0);
      setToCheck = liveVar.getInFlowSets().get(first);
    } else {
      setToCheck = liveVar.getOutFlowSets().get(parentStmt);
    }
    return setToCheck.contains(node.getID());
  }

  @Override
  public void caseLHSName(Name node) {
    if (!outputParams.contains(node.getID()) && !isLive(node)) {
      reportUnused(node);
    }
  }

  @Override
  public void caseInParam(Name node) {
    Stmt firstStatement = NodeFinder.of(Function.class).findParent(node).getStmt(0);
    if (!(liveVar.getInFlowSets().containsKey(firstStatement) &&
          liveVar.getInFlowSets().get(firstStatement).contains(node.getID()))) {
      reportUnused(node);
    }
  }

  @Override
  public void analyze(Lint lint) {
    this.lint = lint;
    tree.analyze(this);
  }
}
