package natlab.tame.builtin.shapeprop;

import java.util.*;
import natlab.tame.valueanalysis.components.shape.*;

public class ShapePropMatch{
	static boolean Debug = false;
	public ShapeFactory factory = new ShapeFactory();
	int numMatched = 0;             //number of matched arguments, act as the index of arguments 
	int numEmittedResults = 0;      //number of emitted results, I cannot say its index of shape equation,
	                                //because there is also non-matching expression in the language.
	int numInVertcat = 0;           //index in vertcat;
	int previousMatchedNumber = 0;
	HashMap<String, Integer> lowercase = new HashMap<String, Integer>();  //lowercase is used like m=prevScalar()
	HashMap<String, Shape<?>> uppercase = new HashMap<String, Shape<?>>();  //mostly, uppercase is used for matching a shape
	boolean whetherLatestMatchedIsNum = false;
	boolean ArrayIndexAssignLeft = false;
	boolean ArrayIndexAssignRight = false;
	boolean isError = false;
	boolean matchingIsDone = false;
	boolean outputIsDone = false;
	boolean isInsideVertcat = false;//this boolean is used for distinguish the lowercase in vertcat or not, like n=previousScalar() and [m,n]
	boolean isInsideAssign = false;
	String previousMatchedLowercase = null;
	String previousMatchedUppercase = null;
	ArrayList<Integer> needForVertcat = new ArrayList<Integer>();
	HashMap<String, Shape<?>> output = new HashMap<String, Shape<?>>();  //used for output results 
	
	
	public ShapePropMatch(){}
	
	/**
	 * constructor referring to parent
	 * @param parent
	 */
	public ShapePropMatch(ShapePropMatch parent){
		this.factory = parent.factory;
        this.numMatched = parent.numMatched;
        this.numEmittedResults = parent.numEmittedResults;
        this.numInVertcat = parent.numInVertcat;
        this.previousMatchedNumber = parent.previousMatchedNumber;
        this.lowercase = parent.lowercase;
        this.uppercase = parent.uppercase;
        this.whetherLatestMatchedIsNum = parent.whetherLatestMatchedIsNum;
        this.ArrayIndexAssignLeft = parent.ArrayIndexAssignLeft;
        this.ArrayIndexAssignRight = parent.ArrayIndexAssignRight;
        this.isError = parent.isError;
        this.matchingIsDone = parent.matchingIsDone;
        this.outputIsDone = parent.outputIsDone;
        this.isInsideVertcat = parent.isInsideVertcat;
        this.isInsideAssign = parent.isInsideAssign;
        this.previousMatchedLowercase = parent.previousMatchedLowercase;
        this.previousMatchedUppercase = parent.previousMatchedUppercase;
        this.needForVertcat = parent.needForVertcat;
        this.output = parent.output;
	}
	
	/**
	 * add new lower case or upper case into current result
	 */
	public ShapePropMatch(ShapePropMatch parent, HashMap<String, Integer> lowercase, HashMap<String, Shape<?>> uppercase){
		this.factory = parent.factory;
		this.numMatched = parent.numMatched;
		this.numEmittedResults = parent.numEmittedResults;
		this.numInVertcat = parent.numInVertcat;
		this.previousMatchedNumber = parent.previousMatchedNumber;
		this.lowercase = parent.lowercase;
		this.uppercase = parent.uppercase;
		if(lowercase!=null){
			this.lowercase.putAll(lowercase);  //using HashMap putAll
		}
		if(uppercase!=null){
			this.uppercase.putAll(uppercase);
		}
		this.whetherLatestMatchedIsNum = parent.whetherLatestMatchedIsNum;
		this.ArrayIndexAssignLeft = parent.ArrayIndexAssignLeft;
		this.ArrayIndexAssignRight = parent.ArrayIndexAssignRight;
	    this.isError = parent.isError;
	    this.matchingIsDone = parent.matchingIsDone;
	    this.outputIsDone = parent.outputIsDone;
	    this.isInsideVertcat = parent.isInsideVertcat;
	    this.isInsideAssign = parent.isInsideAssign;
        this.previousMatchedLowercase = parent.previousMatchedLowercase;
        this.previousMatchedUppercase = parent.previousMatchedUppercase;
        this.needForVertcat = parent.needForVertcat;
        this.output = parent.output;
	}

    /**
     * returns a ShapePropMatch which advances argIndex by one, and refers back to this
     */
    public void comsumeArg(){
    	this.numMatched = this.numMatched+1;
    }
    
