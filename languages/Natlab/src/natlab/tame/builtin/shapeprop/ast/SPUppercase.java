package natlab.tame.builtin.shapeprop.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import natlab.tame.builtin.shapeprop.ShapePropMatch;
import natlab.tame.valueanalysis.value.*;
import natlab.tame.valueanalysis.components.shape.*;
import natlab.tame.valueanalysis.components.constant.*;

public class SPUppercase<V extends Value<V>> extends SPAbstractVectorExpr<V>{
	static boolean Debug = false;
	String s;
	public SPUppercase (String s){
		this.s = s;
		//System.out.println(s);
	}
	
	public ShapePropMatch<V> match(boolean isPatternSide, ShapePropMatch<V> previousMatchResult, Args<V> argValues, int num){
		if(isPatternSide==true){
			if(previousMatchResult.isInsideAssign()==true){
				previousMatchResult.saveLatestMatchedUppercase(s);
				return previousMatchResult;
			}
			if(argValues.get(previousMatchResult.getNumMatched())!=null){
				//get indexing current Matrix Value from args
				//get shape info from current Matrix Value
				Shape<V> argumentShape = ((HasShape<V>)argValues.get(previousMatchResult.getNumMatched())).getShape();
				Constant argumentConstant =((HasConstant)argValues.get(previousMatchResult.getNumMatched())).getConstant();
				if(argumentConstant!=null){
					if (Debug) System.out.println("it's a constant!");
					previousMatchResult.setIsError();
					return previousMatchResult;
				}
				//check whether or not current uppercase already in the previousMatchResult
				try{
					if(previousMatchResult.getLatestMatchedUppercase().equals(s)){
						//cases like (M,M->M), those M should be definitely the same!!! if not, return error information, interesting!
						List<Integer> l = new ArrayList<Integer>();
						l = previousMatchResult.getShapeOfVariable(previousMatchResult.getLatestMatchedUppercase()).getDimensions();
						Shape<V> oldShape = (new ShapeFactory<V>()).newShapeFromIntegers(l);
						//Shape<AggrValue<BasicMatrixValue>> newShape = argumentShape.merge(oldShape); this is wrong at all! see last comment!
						if(argumentShape.getDimensions().equals(oldShape.getDimensions())==false){
							//FIXME really weird, cannot call equals method in Shape class, the problem is still generic problem,
							//cannot cast from Shape<V> to Shape<V>
							if (Debug) System.out.println("MATLAB syntax error!");
							//Shape<AggrValue<BasicMatrixValue>> errorShape = (new ShapeFactory<AggrValue<BasicMatrixValue>>(previousMatchResult.factory)).newShapeFromIntegers(null);
							Shape<V> errorShape = (new ShapeFactory<V>()).newShapeFromIntegers(null);
							errorShape.FlagItsError();
							HashMap<String, Integer> lowercase = new HashMap<String, Integer>();
							lowercase.put(s, null);
							HashMap<String, Shape<V>> uppercase = new HashMap<String, Shape<V>>();
							uppercase.put(s, errorShape);
							ShapePropMatch<V> match = new ShapePropMatch<V>(previousMatchResult, lowercase, uppercase);
							match.comsumeArg();
							match.saveLatestMatchedUppercase(s);
							match.setIsError();//this is important!! break from matching algorithm.
							//System.out.println(match.getValueOfVariable(s));
							if (Debug) System.out.println("the shape of "+s+" is "+match.getShapeOfVariable(s));
							if (Debug) System.out.println("matched matrix expression "+match.getLatestMatchedUppercase());
							return match;
						}
						//if new shape and old shape are equals, just return this shape!
						HashMap<String, Integer> lowercase = new HashMap<String, Integer>();
						lowercase.put(s, null);
						HashMap<String, Shape<V>> uppercase = new HashMap<String, Shape<V>>();
						uppercase.put(s, argumentShape);
						ShapePropMatch<V> match = new ShapePropMatch<V>(previousMatchResult, lowercase, uppercase);
						match.comsumeArg();
						match.saveLatestMatchedUppercase(s);
						//System.out.println(match.getValueOfVariable(s));
						if (Debug) System.out.println("the shape of "+s+" is "+match.getShapeOfVariable(s));
						if (Debug) System.out.println("mathcing "+match.getLatestMatchedUppercase());
						return match;
					}
					
				}catch (Exception e){}
				HashMap<String, Integer> lowercase = new HashMap<String, Integer>();
				lowercase.put(s, null);
				HashMap<String, Shape<V>> uppercase = new HashMap<String, Shape<V>>();
				uppercase.put(s, argumentShape);
				ShapePropMatch<V> match = new ShapePropMatch<V>(previousMatchResult, lowercase, uppercase);
				match.comsumeArg();
				match.saveLatestMatchedUppercase(s);
				//System.out.println(match.getValueOfVariable(s));
				if (Debug) System.out.println("the shape of "+s+" is "+match.getShapeOfVariable(s));
				if (Debug) System.out.println("mathcing "+match.getLatestMatchedUppercase());
				return match;
			}
			//FIXME if index pointing empty, means not match, do something
			return previousMatchResult;
		}
		else{
			if (Debug) System.out.println("inside output uppercase "+s);
			//default, which means in the pattern match side, there is no Uppercase matched.
			if(previousMatchResult.getShapeOfVariable(s)==null){
				if(previousMatchResult.getOutputVertcatExpr().size()==0){
					if(previousMatchResult.getLatestMatchedUppercase().equals("$")){
						previousMatchResult.addToOutput(previousMatchResult.getShapeOfVariable("$"));
						return previousMatchResult;
					}
					previousMatchResult.addToOutput(null);
					return previousMatchResult;
				}
				else if(previousMatchResult.getOutputVertcatExpr().size()==1){
					previousMatchResult.addToVertcatExpr(previousMatchResult.getOutputVertcatExpr().get(0));
					previousMatchResult.copyVertcatToOutput();
					return previousMatchResult;
				}
				else {
					previousMatchResult.copyVertcatToOutput();
					return previousMatchResult;
				}
			}
			else{
				previousMatchResult.addToOutput(previousMatchResult.getShapeOfVariable(s));
				return previousMatchResult;
			}	
		}
	}
	
	public String toString(){
		return s.toString();
	}
}
