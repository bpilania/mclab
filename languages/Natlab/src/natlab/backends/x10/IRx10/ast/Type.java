package natlab.backends.x10.IRx10.ast;


/**
 * @ast node
 * @declaredat irx10.ast:9
 */
public class Type extends AccessVal implements Cloneable {
  /**
   * @apilevel low-level
   */
  public void flushCache() {
    super.flushCache();
  }
  /**
   * @apilevel internal
   */
  public void flushCollectionCache() {
    super.flushCollectionCache();
  }
  /**
   * @apilevel internal
   */
  @SuppressWarnings({"unchecked", "cast"})
  public Type clone() throws CloneNotSupportedException {
    Type node = (Type)super.clone();
    return node;
  }
  /**
   * @apilevel internal
   */
  @SuppressWarnings({"unchecked", "cast"})
  public Type copy() {
      try {
        Type node = (Type)clone();
        if(children != null) node.children = (ASTNode[])children.clone();
        return node;
      } catch (CloneNotSupportedException e) {
      }
      System.err.println("Error: Could not clone node of type " + getClass().getName() + "!");
      return null;
  }
  /**
   * @apilevel low-level
   */
  @SuppressWarnings({"unchecked", "cast"})
  public Type fullCopy() {
    Type res = (Type)copy();
    for(int i = 0; i < getNumChildNoTransform(); i++) {
      ASTNode node = getChildNoTransform(i);
      if(node != null) node = node.fullCopy();
      res.setChild(node, i);
    }
    return res;
    }
  /**
   * @ast method 
   * @declaredat irx10.ast:1
   */
  public Type() {
    super();


  }
  /**
   * @ast method 
   * @declaredat irx10.ast:7
   */
  public Type(String p0) {
    setName(p0);
  }
  /**
   * @apilevel low-level
   * @ast method 
   * @declaredat irx10.ast:13
   */
  protected int numChildren() {
    return 0;
  }
  /**
   * Setter for lexeme Name
   * @apilevel high-level
   * @ast method 
   * @declaredat irx10.ast:5
   */
  public void setName(String value) {
    tokenString_Name = value;
  }
  /**   * @apilevel internal   * @ast method 
   * @declaredat irx10.ast:8
   */
  
  /**   * @apilevel internal   */  protected String tokenString_Name;
  /**
   * Getter for lexeme Name
   * @apilevel high-level
   * @ast method 
   * @declaredat irx10.ast:13
   */
  public String getName() {
    return tokenString_Name != null ? tokenString_Name : "";
  }
}
