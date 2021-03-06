package natlab.refactoring;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import natlab.refactoring.Exceptions.RefactorException;
import natlab.toolkits.ParsedCompilationUnitsContextStack;
import natlab.toolkits.analysis.test.LivenessAnalysis;
import natlab.toolkits.analysis.test.ReachingDefs;
import natlab.toolkits.analysis.varorfun.VFFlowInsensitiveAnalysis;
import natlab.toolkits.analysis.varorfun.VFFlowset;
import natlab.toolkits.filehandling.genericFile.GenericFile;
import natlab.utils.NodeFinder;
import ast.AssignStmt;
import ast.CompilationUnits;
import ast.Function;
import ast.GlobalStmt;
import ast.Name;
import ast.Stmt;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class ExtractFunction {
    private static final boolean DEBUG = false;
    ParsedCompilationUnitsContextStack context;

    public ExtractFunction(CompilationUnits cu){
        context = new ParsedCompilationUnitsContextStack(Lists.<GenericFile>newLinkedList(),
            cu.getRootFolder(), cu);
    }

    public List<RefactorException> extract(ast.List<Stmt> stmtList, int offset_start, int offset_end, Function f){
        Function orig = NodeFinder.findParent(stmtList, Function.class);
        ast.List<Stmt> newStmtList = new ast.List<Stmt>();
        VFFlowInsensitiveAnalysis kindAnalysis = new VFFlowInsensitiveAnalysis(orig);

        kindAnalysis.analyze();

        for (int i=offset_start; i<offset_end; i++) {
            newStmtList.addChild(stmtList.getChild(i));
        }

        f.setStmtList(newStmtList);

        ReachingDefs reachingAnalysisOrig = new ReachingDefs(orig);
        reachingAnalysisOrig.analyze();
        ReachingDefs reachingAnalysisNew = new ReachingDefs(f);
        reachingAnalysisNew.analyze();

        LivenessAnalysis liveAnalysisOrig = new LivenessAnalysis(orig);
        liveAnalysisOrig.analyze();

        LivenessAnalysis liveAnalysisNew = new LivenessAnalysis(f);
        liveAnalysisNew.analyze();

        Stmt startStmt = stmtList.getChild(offset_start);
        Stmt endStmt = stmtList.getChild(offset_end-1);

        Map<String, Set<AssignStmt>> reachingBefore = reachingAnalysisOrig.getOutFlowSets().get(startStmt).toMap();
        Map<String, Set<AssignStmt>> reachingAfter = reachingAnalysisNew.getOutFlowSets().get(f).toMap();
        Set<String> liveBefore = liveAnalysisNew.getInFlowSets().get(f).getSet();
        Set<String> liveAfter = liveAnalysisOrig.getOutFlowSets().get(endStmt).getSet();
        VFFlowset kinds = kindAnalysis.getFlowSets().get(orig);
        if (DEBUG) {
            System.out.println(liveBefore);
            System.out.println(liveAfter);
            System.out.println(reachingBefore);
            System.out.println(reachingAfter);
        }
        LinkedList<RefactorException> errors = Lists.newLinkedList();
        Set<String> addedGlobals = Sets.newHashSet();
        //inputs 
        Set<String> inputs = Sets.newHashSet();
        for (String id: liveBefore){
            if (kinds.getKind(id).isVariable()){
                if (reachingBefore.get(id).contains(ReachingDefs.GLOBAL)){
                    if (!addedGlobals.contains(id))
                        newStmtList.insertChild(new GlobalStmt(new ast.List<Name>().add(new Name(id))),0);
                } else if ((!reachingBefore.get(id).contains(ReachingDefs.UNDEF)) && 
                    (reachingBefore.size()>0)){
                    f.addInputParam(new Name(id));
                    inputs.add(id);
                } else {
                    errors.add(new Exceptions.FunctionInputCanBeUndefined(new Name(id)));
                }
            }
        }

        for (String id: liveAfter){
            if (kinds.getKind(id).isVariable() && reachingAfter.containsKey(id)) {
                if (reachingAfter.get(id).contains(ReachingDefs.GLOBAL)) {
                    if (!addedGlobals.contains(id))
                        newStmtList.insertChild(new GlobalStmt(new ast.List<Name>().add(new Name(id))),0);
                } else if (reachingAfter.get(id).contains(ReachingDefs.UNDEF)) {
                    f.addOutputParam(new Name(id));
                    if (!inputs.contains(id)) 
                        f.addInputParam(new Name(id));
                    errors.add(new Exceptions.FunctionOutputCanBeUndefined(new Name(id)));
                } else {
                    f.addOutputParam(new Name(id));
                }
            }
        }
        return errors;
    }
}