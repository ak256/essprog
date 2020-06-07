package com.ak.essprogc.expr;

import static com.ak.essprogc.EssprogCompiler.getExitIndex;
import static com.ak.essprogc.EssprogCompiler.isOp;
import static com.ak.essprogc.EssprogCompiler.isReserved;
import static com.ak.essprogc.EssprogCompiler.precedence;

import java.util.ArrayList;

import com.ak.essprogc.EssprogCompiler;
import com.ak.essprogc.EssprogCompiler.Operator;
import com.ak.essprogc.objects.types.PrimitiveType;
import com.ak.essprogc.symbol.Symbolizer;
import com.ak.essprogc.symbol.symbols.LocalSymbol;

/**
 * Represents an expression broken down into most basic steps.
 * Essentially an abstract syntax tree.
 * 
 * @author Andrew Klinge
 */
public abstract class Expr {

	/** Empty value. Writes nothing. */
	public static final Expr NULL = new Expr() {
		@Override
		public Result symbolize(Symbolizer sym, ArrayList<LocalSymbol> locals) {
			return new Result(PrimitiveType.VOID, new Value(PrimitiveType.VOID.id()));
		}

		@Override
		public String toString() {
			return EssprogCompiler.NULL_VALUE;
		}
	};

	/**
	 * For Symbolizer only. Generates symbols and returns the resulting type.
	 * 
	 * @param locals - add all generated local symbols to this list.
	 * @return the resulting type and identifier for the symbolized expression.
	 */
	public abstract Result symbolize(Symbolizer sym, ArrayList<LocalSymbol> locals);

	/**
	 * Adds symbols to the symbolizer's current local symbols list.
	 * 
	 * @see #symbolize(Symbolizer, ArrayList)
	 */
	public final Result symbolize(Symbolizer sym) {
		return symbolize(sym, sym.getLocals());
	}

