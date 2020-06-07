package com.ak.essprogc;

import static com.ak.essprogc.EssprogCompiler.getNonStringIndex;
import static com.ak.essprogc.EssprogCompiler.isOp;
import static com.ak.essprogc.EssprogCompiler.isReserved;
import static com.ak.essprogc.EssprogCompiler.reserved;

import com.ak.essprogc.EssprogCompiler.Operator;
import com.ak.essprogc.EssprogCompiler.Visibility;
import com.ak.essprogc.errors.DuplicateError;
import com.ak.essprogc.errors.Error;
import com.ak.essprogc.errors.NamingError;
import com.ak.essprogc.errors.StaticError;
import com.ak.essprogc.expr.Expr;
import com.ak.essprogc.map.Def;
import com.ak.essprogc.map.DefType;
import com.ak.essprogc.objects.funcs.essp.Constructor;
import com.ak.essprogc.token.tokens.BlockToken;
import com.ak.essprogc.token.tokens.ChainBlockToken;
import com.ak.essprogc.token.tokens.OwnerToken;
import com.ak.essprogc.token.tokens.Token;
import com.ak.essprogc.token.tokens.cmd.CallFuncToken;
import com.ak.essprogc.token.tokens.cmd.ExitToken;
import com.ak.essprogc.token.tokens.cmd.RedefToken;
import com.ak.essprogc.token.tokens.def.DefToken;
import com.ak.essprogc.token.tokens.def.essp.DefCtorToken;
import com.ak.essprogc.token.tokens.def.essp.DefFuncToken;
import com.ak.essprogc.token.tokens.def.essp.DefGlobalVarToken;
import com.ak.essprogc.token.tokens.def.essp.DefInterfaceToken;
import com.ak.essprogc.token.tokens.def.essp.DefOverloadToken;
import com.ak.essprogc.token.tokens.def.essp.DefTypeToken;
import com.ak.essprogc.token.tokens.def.essp.DefVarToken;

/**
 * Represents a reader that scans Essprog code and generates information.
 * 
 * @author Andrew Klinge
 */
public abstract class Scanner {

	/** Whether the code is currently within a comment. */
	protected boolean multiComment, megaComment;

	//@no format
	protected abstract Def getTree();
	protected abstract void add(DefToken token);
	
	protected void add(Token token) {}
	protected void add(ExitToken token) {}
	protected void add(OwnerToken token) {}
	protected void add(ChainBlockToken token) {}
	protected void add(BlockToken token) {}
	//@format

	/** Prints a warning message. */
	protected abstract void warn(String message);

	/** Rescans part of a line. */
	protected abstract void rescan(String segment);

	/** Exits after a single-line block definition. */
	protected abstract void autoExit();

	protected void reset() {
		multiComment = false;
		megaComment = false;
	}

	/**
	 * Formats the raw line of code for use by the compiler. Handles indentation, comments, aliases, and extra whitespace.
	 * 
	 * @param ignoreIndent Whether not to change codespace based on indentation.
	 * @return The formatted line [0] and whether the line is empty (or commented) [1].
	 * @exception Exception If there is a problem with indentation.
	 */
	protected final String format(String line) {
		line = line.trim(); // remove excess whitespace

		if (!line.isEmpty()) {
			// detect comments
			if (megaComment) {
				if (line.equals(Operator.NOT.op + EssprogCompiler.MEGA_COMMENT)) {
					megaComment = false;
				}
				return "";

			} else {
				int commentIndex = getNonStringIndex(line, EssprogCompiler.COMMENT + "");
				if (commentIndex != -1) {
					if (commentIndex + 1 < line.length() && line.charAt(commentIndex + 1) == EssprogCompiler.COMMENT) { // multiline comment
						if (!line.endsWith(EssprogCompiler.COMMENT + "" + EssprogCompiler.COMMENT) || line.lastIndexOf(EssprogCompiler.COMMENT) - 1 == commentIndex) {
							multiComment = !multiComment;

							if (!multiComment) {
								return "";
							}
						}
					}
					line = line.substring(0, commentIndex);
					if (line.isEmpty()) return "";

				} else if (multiComment) {
					return "";
				} else if (line.equals(EssprogCompiler.MEGA_COMMENT)) {
					megaComment = true;
					return "";
				}
			}

			// finally, remove unnecessary spacing (but preserve keywords!)
			// BEFORE: public static var : (3 + 5 + "a b c") +
			// AFTER: public static var:(3+5+"a b c")+
			String result = "";
			boolean text = false;
			char last = 0;

			for (int i = 0; i < line.length(); i++) {
				char c = line.charAt(i);
				if (c == '"') {
					if (i == 0 || last != '\\') {
						text = !text;
					}
					if (text) {
						while (result.endsWith(" ")) {
							if (Character.isAlphabetic(result.charAt(result.length() - 2))) break;
							result = result.substring(0, result.length() - 1);
						}
					}
				} else if (!text) {
					if (c == ' ' && (last == ' ' || isReserved(last))) continue; // do not add extra spaces
					if (last == ' ' && isOp(c)) result = result.substring(0, result.length() - 1); // do not include spaces between operators
					if (c != '.' && (last == ')' || last == ']' || last == '}') && !isReserved(c)) result += ' '; // make sure there are spaces between end brackets and keywords
				}
				result += c;
				last = c;
			}

			line = result;
		}

		return line;
	}

