package natlab.tame.valueanalysis;

import java.util.*;
import java.util.List;

import natlab.tame.builtin.Builtin;
import natlab.tame.callgraph.StaticFunction;
import natlab.tame.interproceduralAnalysis.*;
import natlab.tame.tir.*;
import natlab.tame.tir.analysis.*;
import natlab.tame.valueanalysis.aggrvalue.*;
import natlab.tame.valueanalysis.components.constant.*;
import natlab.tame.valueanalysis.value.*;
import natlab.toolkits.path.FunctionReference;
import ast.*;

/**
 * This analysis attempts to find the class of every variable.
 * It also propagates some constant information. This analysis
 * stores a flow set for every statement, which is a map of
 * variablename->classname->value
 * 
 * This class operates on the static IR.
 * @author ant6n
 */
public class IntraproceduralValueAnalysis<V extends Value<V>>
extends TIRAbstractSimpleStructuralForwardAnalysis<ValueFlowMap<V>>
implements FunctionAnalysis<Args<V>, Res<V>>{
    StaticFunction function;
    ValueFactory<V> factory;
    ValuePropagator<V> valuePropagator;
    ValueFlowMap<V> argMap;
    Args<V> args;
    static boolean Debug = false;
    InterproceduralAnalysisNode<IntraproceduralValueAnalysis<V>, Args<V>, Res<V>> node;
    
    public IntraproceduralValueAnalysis(InterproceduralAnalysisNode<IntraproceduralValueAnalysis<V>, Args<V>, Res<V>> node,
            StaticFunction function, ValueFactory<V> factory) {
        super(function.getAst());
        this.node = node;
        this.function = function;
        this.factory = factory;
        this.valuePropagator = factory.getValuePropagator();
    }
    
    /**
     * constructor that allows specifying values of the arguments
     */
    public IntraproceduralValueAnalysis(InterproceduralAnalysisNode<IntraproceduralValueAnalysis<V>, Args<V>, Res<V>> node,
            StaticFunction function, ValueFactory<V> factory, Args<V> args) {
        this(node,function,factory);
        argMap = new ValueFlowMap<V>();
        this.args = args;
        //TODO check whether given args <= declared args
        for (int i = 0; i < args.size(); i++){
            argMap.put(
                    function.getAst().getInputParam(i).getID(),
                    ValueSet.newInstance(args.get(i)));
        }
        if (Debug) System.out.println("intraprocedural value analysis on "+function.getName()+" with args "+argMap);
    }
    
    
    /**
     * returns the result of the analysis. Runs the analysis if isAnalyzed is false
     */
    public Res<V> getResult(){
        if (! isAnalyzed()) this.analyze();
        Res<V> result = Res.newInstance();
        ValueFlowMap<V> flowResult = getOutFlowSets().get(function.getAst());
        //TODO - make sure there's the right number of outputs
        for (Name out : function.getAst().getOutputParamList()){
            result.add(flowResult.get(out.getID()));
        }
        return result;
    }
    
    public Args<V> getArgs(){
    	return args;
    }
    public ValueFlowMap<V> getArgMap(){
    	return argMap;
    }
    

    /*********** inherited stuff **************************************/
    @Override
    public void copy(ValueFlowMap<V> source, ValueFlowMap<V> dest) {
        dest.copyOtherIntoThis(source);
    }

    @Override
    public void merge(ValueFlowMap<V> in1, ValueFlowMap<V> in2, ValueFlowMap<V> out) {
        out.copyOtherIntoThis(in1.merge(in2));
    }

    @Override
    public ValueFlowMap<V> newInitialFlow() {
        return new ValueFlowMap<V>();
    }

    /*********** Function case - setting up args *************************/
    @Override
    public void caseFunction(Function node) {
        currentInSet = argMap.copy();
        caseASTNode(node);
        associateInAndOut(node);
    }

    /*********** statement cases *****************************************/
    @Override
    public void caseTIRCallStmt(TIRCallStmt node) {
        if (checkNonViable(node)) return;
        if (Debug) System.out.println("ircall: "+node.getPrettyPrinted());
        //find function
        FunctionReference ref = function.getCalledFunctions().get(node.getFunctionName().getID());
        //do call
        setCurrentOutSet(assign(getCurrentInSet(),node.getTargets(),
                call(ref, getCurrentInSet(), node.getArguments(), node.getTargets(), node, null)));
        //associate flowsets
        associateInAndOut(node);
    }
    
    public void caseIRCommentStmt(TIRCommentStmt node) {}
    
    @SuppressWarnings("unchecked")
    @Override
    public void caseLoopVar(AssignStmt assign) {
        if (checkNonViable(assign)) return;
        TIRForStmt node = (TIRForStmt)assign.getParent();
        ValueFlowMap<V> flow = getCurrentInSet();
        LinkedList<Res<V>> results = new LinkedList<Res<V>>();
        //set the loop var
        //TODO - do we have to check whether colon is overloaded? - some initial checks say no, it cannot be overloaded
        //TODO - should we just call builtin colon propagator?
        if (node.hasIncr()){ //there's an inc value
            for (LinkedList<V> list : cross(flow,
                    node.getLowerName(),node.getIncName(),node.getUpperName())){
                results.add(Res.newInstance(
                		factory.forRange(list.getFirst(), list.getLast(), list.get(1))));
            }
        } else { //there's no inc value
            for (LinkedList<V> list : cross(flow,
                    node.getLowerName(),node.getUpperName())){
                results.add(Res.newInstance(
                		factory.forRange(list.getFirst(),list.getLast(),null)));
            }
        }
        //put results
        setCurrentOutSet(assign(flow,node.getLoopVarName().getID(), Res.newInstance(results)));
        associateInAndOut(node);
    }
    
    
    @Override
    public void caseIfCondition(Expr condExpr) {
        if (checkNonViable(condExpr)) return;
        ValueFlowMap<V> current = getCurrentInSet();
        ValueSet<V> values = current.get(((NameExpr)condExpr).getName().getID());
        for (V value : values){
        	//call 'any' on the condition value
            Constant any = Builtin.Any.getInstance().visit(
            		ConstantPropagator.<V>getInstance(),
            		Args.newInstance(value));
            if (any != null && any instanceof LogicalConstant){
                if (((LogicalConstant)any).equals(Constant.get(true))){
                    //result is true - false set is not viable
                    setTrueFalseOutSet(current, ValueFlowMap.<V>newInstance(false));
                } else {
                    //result is false - true set is not viable
                    setTrueFalseOutSet(ValueFlowMap.<V>newInstance(false), current);
                }
                return;
            }
        }
        setTrueFalseOutSet(current, current);
    }
    
    @SuppressWarnings("unchecked")
	@Override
    public void caseTIRArrayGetStmt(TIRArrayGetStmt node) {
        if (checkNonViable(node)) return;
        if (Debug) System.out.println("case array get: "+node.getPrettyPrinted());
        ValueFlowMap<V> flow = getCurrentInSet(); //note copied!
        //if (Debug) System.out.println(flow);
        ValueSet<V> array = flow.get(node.getArrayName().getID());
        if (array == null) throw new UnsupportedOperationException("attempting to access unknown array "+node.getArrayName().getID()+" in\n"+node.getPrettyPrinted()+"\n with flow "+flow);
        LinkedList<Res<V>> results = new LinkedList<Res<V>>();
        //go through all possible array values
        for (V arrayValue : array){
            if (arrayValue instanceof MatrixValue){
                if (node.getIndizes().size() == 0){
                    results.add(Res.newInstance(ValueSet.newInstance(arrayValue)));
                } else {
                    //go through all possible index setss
                    //TODO - deal with overloading etc.
                    //TODO - errors on assign - use is assign to var??
                    for (List<V> indizes : cross(flow,node.getIndizes())){
                        results.add(Res.newInstance(arrayValue.arraySubsref(Args.newInstance(indizes))));
                    }
                }
            } else if (arrayValue instanceof FunctionHandleValue){
                //go through all function handles this may represent and get the result
                //TODO - make this independent of the specific function handle value implementation
                for (FunctionHandleValue.FunctionHandle handle :((FunctionHandleValue<?>)((Object)arrayValue)).getFunctionHandles()){
                    results.add(call(
                          handle.getFunction(), flow, node.getIndizes(), node.getTargets(), node, (List<ValueSet<V>>)(List<?>)handle.getPartialValues()));
                }
            } else {
                //TODO more possible values here
                throw new UnsupportedOperationException("array get received unknown value "+arrayValue);
            }
        }
        
        //put result assign/set flowsets
        setCurrentOutSet(assign(flow, node.getTargets(), Res.newInstance(results)));
        associateInAndOut(node);
    }
    
    //TODO make it deal with overloading properly
    @Override
    public void caseTIRAbstractCreateFunctionHandleStmt(TIRAbstractCreateFunctionHandleStmt node) {
        if (checkNonViable(node)) return;
        if (Debug) System.out.println("case assign f_handle: "+node.getPrettyPrinted());
        //find var and remove
        String targetName = node.getTargetName().getID();
        ValueFlowMap<V> flow = getCurrentInSet();
        
        //find the function handle
        FunctionReference f = 
            function.getCalledFunctions().get(node.getFunctionName().getID());
        
        //get enclosed workspace - the set of values already assigned
        List<Name> enclosedNames = new ArrayList<Name>();
        if (node instanceof TIRCreateLambdaStmt){
        	enclosedNames = ((TIRCreateLambdaStmt)node).getEnclosedVars();
        }
        LinkedList<ValueSet<V>> enclosedValues = new LinkedList<ValueSet<V>>();
        for (Name var : enclosedNames){
            enclosedValues.add(flow.get(var.getID()));
        }
        
        //assign result
        setCurrentOutSet(assign(flow, targetName, 
                Res.newInstance(factory.newFunctionHandlevalue(f, enclosedValues))));
        
        //set in/out set
        associateInAndOut(node);
    }
    
    
    @Override
    public void caseTIRArraySetStmt(TIRArraySetStmt node) {
        if (checkNonViable(node)) return;
        if (Debug) System.out.println("case array set: "+node.getPrettyPrinted());
        //find vars
        ValueFlowMap<V> flow = getCurrentInSet();
        String targetName = node.getArrayName().getID();
        ValueSet<V> targets = flow.get(targetName);
        ValueSet<V> values = flow.get(node.getValueName().getID());
        List<LinkedList<V>> indizesList = cross(flow,node.getIndizes());
        
        //if target is undefined yet, define it
        if (targets == null){
            //for now just use the value... FIXME
            targets = values;
        }
        if (values == null){
            throw new UnsupportedOperationException("bad array set "+node.getPrettyPrinted()+"\n"+targetName+","+targets+","+values+"\n"+flow+"\n"+function);
        }
        
        //assign all combinations
        LinkedList<V> results = new LinkedList<V>();
        for (V value : values){
            for (LinkedList<V> indizes : indizesList){
                Args<V> is = Args.newInstance(indizes);
                for (V target : targets){
                    results.add(target.arraySubsasgn(is, value));
                }
            }
        }
        
        //assign result
        setCurrentOutSet(assign(flow, targetName, Res.newInstance(ValueSet.newInstance(results))));
        
        //set in/out set
        associateInAndOut(node);
    }
    
    @Override
    public void caseTIRAssignLiteralStmt(TIRAssignLiteralStmt node) {
        if (checkNonViable(node)) return;
        if (Debug) System.out.println("case assign literal: "+node.getPrettyPrinted());
        //get literal and make constant
        Constant constant = Constant.get(node.getRHS());

        //put in flow map
        ValueFlowMap<V> flow = getCurrentInSet();
        String targetName = node.getTargetName().getID();
        
        //assign result
        setCurrentOutSet(assign(flow,targetName, 
                Res.newInstance(factory.newValueSet(constant))));

        //set in/out set
        associateInAndOut(node);
    }
    
    @Override
    public void caseTIRCopyStmt(TIRCopyStmt node) {
        if (checkNonViable(node)) return;
        if (Debug) System.out.println("case copy: "+node.getPrettyPrinted());

        ValueFlowMap<V> flow = getCurrentInSet();
        String targetName = node.getTargetName().getID();
        ValueSet<V> valueSet = flow.get(node.getSourceName().getID());
        
        //assign result
        setCurrentOutSet(assign(flow, targetName, Res.newInstance(valueSet)));
        
        //set in/out set
        associateInAndOut(node);
        if (Debug) System.out.println("after copy: "+flow);
    }
    
    
    @Override
    public void caseTIRDotGetStmt(TIRDotGetStmt node) {
        if (checkNonViable(node)) return;
        if (Debug) System.out.println("case dot get: "+node.getPrettyPrinted());

        //get variable, field, flow
        ValueFlowMap<V> flow = getCurrentInSet();
        String objName = node.getDotName().getID();
        String field = node.getFieldName().getID();
        
        LinkedList<Res<V>> results = new LinkedList<Res<V>>();
        
        //TODO check if var exists
        //loop through all possible values
        for (Value<V> v : flow.get(objName)){
            results.add(Res.newInstance(v.dotSubsref(field)));
        }
        //assign result
        setCurrentOutSet(assign(flow, node.getTargets(),Res.newInstance(results)));

        //set in/out set
        associateInAndOut(node);
    }
    
    @Override
    public void caseTIRDotSetStmt(TIRDotSetStmt node) {
        if (checkNonViable(node)) return;
        if (Debug) System.out.println("case dot set: "+node.getPrettyPrinted());

        //get variable, field, flow
        ValueFlowMap<V> flow = getCurrentInSet();
        String objName = node.getDotName().getID();
        String field = node.getFieldName().getID();
        String rhs = node.getValueName().getID();
        
        LinkedList<Res<V>> results = new LinkedList<Res<V>>();
        //loop through all possible rhs
        for (V v : flow.get(rhs)){
            //check if var exists - TODO this might change
            if (isVarAssigned(flow, objName)){
                //go through all possible objects
                for (V obj : flow.get(objName)){
                    results.add(Res.newInstance(obj.dotSubsasgn(field, v)));                    
                }
            } else {
                results.add(Res.newInstance(factory.newStruct().dotSubsasgn(field, v)));
            }
        }

        //assign result
        setCurrentOutSet(assign(flow, objName, Res.newInstance(results)));

        //set in/out set
        associateInAndOut(node);
    }
    
    
    @Override
    public void caseTIRCellArrayGetStmt(TIRCellArrayGetStmt node) {
        if (checkNonViable(node)) return;
        if (Debug) System.out.println("case cell array get: "+node.getPrettyPrinted());
        ValueFlowMap<V> flow = getCurrentInSet();
        //if (Debug) System.out.println(flow);
        ValueSet<V> array = flow.get(node.getCellArrayName().getID());
        if (array == null) throw new UnsupportedOperationException("attempting to access unknown cell array "+node.getCellArrayName().getID()+" in\n"+node.getPrettyPrinted()+"\n with flow "+flow);
        LinkedList<Res<V>> results = new LinkedList<Res<V>>();
        //go through all possible array values
        for (V arrayValue : array){
            //go through all possible index setss
            //TODO - deal with overloading etc.
            //TODO - errors on assign - use is assign to var??
            for (List<V> indizes : cross(flow,node.getArguments())){
                results.add(arrayValue.cellSubsref(Args.newInstance(indizes)));
            }
        }
        
        //put result assign/set flowsets
        setCurrentOutSet(assign(flow, node.getTargets(), Res.newInstance(results)));
        associateInAndOut(node);
    }
    
    @Override
    public void caseTIRCellArraySetStmt(TIRCellArraySetStmt node) {     
        if (checkNonViable(node)) return;
        if (Debug) System.out.println("case cell array set: "+node.getPrettyPrinted());
        //find vars
        ValueFlowMap<V> flow = getCurrentInSet();
        String targetName = node.getCellArrayName().getID();
        ValueSet<V> targets = flow.get(targetName);
        ValueSet<V> values = flow.get(node.getValueName().getID());
        List<LinkedList<V>> indizesList = cross(flow,node.getIndizes());
        
        //if target is undefined yet, define it
        if (targets == null){
            //TODO - call builtin cell instead
            targets = ValueSet.newInstance(factory.newCell());
        }
        if (values == null){
            //TODO return error value
            throw new UnsupportedOperationException("bad array set "+node.getPrettyPrinted()+"\n"+targetName+","+targets+","+values+"\n"+flow);
        }
        
        //assign all combinations
        LinkedList<Res<V>> results = new LinkedList<Res<V>>();
        for (V value : values){
            for (LinkedList<V> indizes : indizesList){
                Args<V> is = Args.newInstance(indizes);
                for (V target : targets){
                    results.add(Res.newInstance(target.cellSubsasgn(is,Args.newInstance(value))));
                }
            }
        }
        
        //assign result
        setCurrentOutSet(assign(flow, node.getCellArrayName().getID(), Res.newInstance(results)));
        associateInAndOut(node);
    }
    
    @Override
    public void caseTIRCommentStmt(TIRCommentStmt node) {
    	//TODO - do we need something here?
    }
    
    @Override
    public void caseStmt(Stmt node) {
        if (checkNonViable(node)) return;
        //if (Debug)
        System.out.println("IntraproceduralValueAnalysis: Stmt "+node+"-"+node.getPrettyPrinted());
        //set in/out set
        associateInAndOut(node);
    }
    
    /**
     * associates the current in and out set with the given node
     */
    private void associateInAndOut(ASTNode<?> node){
        associateInSet(node, getCurrentInSet());
        associateOutSet(node, getCurrentOutSet());
    }


    /**
     * given an IRCommaSeparated list and a flow set, returns all possible combinations
     * of values as a list of lists
     */
    private List<LinkedList<V>> cross(ValueFlowMap<V> flow,TIRCommaSeparatedList args){
        return cross(flow,args,null);
    }
    
    /**
     * given an IRCommaSeparated list and a flow set, returns all possible combinations
     * of values as a list of lists
     * partial values will be prepended
     */
    private List<LinkedList<V>> cross(ValueFlowMap<V> flow,TIRCommaSeparatedList args,List<ValueSet<V>> partialValues){
        //if (Debug)System.out.println("cross - flow: "+flow+" args: "+args);
        //get list of value sets from the names        
        ArrayList<ValueSet<V>> values;
        if (partialValues == null){
            values = new ArrayList<ValueSet<V>>(args.size());
        } else {
            values = new ArrayList<ValueSet<V>>(args.size()+partialValues.size());
            values.addAll(partialValues);
        }
        for (Expr expr : args){
            if (expr instanceof NameExpr){
                String name = ((NameExpr)expr).getName().getID();
                ValueSet<V> vs = flow.get(name);
                if (vs == null){
                    System.out.println(function.toString());
                    throw new UnsupportedOperationException("name "+name+" not found in "+flow);
                }
                values.add(vs);
            } else if (expr instanceof ColonExpr){
                values.add(ValueSet.newInstance(factory.newColonValue()));
            } else {
                throw new UnsupportedOperationException("received bad arg set "+args);
            }
        }
        return ValueSet.cross(values);
    }
    
    /**
     * returns ValueSet.cross but using valueSets from the given flowmap, using Names
     */
    private List<LinkedList<V>> cross(ValueFlowMap<V> flow, Name... args){
        LinkedList<ValueSet<V>> list = new LinkedList<ValueSet<V>>();
        for (Name arg : args){
            list.add(flow.get(arg.getID()));
        }
        return ValueSet.cross(list);
    }
    
    
    /**
     * implements the flow equations for calling functions
     * Returns a Result, doesn't modify anything
     */
    private Res<V> call(
            FunctionReference function,ValueFlowMap<V> flow,
            TIRCommaSeparatedList args,TIRCommaSeparatedList targets,
            ASTNode<?> callsite, List<ValueSet<V>> partialArgs){
        LinkedList<Res<V>> results = new LinkedList<Res<V>>();
        if (Debug) System.out.println("calling function "+function+" with\n"+cross(flow,args,partialArgs));
        for (LinkedList<V> argumentList : cross(flow,args,partialArgs)){
            //TODO - do overloading, deal with dominant args etc.
            //actually call
            //System.out.println("doFunctionCall result "+res);
            if (function.isBuiltin()){
                Args<V> argsObj = Args.newInstance(argumentList);
                //spcial cases for some known functions
                if (function.getname().equals("nargin") && argsObj.size() == 0){
                    results.add(Res.newInstance(factory.newMatrixValue(argMap.size())));
                } else {
                    results.add(valuePropagator.call(function.getname(), argsObj));
                }
            }else{
                //simplify args
                ArrayList<V> newArgumentList = new ArrayList<V>(argumentList);
                for (int i = 0; i < newArgumentList.size(); i++){
                    newArgumentList.set(i, newArgumentList.get(i).toFunctionArgument(false));
                }
                Args<V> argsObj = Args.newInstance(newArgumentList);
                //simplify again if its recursive
                //TODO - rethink this - if a call is recursive, force it to be even if the arg ist different?
                if (node.getCallString().contains(function, argsObj)){
                    newArgumentList = new ArrayList<V>(argumentList);
                    for (int i = 0; i < newArgumentList.size(); i++){
                        newArgumentList.set(i, newArgumentList.get(i).toFunctionArgument(true));
                    }
                    argsObj = Args.newInstance(newArgumentList);
                }
                //perform analysis for call
                results.add(node.analyze(function, argsObj, callsite));
            }
        }        
        if (Debug) System.out.println("called "+function+", received "+Res.newInstance(results));
        if (cross(flow,args,partialArgs).size() > 1){
        	System.out.println("exiting");
        	System.out.println(results);
        	//System.exit(0);
        }
        //FIXME
        return Res.newInstance(results);
    }
    

    /**
     * assigns the given collection of values, to the targets represented
     * by the comma separated list. Returns a new flowmap which is a copy
     * of old one, except for the newly assigned valeus.
     * 
     * all assignments should go through assign calls
     * TODO - should the third argument be a ValueSet?
     */
    private ValueFlowMap<V> assign(ValueFlowMap<V> flow, 
            String target, Res<V> values){
        return assign(flow,new TIRCommaSeparatedList(new NameExpr(new Name(target))),values);
    }
    
    /**
     * assigns the given collection of values, to the targets represented
     * by the comma separated list. Returns a new flowmap which is a copy
     * of old one, except for the newly assigned valeus.
     * 
     * all assignments should go through assign calls
     * 
     * TODO - should this be part of ValueFlowMap? Or maybe a helper
     * TODO - should we just assign Res<V>?
     */
    private ValueFlowMap<V> assign(ValueFlowMap<V> flow, 
            TIRCommaSeparatedList targets, Res<V> values){
       if (Debug) System.out.println("assign: "+targets+" = "+values);
       ValueFlowMap<V> result = flow.copy();
       if (!values.isViable()){
           return ValueFlowMap.newInstance(false);
       }
       if (targets.isAllNameExpressions()){
           Iterator<ValueSet<V>> iValues = values.iterator();
           Iterator<Name> iNames = targets.asNameList().iterator();
           while (iNames.hasNext()){
               result.put(iNames.next().getID(), iValues.next());
           }
       } else {
           throw new UnsupportedOperationException("no support for non-primitive assings");
       }
       return result;
    }
    
    
    @Override
    public Res<V> getDefaultResult() {
        return Res.newInstance(false);
    }
    
    
    @Override
    public TIRFunction getTree() {
        return (TIRFunction)this.function.getAst();
    }
    
    
    /**
     * returns true if the given variable name is assigned in the given flow set
     */
    private boolean isVarAssigned(ValueFlowMap<V> flow,String varName){
        return (flow.containsKey(varName) && (flow.get(varName).size > 0));
    }    
    
    
    /**
     * checks whether the current flow set is not viable. If it isn't, associate
     * nonviable in/out sets, and return true. Thus, every statement case should
     * start with
     *  if (checkNonViable(node)) return;
     */
    private boolean checkNonViable(ASTNode<?> node){
        if (Debug){
            System.out.println("==== analyzing "+node+": "+node.getPrettyPrinted());
            System.out.println("flowset "+getCurrentInSet());
        }
        if (getCurrentInSet().isViable()) return false;
        setCurrentOutSet(getCurrentInSet().copy());
        associateInAndOut(node);
        return true;
    }
}