	/** Parses and returns an expression tree from the given expression. */
	public static Expr parse(String str) {
		ExprBinary at = new ExprBinary(null, null); // the root of the tree
		StringBuilder opstore = new StringBuilder(); // stores operator characters
		StringBuilder adder = new StringBuilder(); // stores value characters
		boolean text = false; // whether inside of a string

		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			boolean add = (c != '\\'); // whether to add the char to adder. ignore escape char
			boolean leftSide = (opstore.length() == 0); // whether on the left side of the binary expression (before the operator)
			String adderStr = adder.toString();
			String opStr = opstore.toString();

			if (c == '"') { // string
				if (i == 0 || str.charAt(i - 1) != '\\') {
					text = !text; // ignore string contents
				}
			} else if (!text) {
				if (c == ' ') {
					add = false; // ignore spaces
				} else if (c == '(') { // grouping or function
					int closeIndex = getExitIndex(str, i);
					String contents = str.substring(i + 1, closeIndex);
					Expr group; // the expression denoted by the parentheses group

					if (i > 0 && (!isReserved(str.charAt(i - 1)) || str.charAt(i - 1) == ')')) { // function
						if (adderStr.isEmpty()) { // function reference call ... func()()
							Expr leftLink = leftSide ? at.left() : at.right();
							ExprCall rightLink = new ExprCall(null, contents); // a reference call is only a parameter group, null name
							group = new ExprCallChain(leftLink, rightLink);

						} else { // function call
							if (adderStr.startsWith(".")) { // function sub call ... func().subfunc()
								Expr leftLink = leftSide ? at.left() : at.right();
								ExprCall rightLink = new ExprCall(adderStr.substring(1), contents);
								group = new ExprCallChain(leftLink, rightLink);

							} else { // standalone function ... func()
								if (Character.toString(adderStr.charAt(0)).equals(Operator.REF.op)) { // @func()
									group = new ExprFuncRef(adderStr.substring(1), contents);
								} else { // func()
									group = new ExprCall(adderStr, contents);
								}
							}
						}
					} else { // parenthetical grouping
						char cc = (adderStr.length() == 1 ? adderStr.charAt(0) : 0); // operator before parenthesis ... !()
						if (isReserved(cc)) { // operation on the group
							group = new ExprUnary(Operator.get(Character.toString(cc)), parse(contents));
						} else { // ignore character, not an operator
							group = parse(contents);
						}
					}

					// add to tree
					if (leftSide) at.setLeft(group);
					else at.setRight(group);

					adder.setLength(0);
					i = closeIndex;
					add = false;

				} else if (c == '{') { // array initializer ... {}
					int closeIndex = getExitIndex(str, i);
					Expr arr = new ExprArray(str.substring(i + 1, closeIndex), adderStr);

					if (adderStr.length() == 1 && isReserved(adderStr.charAt(0))) {
						// unary op done on array initializer ... !{}
						arr = new ExprUnary(Operator.get(Character.toString(adderStr.charAt(0))), arr);
					}

					if (leftSide) at.setLeft(arr);
					else at.setRight(arr);

					adder.setLength(0);
					i = closeIndex;
					add = false;

				} else if (isOp(c)) { // operator
					// add value used in operation to tree
					if (!adderStr.isEmpty()) {
						if (leftSide) { // ex: adderStr + ___
							at.setLeft(parseValue(adderStr));
						} else { // ex: (___ + adderStr) + ___
							at.setRight(parseValue(adderStr));
						}
					}

					if (opStr.equals("") || (i > 0 && isOp(str.charAt(i - 1)))) { // accumulate operator from adjacent operator characters (ex: != consists of ! and =)
						if (!(adderStr.isEmpty() && (opStr + c).equals("-")) && (Operator.get(opStr + c) != null || (opStr.isEmpty() && isOp(c)))) {// if the operator exists, then it is an operator character
							if (opStr.equals("-") && c == '-') { // this and the above line handle negatives and double negatives
								opStr = "+";
							} else {
								opStr += c;
							}
							add = false;
						}
					} else { // existing operator operation must be added to tree ... in "(a + b) * c", "(a+b)" must be abstracted-out so we get "x * c"
						int endIndex = i + 1; // end index of operator

						// get entire operator if operator is more than 1 character long
						for (int i2 = endIndex; i2 < str.length(); i2++) {
							if (isOp(str.charAt(i2))) endIndex++;
							else break;
						}
						Operator nextOp = Operator.get(str.substring(i, endIndex)); // second (right-most) operator
						at.op = Operator.get(opStr); // first (left-most) operator

						// group values in operation by operator precedence
						int p1 = precedence(nextOp); // right-most op
						int p2 = precedence(at.op); // left-most op

						if (p1 > p2) { // group around the right-most op
							ExprBinary right = new ExprBinary(at, nextOp);
							right.setLeft(at.right());

							at.setRight(right);
							at = right;

						} else if (p1 < p2) { // group around the left-most op
							ExprBinary last = at;
							ExprBinary match = at.parent();

							// correctly add to the binary tree based on precedence of operators
							while (match instanceof ExprBinary && p1 < (p2 = precedence(((ExprBinary) match).op))) {
								last = match;
								match = match.parent();
							}

							ExprBinary tr = new ExprBinary(match, nextOp);
							tr.setLeft(last);

							if (match != null) {
								if (last == match.right()) match.setRight(tr);
								else match.setLeft(tr);
							}

							at = tr;

						} else { // group from left to right (equal operator precedence)
							ExprBinary left = new ExprBinary(at, at.op); // group existing values into left node, add right to open right node spot
							left.setLeft(at.left());
							left.setRight(at.right());

							at.op = nextOp;
							at.setLeft(left);
							at.setRight(null);
						}

						opStr = nextOp.op;
						i = endIndex - 1;
						add = false;
					}

					adder.setLength(0);
				}
			}

			// store valid value characters
			if (add) {
				adder.append(c);

				// if this is the last character, then add the stored value to the tree.
				// otherwise, would be incorrectly ignored because there is no next operator
				if (i + 1 == str.length()) {
					if (opstore.length() == 0) {
						at.setLeft(parseValue(adder.toString()));
					} else {
						at.setRight(parseValue(adder.toString()));
					}
				}
			}
		}

		// initialize binary tree operator if not done so already
		if (at.op == null) {
			at.op = Operator.get(opstore.toString());
		}

		// after traversing the tree to some point, we need to get back to root so we can return the entire tree
		while (at.parent() != null) {
			at = (ExprBinary) at.parent();
		}

		// the expression may not actually be a binary operation
		if (at.op == null && at.right() == NULL) {
			if (at.left() == NULL) return NULL; // the expression is empty
			return at.left(); // the expression is a single value/unary-operation
		}

		return at;
	}

	/** Helper method for parse(). Returns an ExprVal or an ExprUnary parsed from the string. */
	private static Expr parseValue(String string) {
		if (string.isEmpty()) return null;

		char first = string.charAt(0);
		if (first != '"' && first != '-' && isReserved(first)) {
			return new ExprUnary(Operator.get(Character.toString(first)), new ExprVal(string.substring(1)));
		} else {
			return new ExprVal(string);
		}
	}
}