	/**
	 * Attempts to parse modifiers from the string. <br>
	 * {new_line(string), type(Type), visibility(int), static(boolean), const(boolean)}
	 */
	protected final Object[] tryModifiers(String line) {
		Object[] data = new Object[5];
		int index = -1;

		whole: {
			while ((index = getNonStringIndex(line, " ")) != -1) {
				String mod = line.substring(0, index);
				Visibility vis;

				switch (mod) {
					case EssprogCompiler.PUBLIC:
						vis = Visibility.PUBLIC;
						if (data[2] != null) throw new DuplicateError(mod, "visibility modifiers");
						data[2] = vis;
						break;
					case EssprogCompiler.PRIVATE:
						vis = Visibility.PRIVATE;
						if (data[2] != null) throw new DuplicateError(mod, "visibility modifiers");
						data[2] = vis;
						break;
					case "final":
						if (data[4] != null) throw new DuplicateError(mod, "modifiers");
						data[4] = true;
						break;
					case "static":
						if (data[3] != null) throw new DuplicateError(mod, "modifiers");
						data[3] = true;
						break;
					default: {// possible type modifier
						if (data[1] != null || mod.contains(EssprogCompiler.ASSIGN + "") || mod.contains(EssprogCompiler.SINGLE_LINE_DELIMITER) || mod.contains("(")) break whole;
						else data[1] = mod;
					}
				}

				line = line.substring(mod.length() + 1);
			}
		}

		// check that visibility modifiers are allowed
		if (data[2] != null && !getTree().type.isBase()) throw new Error("Visibility modifiers only have effect when used with objects defined directly within files!");

		// finish and return
		data[0] = line;
		return data;
	}

	protected final void defineObj(String line, boolean mapping) {
		// check for object modifiers
		Object[] data = tryModifiers(line);
		line = (String) data[0];
		String type = (String) data[1];
		Visibility vis = data[2] == null ? Visibility.INTERNAL : (Visibility) data[2];
		int arrowIndex = getNonStringIndex(line, EssprogCompiler.SINGLE_LINE_DELIMITER);
		int def = getNonStringIndex(line, EssprogCompiler.ASSIGN + "");
		if (def != -1 && def != line.length() - 1 && line.charAt(def + 1) == EssprogCompiler.ASSIGN) {
			def = -1;
		}

		if (type == null) { // function call, constructor, or redefinition of variable
			if (def == -1) {
				if (line.startsWith(Constructor.DEF_KEYWORD + "(")) {
					int paren = line.indexOf('(');
					String paramstr = line.substring(paren + 1, arrowIndex == -1 ? (line.charAt(line.length() - 1) == ';' ? line.length() - 2 : line.length() - 1) : arrowIndex - 1);

					if (getTree().type != DefType.TYPE) throw new Error("Constructors must be defined within classes!");
					add(new DefCtorToken(vis, ((DefTypeToken) getTree().token).getName(), paramstr));

					if (arrowIndex != -1) { // singleline func
						rescan(line.substring(arrowIndex + 2));
						autoExit();

					} else if (line.charAt(line.length() - 1) == ';') {
						autoExit();
					}
					return;

				} else if (line.charAt(line.length() - 1) == ')') { // function call
					add(new CallFuncToken(Expr.parse(line)));
					return;
				} else {
					throw new Error("Unrecognizable code!");
				}
			} else { // redefinition
				for (int i = 1; i < data.length; i++) {
					if (data[i] != null) {
						throw new Error("Incorrect use of modifier; is this meant to be a variable definition? The type is missing.");
					}
				}
				add(parseRedefToken(line));
			}
		} else { // class, enum, var, or func def
			if (def == -1) {
				String name = arrowIndex == -1 ? line : line.substring(0, arrowIndex);

				if (type.equals(EssprogCompiler.CLASS)) { // class def
					String[] parents;
					// init parents array
					if (name.charAt(name.length() - 1) == ')') {
						int openParen = name.indexOf('(');
						parents = EssprogCompiler.parseList(name.substring(openParen + 1, name.length() - 1));
						name = name.substring(0, openParen);
					} else {
						parents = new String[0];
					}

					if (Character.isLowerCase(name.charAt(0))) warn("By convention, types' names begin with a capital letter. Your type's name: \"" + name + "\"");
					if (!Character.isAlphabetic(name.charAt(0))) throw new Error("First character of a type's name must be alphabetic!");

					add(new DefTypeToken(vis, name, parents));

					if (arrowIndex != -1) {
						rescan(line.substring(arrowIndex + 2));
						autoExit();
					}
					return;

				} else if (type.equals(EssprogCompiler.INTERFACE)) { // interface def
					if (getTree().type == DefType.INTERFACE || getTree().type == DefType.TYPE) {
						throw new Error("Cannot define types within other types!");
					}

					String[] parents;
					// init parents array
					if (name.charAt(name.length() - 1) == ')') {
						int openParen = name.indexOf('(');
						parents = EssprogCompiler.parseList(name.substring(openParen + 1, name.length() - 1));
						name = name.substring(0, openParen);
					} else {
						parents = new String[0];
					}

					if (Character.isLowerCase(name.charAt(0))) {
						warn("By convention, types' names begin with a capital letter. Your type's name: \"" + name + "\"");
					}
					if (!Character.isAlphabetic(name.charAt(0))) throw new Error("First character of a type's name must be alphabetic!");

					add(new DefInterfaceToken(vis, name, parents));

					if (arrowIndex != -1) {
						rescan(line.substring(arrowIndex + 2));
						autoExit();
					}
					return;

				} else { // var, singleline func, or multiline func
					if (arrowIndex == -1) { // var or multiline func
						if (line.charAt(line.length() - 1) == ')') { // multiline func
							defineFunc(line, type, vis, (data[3] != null), mapping);
							return;

						} else {
							if (line.charAt(line.length() - 1) == ';' && line.charAt(line.length() - 2) == ')') { // empty function
								defineFunc(name.substring(0, name.length() - 1), type, vis, (data[3] != null), mapping);
								autoExit();
								return;

							} else { // new variable without value
								defineVar(line, "", data, vis, mapping);
								return;
							}
						}
					} else { // singleline func
						defineFunc(name, type, vis, (data[3] != null), mapping);
						rescan(line.substring(arrowIndex + 2));
						autoExit();
						return;
					}
				}
			} else { // def != -1 ... variable
				if (type.equals(EssprogCompiler.CLASS) || type.equals(EssprogCompiler.INTERFACE)) throw new Error("Misplaced structure keyword or assignment operator!");
				defineVar(line.substring(0, def), line.substring(def + 1), data, vis, mapping);
				return;
			}
		}
	}

