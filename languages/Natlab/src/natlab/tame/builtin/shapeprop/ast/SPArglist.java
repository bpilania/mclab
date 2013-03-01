package natlab.tame.builtin.shapeprop.ast;

import natlab.tame.builtin.shapeprop.ShapePropMatch;
import natlab.tame.valueanalysis.value.*;

public class SPArglist<V extends Value<V>> extends SPAbstractVertcatExprArg<V>{
	SPAbstractVertcatExprArg<V> first;
	SPAbstractVertcatExprArg<V> next;
	
	public SPArglist(SPAbstractVertcatExprArg<V> first, SPAbstractVertcatExprArg<V> next){
		this.first = first;
		this.next = next;
		//System.out.println(",");
	}
	
	public ShapePropMatch<V> match(boolean isPatternSide, ShapePropMatch<V> previousMatchResult, Args<V> argValues, int num){
		previousMatchResult = first.match(isPatternSide, previousMatchResult, argValues, num);
		if(next!=null){
			ShapePropMatch<V> continueMatch = next.match(isPatternSide, previousMatchResult, argValues, num);
			return continueMatch;
		}
		return previousMatchResult;
	}
	
	public String toString(){
		return first.toString()+(next==null?"":","+next);
	}
}
