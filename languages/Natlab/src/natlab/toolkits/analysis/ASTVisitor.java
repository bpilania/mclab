/**
 * Interface of visitor pattern for traversing the AST.
 * Change Log:
 *  - 2008.06 by Jun Li 
 *		- file created
 *  - 2008.06 by Jun Li 
 *		- adding caseSwitchStmt()
 */

package natlab.toolkits.analysis;

import ast.ASTNode;
import ast.IfStmt;
import ast.SwitchStmt;

public interface ASTVisitor 
{
	public void caseASTNode(ASTNode node);
	public void caseLoopStmt(ASTNode node);
	public void caseIfStmt(IfStmt node);
	public void caseSwitchStmt(SwitchStmt node);
	public void caseBranchingStmt(ASTNode node);
}