	/** @param line - the entire line (without -> and code if singleline). */
	private final void defineFunc(String line, String type, Visibility visib, boolean isStatic, boolean errorCheck) {
		int paren = line.indexOf('(');
		String name = line.substring(0, paren);
		String paramstr = line.substring(paren + 1, line.length() - 1);

		if (errorCheck) {
			if (getTree().get(DefType.FUNC, name) != null) {
				throw new DuplicateError(name, "functions");
			}
		}

		if (isOp(name.charAt(name.length() - 1))) { // overloader type
			add(new DefOverloadToken(name, type, paramstr));
		} else {
			add(new DefFuncToken(isStatic, visib, name, type, paramstr));
		}
	}

	private final void defineVar(String name, String value, Object[] data, Visibility visib, boolean errorCheck) {
		if (errorCheck) {
			if (getTree().get(DefType.LOCAL_VAR, name) != null) throw new DuplicateError(name, "variables");
			for (int i = 0; i < name.length(); i++) {
				if (reserved.contains(name.charAt(i) + "")) {
					throw new NamingError(name, name.charAt(i));
				}
			}
		}

		// get properties
		String type = (String) data[1];
		boolean isStatic = (data[3] != null);
		boolean isFinal = (data[4] != null);
		boolean isGlobal = getTree().isRoot() || isStatic;
		if (isGlobal) { // global variables must be final
			isFinal = true;
		}

		if (errorCheck) {
			if (getTree().type == DefType.INTERFACE && (!isFinal || !isStatic)) {
				throw new Error("Variables within interfaces must be static and final!");
			}
			if (isStatic && !getTree().isRoot() && getTree().type != DefType.FUNC && getTree().type != DefType.TYPE) {
				// make sure statics are only defined in base file-space or function
				throw new StaticError();
			}
		}

		// create var
		if (isGlobal) {
			add(new DefGlobalVarToken(visib, type, name, Expr.parse(value)));
		} else {
			add(new DefVarToken(visib, isFinal, type, name, Expr.parse(value)));
		}
	}

	/** Parses and returns a RedefToken from the given string. Assumes the line is parsable. */
	protected static RedefToken parseRedefToken(String line) {
		int def = getNonStringIndex(line, EssprogCompiler.ASSIGN + "");
		if(def == -1) {
			throw new Error("Invalid redefinition statement");
		}
		
		char char1 = line.charAt(def - 1);
		if (isOp(char1)) { // self redef
			Operator op = Operator.get(Character.toString(char1));
			char char2 = def - 2 >= 0 ? line.charAt(def - 2) : (char) -1;

			if (isOp(char2)) { // 2-char operator
				String opStr = char1 + "" + char2;
				op = Operator.get(opStr);

				if (op == null) throw new Error("Unrecognized operator: " + opStr);
				return new RedefToken(line.substring(0, def - 2), Expr.parse(line.substring(0, def) + line.substring(def + 1)));

			} else {
				return new RedefToken(line.substring(0, def - 1), Expr.parse(line.substring(0, def) + line.substring(def + 1)));
			}
		} else { // normal redef
			return new RedefToken(line.substring(0, def), Expr.parse(line.substring(def + 1)));
		}
	}
}
