package natlab.tame;

import java.util.ArrayList;
import java.util.List;

import natlab.tame.builtin.Builtin;
import natlab.tame.callgraph.FunctionCollection;
import natlab.tame.callgraph.StaticFunction;
import natlab.tame.classes.reference.PrimitiveClassReference;
import natlab.tame.valueanalysis.IntraproceduralValueAnalysis;
import natlab.tame.valueanalysis.aggrvalue.AggrValue;
import natlab.tame.valueanalysis.basicmatrix.*;
import natlab.tame.valueanalysis.value.Args;
import natlab.toolkits.filehandling.genericFile.GenericFile;
import natlab.toolkits.path.FilePathEnvironment;

public class TamerToolPlusShape {

	public IntraproceduralValueAnalysis<AggrValue<BasicMatrixValue>> 
	        tameMatlabToSingleFunction(java.io.File mainFile, List<AggrValue<BasicMatrixValue>> inputValues){
		System.out.println("inside TTPS first function!");
		GenericFile gFile = GenericFile.create(mainFile); //file -> generic file
		FilePathEnvironment path = new FilePathEnvironment(gFile, Builtin.getBuiltinQuery()); //get path environment obj
		FunctionCollection callgraph = new FunctionCollection(path); //build simple callgraph
		StaticFunction function = callgraph.getAsInlinedStaticFunction(); //inline all

		//build intra-analysis
		@SuppressWarnings("unchecked")
		IntraproceduralValueAnalysis<AggrValue<BasicMatrixValue>> analysis = 
		        new IntraproceduralValueAnalysis<AggrValue<BasicMatrixValue>>(
		        		null, function, new BasicMatrixValueFactory(), 
		        		Args.<AggrValue<BasicMatrixValue>>newInstance(inputValues));
		System.out.println("before analyze!");
		analysis.analyze(); //run analysis
		return analysis;
	}

	/**
	 * This is the same as tameMatlabToSingleFunction, but takes 
	 * a list of PrimitiveClassReferences as arguments, rather than
	 * abstract values. PrimitiveClassReference is an enum of 
	 * the builtin matlab classes representing matrizes.
	 */
	//XU expands it to support initial shape input
	public IntraproceduralValueAnalysis<AggrValue<BasicMatrixValue>> 
			tameMatlabToSingleFunctionFromClassReferences(java.io.File mainFile, List<PrimitiveClassReference> inputValues){
		System.out.println("inside TTPS second function!");
		//System.out.println(inputValues);
		BasicMatrixValueFactory factory = new BasicMatrixValueFactory();
		ArrayList<AggrValue<BasicMatrixValue>> list = new ArrayList<AggrValue<BasicMatrixValue>>(inputValues.size());
		for (PrimitiveClassReference ref : inputValues){
			list.add(new BasicMatrixValue(ref));         //here, we create list of basicMatrixValue with the input default class (double)
		}
		return tameMatlabToSingleFunction(mainFile, list);
	}

	//TODO give more useful functions!
}