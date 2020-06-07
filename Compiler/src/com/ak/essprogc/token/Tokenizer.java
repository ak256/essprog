package com.ak.essprogc.token;

import static com.ak.essprogc.EssprogCompiler.getNonStringIndex;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import com.ak.essprogc.EssprogCompiler;
import com.ak.essprogc.Scanner;
import com.ak.essprogc.errors.BoolConditionError;
import com.ak.essprogc.errors.Error;
import com.ak.essprogc.errors.ExitError;
import com.ak.essprogc.errors.FormatError;
import com.ak.essprogc.errors.InternalError;
import com.ak.essprogc.expr.Expr;
import com.ak.essprogc.map.Def;
import com.ak.essprogc.objects.types.PrimitiveType;
import com.ak.essprogc.targets.EssprogTarget;
import com.ak.essprogc.token.tokens.BlockToken;
import com.ak.essprogc.token.tokens.ChainBlockToken;
import com.ak.essprogc.token.tokens.OwnerToken;
import com.ak.essprogc.token.tokens.Token;
import com.ak.essprogc.token.tokens.cmd.AutoExitToken;
import com.ak.essprogc.token.tokens.cmd.BreakToken;
import com.ak.essprogc.token.tokens.cmd.ContinueToken;
import com.ak.essprogc.token.tokens.cmd.ExitToken;
import com.ak.essprogc.token.tokens.cmd.ReturnToken;
import com.ak.essprogc.token.tokens.def.DefToken;
import com.ak.essprogc.token.tokens.flow.ElifToken;
import com.ak.essprogc.token.tokens.flow.ElseToken;
import com.ak.essprogc.token.tokens.flow.IfToken;
import com.ak.essprogc.token.tokens.flow.switching.CaseToken;
import com.ak.essprogc.token.tokens.flow.switching.DefaultToken;
import com.ak.essprogc.token.tokens.flow.switching.SwitchToken;
import com.ak.essprogc.token.tokens.loops.ForListToken;
import com.ak.essprogc.token.tokens.loops.ForRangeToken;
import com.ak.essprogc.token.tokens.loops.WhileToken;

/**
 * Compiler for the first compilation pass. <br>
 * Only reads input code and translates it into tokens. Also detects any simple errors.
 * <p>
 * Does not perform error-checks in certain cases, because those checks have already been done by the mapping pass.
 * 
 * @author Andrew Klinge
 */
public final class Tokenizer extends Scanner {

	/** The last created label. Should be nullified as necessary. */
	private String label;

	/** The current tokenized file. */
	public TFile tfile;

	public Tokenizer() {}

	// TODO instead of throwing errors for things that will not affect the Tokenizing stage immediately, collect them and then throw the list after the tokenizing stage is complete

	@Override
	protected void warn(String message) {
		System.err.println("Warning@" + tfile.path + ":" + Token.currentLine + "\n\t" + message);
	}

	/** Returns the label field and then nullifies it. */
	private String useLabel() {
		String result = label;
		label = null;
		return result;
	}

