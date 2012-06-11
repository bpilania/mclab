package natlab.tame.callgraph;

import java.util.ArrayList;
import java.util.HashSet;

import natlab.CompilationProblem;
import natlab.toolkits.path.FileEnvironment;
import natlab.toolkits.path.FunctionReference;

/**
 * this is similar to the simple function collection, but rather than
 * attempting to load every file involved in the program, the files are loaded
 * only as they are requested. Every time a file is actually loaded, all 
 * files that it refers to are placed in a load cache.
 */
public class IncrementalFunctionCollection extends SimpleFunctionCollection{
	private static final long serialVersionUID = 1L;
	public ArrayList<CompilationProblem> errors = new ArrayList<CompilationProblem>();
	private HashSet<FunctionReference> loadCache;
	
	public IncrementalFunctionCollection(FileEnvironment env) {
		super(env);
	}
	
	@Override
	public boolean collect(FunctionReference ref,
			ArrayList<CompilationProblem> errList) {
        if (super.containsKey(ref)) return true;
        if (ref.isBuiltin) return true;
        if (loadCache == null) loadCache = new HashSet<FunctionReference>();
        loadCache.add(ref);
        return true;
	}
	
	@Override
	public boolean containsKey(Object key) {
		return super.containsKey(key) || loadCache.contains(key);
	}
	
	@Override
	public StaticFunction get(Object key) {
		//actually load if necessary
		if (loadCache.contains(key)){
			super.collect((FunctionReference)key, errors);
			loadCache.remove(key);
		}		
		//then return
		return super.get(key);
	}

}