    public void setIsError(){
    	this.isError = true;
    }
    //resetIsError is for OR case
    public void resetIsError(){
    	this.isError = false;
    }
    
    public boolean getIsError(){
    	return this.isError;
    }
    
    public void setWhetherLatestMatchedIsNum(boolean condition){
    	this.whetherLatestMatchedIsNum = condition;
    }
    
    public boolean getWhetherLatestMatchedIsNum(){
    	return this.whetherLatestMatchedIsNum;
    }
    
    public void setArrayIndexAssignLeft(boolean condition){
    	this.ArrayIndexAssignLeft = condition;
    }
    
    public boolean isArrayIndexAssignLeft(){
    	return this.ArrayIndexAssignLeft;
    }
    
    public void setArrayIndexAssignRight(boolean condition){
    	this.ArrayIndexAssignRight = condition;
    }
    
    public boolean isArrayIndexAssignRight(){
    	return this.ArrayIndexAssignRight;
    }
    
    public void saveLatestMatchedNumber(int latestMatchedNumber){
    	this.previousMatchedNumber = latestMatchedNumber;
    }
    
    public int getLatestMatchedNumber(){
    	return previousMatchedNumber;
    }
    
    public void saveLatestMatchedLowercase(String latestMatchedLowercase){
    	this.previousMatchedLowercase = latestMatchedLowercase;
    }
    
    public String getLatestMatchedLowercase(){
    	return this.previousMatchedLowercase;
    }
    
    public void saveLatestMatchedUppercase(String latestMatchedUpperCase){
    	this.previousMatchedUppercase = latestMatchedUpperCase;
    }
    
    public String getLatestMatchedUppercase(){
    	return this.previousMatchedUppercase;
    }
    
    public int getNumMatched(){
    	return numMatched;
    }
    
    public int getNumEmittedResults(){
    	return numEmittedResults;
    }
    
    public void setNumInVertcat(int index){
    	this.numInVertcat = index;
    }
    
    public int getNumInVertcat(){
    	return this.numInVertcat;
    }
    
    public void setMatchingIsDone(){
    	this.matchingIsDone = true;
    }
    
    public void setOutputIsDone(){
    	this.outputIsDone = true;
    }
    
    public void setIsInsideVertcat(boolean condition){
    	this.isInsideVertcat = condition;
    }
    
    public boolean matchingIsDone(){
    	return matchingIsDone;
    }
    
    public boolean outputIsDone(){
    	return outputIsDone;
    }
    
    public boolean isInsideVertcat(){
    	return isInsideVertcat;
    }
    
    public void setIsInsideAssign(boolean condition){
    	this.isInsideAssign = condition;
    }
    
    public boolean isInsideAssign(){
    	return isInsideAssign;
    }
    
    public boolean hasValue(String key){
    	if(this.lowercase.get(key)==null){
    		return false;
    	}
    	else{
    		return true;
    	}
    }
    
    public int getValueOfVariable(String key){
    	int value = this.lowercase.get(key);
    	return value;
    }
    
    public Shape<?> getShapeOfVariable(String key){
    	Shape<?> shape = this.uppercase.get(key);
    	return shape;
    }
    
    public List<Shape<?>> getAllResults(){//FIXME better!
    	LinkedList<Shape<?>> results = new LinkedList<Shape<?>>();
    	if (Debug) System.out.println(output);
    	if (Debug) System.out.println(needForVertcat);
    	for(Shape<?> value: output.values()){
    		results.add(value);    		
    	}
    	return results;
    }
    
    public HashMap<String, Integer> getAllLowercase(){
    	return lowercase;
    }
    
    public HashMap<String, Shape<?>> getAllUppercase(){
    	return uppercase;
    }
    
    public void addToVertcatExpr(Integer i){
    	this.needForVertcat.add(i);
    }
    
    public ArrayList<Integer> getOutputVertcatExpr(){
    	return needForVertcat;
    }
    
    public void addToOutput(String s,Shape<?> value){
    	this.output.put(s, value);
    }
    
    public Shape<?> getOutput(String key){
    	return output.get(key);
    }
    
    public void copyVertcatToOutput(String defaultM){
    	Shape<?> shape = (new ShapeFactory()).newShapeFromIntegers(this.getOutputVertcatExpr());
    	if (Debug) System.out.println("inside copy vertcat to output "+needForVertcat);
    	this.addToOutput(defaultM, shape);
    }
    
/*    @Override
    public String toString() {
        return "machresult-"+numMatched+"-"+getAllResults();
    }*/

}