	public void tokenize(EssprogTarget target) {
		reset();

		// setup for compilation
		this.tfile = new TFile(target.getPath());
		String line = "";
		Token.currentLine = 1;
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(target.getFile()));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			return;
		}

		// tokenize
		try {
			while ((line = reader.readLine()) != null) {
				scan(line);
				Token.currentLine++;
			}
		} catch (Error e) {
			e.setLocation(Token.currentLine, target);
			throw e;

		} catch (Exception e) {
			InternalError ie = new InternalError(e);
			ie.setLocation(Token.currentLine, target);
			throw ie;

		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// exit all spaces (cleanup)
		while (tfile.getTree().parent != null) {
			tfile.add(new ExitToken());
		}
	}

	@Override
	protected void autoExit() {
		tfile.add(new AutoExitToken());
		if (tfile.getTree() == null) throw new ExitError();
	}

	@Override
	protected void rescan(String line) {
		scan(line);
	}

	/** Scans a line of code and generates detailed information from it. */
	protected void scan(String line) {
		// format
		line = format(line).trim();
		if (line.isEmpty()) return;

		// manually close block
		if (line.length() == 1 && line.charAt(0) == EssprogCompiler.BLOCK_EXIT) {
			tfile.add(new ExitToken());
			if (tfile.getTree() == null) throw new ExitError();
			return;
		}

		// check for single-keyword line
		switch (line) {
			case EssprogCompiler.BREAK:
				tfile.add(new BreakToken());
				return;
			case EssprogCompiler.RETURN:
				tfile.add(new ReturnToken(null));
				return;
			case EssprogCompiler.CONTINUE:
				tfile.add(new ContinueToken());
				return;
			case EssprogCompiler.DEFAULT:
				addDefaultToken();
				return;
			case EssprogCompiler.ELSE:
				tfile.add(new ElseToken());
				return;
			case EssprogCompiler.BLOCK:
				tfile.add(new BlockToken());
				return;
		}

		// check for label
		if (line.charAt(line.length() - 1) == EssprogCompiler.GENERIC_DELIMITER) {
			if (label != null) throw new Error("Cannot have concurrent labels!");
			label = line.substring(0, line.length() - 1);
			return;
		}

		int spaceIndex = getNonStringIndex(line, " ");
		if (spaceIndex == -1) { // check for condition-less single-line block keywords
			int arrowIndex = getNonStringIndex(line, EssprogCompiler.SINGLE_LINE_DELIMITER);
			if (arrowIndex != -1) {
				String word = line.substring(0, arrowIndex);
				switch (word) {
					case EssprogCompiler.DEFAULT: {
						addDefaultToken();
						scan(line.substring(arrowIndex + 2));
						autoExit();
						return;
					}
					case EssprogCompiler.ELSE: {
						tfile.add(new ElseToken());
						scan(line.substring(arrowIndex + 2));
						autoExit();
						return;
					}
				}
			}
		} else { // check for commands and blocks
			String word = line.substring(0, spaceIndex);
			String info = line.substring(spaceIndex + 1);
			switch (word) {
				case EssprogCompiler.DECLARE:
				case EssprogCompiler.INCLUDE:
				case EssprogCompiler.IMPORT:
					return; // handled by Mapper

				case EssprogCompiler.BREAK: {
					tfile.add(new BreakToken(info));
					return;
				}
				case EssprogCompiler.CONTINUE: {
					tfile.add(new ContinueToken(info));
					return;
				}
				case EssprogCompiler.RETURN: {
					tfile.add(new ReturnToken(Expr.parse(info)));
					return;
				}
				case EssprogCompiler.CASE: {
					OwnerToken otk = tfile.getCurrentOwner();
					if (!(otk instanceof SwitchToken)) throw new Error("Case statements cannot be used outside of switch statements!");
					// continue to below cases
				}
				case EssprogCompiler.WHILE:
				case EssprogCompiler.IF:
				case EssprogCompiler.ELIF: {
					String label = useLabel();
					if (info.isEmpty()) {
						throw new BoolConditionError(PrimitiveType.VOID);
					}

					int defIndex = getNonStringIndex(info, EssprogCompiler.SINGLE_LINE_DELIMITER);
					Expr value = Expr.parse(defIndex == -1 ? info : info.substring(0, defIndex));

					if (word.equals(EssprogCompiler.IF)) tfile.add(new IfToken(value));
					else if (word.equals(EssprogCompiler.ELIF)) tfile.add(new ElifToken(value));
					else if (word.equals(EssprogCompiler.WHILE)) tfile.add(new WhileToken(value, label));
					else if (word.equals(EssprogCompiler.CASE)) tfile.add(new CaseToken(value));

					// compile the body code if single-line
					if (defIndex != -1) {
						scan(info.substring(defIndex + 2));
						autoExit();
					}
					return;
				}
				case EssprogCompiler.SWITCH: {
					String label = useLabel();

					if (info.isEmpty()) {
						throw new BoolConditionError(PrimitiveType.VOID);
					}
					Expr value = Expr.parse(info);
					tfile.add(new SwitchToken(value, label));
					return;
				}
				case EssprogCompiler.FOR: {
					int index = getNonStringIndex(info, EssprogCompiler.GENERIC_DELIMITER);
					if (index == -1) throw new FormatError(line, "e.g. \"for string s : list\" or \"for int x = 0: x < 10: x++\"");
					int index2 = getNonStringIndex(info, EssprogCompiler.GENERIC_DELIMITER, index + 1);
					int defIndex = getNonStringIndex(info, EssprogCompiler.SINGLE_LINE_DELIMITER);
					int endList = (defIndex == -1 ? info.length() : defIndex);

					int spi = info.indexOf(' ');
					String varType;
					if (spi == -1 || spi > index) { // missing iterator type; variable already exists
						varType = null;
						spi = 0;
					} else {
						varType = info.substring(0, spi);
					}

					// loop type determined by number of ':' delimiters
					if (index2 == -1) {
						// list loop
						tfile.add(new ForListToken(varType, info.substring(spi + 1, index), Expr.parse(info.substring(index + 1, endList)), label));
					} else {
						// range loop
						int eq = getNonStringIndex(info, EssprogCompiler.ASSIGN);
						tfile.add(new ForRangeToken( //
										varType, //
										info.substring(spi + 1, eq), //
										Expr.parse(info.substring(eq + 1, index)), //
										Expr.parse(info.substring(index + 1, index2)), //
										parseRedefToken(info.substring(index2 + 1, endList)), //
										label //
						));
					}

					// compile body code if singleline definition
					if (defIndex != -1) {
						scan(info.substring(defIndex + 2));
						autoExit();
					}
					return;
				}
			}
		}

		// if line is so far unrecognizable, try the last possible thing: an object defintion
		defineObj(line, false);
	}

	private void addDefaultToken() {
		OwnerToken otk = tfile.getCurrentOwner();
		if (!(otk instanceof SwitchToken)) throw new Error("Case statements cannot be used outside of switch statements!");
		tfile.add(new DefaultToken());
	}

	@Override
	protected Def getTree() {
		return tfile.getTree();
	}

	@Override
	protected void add(DefToken token) {
		tfile.add(token);
	}

	@Override
	protected void add(Token token) {
		tfile.add(token);
	}

	@Override
	protected void add(ExitToken token) {
		tfile.add(token);
	}

	@Override
	protected void add(OwnerToken token) {
		tfile.add(token);
	}

	@Override
	protected void add(ChainBlockToken token) {
		tfile.add(token);
	}

	@Override
	protected void add(BlockToken token) {
		tfile.add(token);
	}
}