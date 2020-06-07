package com.ak.essprogc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import com.ak.essprogc.errors.Error;
import com.ak.essprogc.expr.Expr;
import com.ak.essprogc.map.FileType;
import com.ak.essprogc.map.Mapper;
import com.ak.essprogc.objects.blocks.Filespace;
import com.ak.essprogc.objects.types.Type;
import com.ak.essprogc.objects.types.UserType;
import com.ak.essprogc.symbol.EssprogOutputStream;
import com.ak.essprogc.symbol.Symbolizer;
import com.ak.essprogc.targets.CTarget;
import com.ak.essprogc.targets.EssprogTarget;
import com.ak.essprogc.targets.OperatingSystem;
import com.ak.essprogc.token.Tokenizer;
import com.ak.essprogc.token.tokens.TParameter;

/**
 * Essprog compiler environment.
 * 
 * @author Andrew Klinge
 */
public final class EssprogCompiler {

	// TODO memory management (ARC - SWIFT)
	// TODO polymorphism & inheritance
	// TODO classes & interfaces

	/** Essprog compiler version (release-date). */
	public static final String VERSION = "4.21.2020";

	public static final OperatingSystem OS;
	static {
		String raw = System.getProperty("os.name");
		if (raw == null)
			throw new RuntimeException("Failed to identify operating system!");

		raw = raw.toLowerCase();
		if (raw.contains("windows"))
			OS = OperatingSystem.WINDOWS;
		else if (raw.contains("mac"))
			OS = OperatingSystem.MAC;
		else if (raw.contains("linux") || raw.contains("mpe/ix") || raw.contains("freebsd") || raw.contains("irix")
			|| raw.contains("unix"))
			OS = OperatingSystem.UNIX;
		else if (raw.contains("sun") || raw.contains("solaris") || raw.contains("hp-ux") || raw.contains("aix"))
			OS = OperatingSystem.POSIX_UNIX;
		else
			throw new RuntimeException("Unknown or unsupported operating system \"" + raw + "\"");
	}

	/** Input Essprog code file extension. */
	public static final String CODE_EXT = ".essp";

	public static final String C_CODE_EXT = ".c";

	/** Output LLVM IR bytecode file extension. */
	public static final String LLVM_IR_EXT = ".ll";

	/** Output static library file extension. */
	public static final String LIBRARY_EXT = ".essplib";

	/** Static library manifest file name. */
	public static final String MANIFEST = "manifest.txt";

	/** Default binaries directory (where to put intermediate LLVM IR files). */
	static final File DEFAULT_BIN = new File("bin/");

	/** Name of the binaries directory's log file from last compilation. */
	static final String BIN_LOG = "last.log";

	static File getBinLog(File binDir) {
		return new File(binDir, BIN_LOG);
	}

	public static final ZipFile STD_LIB;
	static {
		try {
			STD_LIB = new ZipFile(new File("stdlib" + LIBRARY_EXT));
		} catch (Exception e) {
			throw new Error("Failed to find Essprog standard library!");
		}
	}

	public static final Filespace SYSFUNCS_FS = new Filespace(FileType.STANDARD, "essprog.sysfuncs.cpp");
	public static final Filespace STRING_FS = new Filespace(FileType.ELEVATED, "essprog.string");
	public static final UserType STRING = new UserType(Visibility.PUBLIC, "string", new Type[0], STRING_FS, "%\"essprog.string:0\"");

	// @no format
	public static final String 
		SINGLE_LINE_DELIMITER = "->", 
		MEGA_COMMENT = "IGNORE",
		SUPER = "super",
		NULL_VALUE = "null",
		TRUE_VALUE = "true",
		FALSE_VALUE = "false",
		STRING_DELIMITER = "\"",
		PUBLIC = "public",
		PRIVATE = "private",
		CLASS = "class",
		INTERFACE = "interface",
		BREAK = "break",
		CONTINUE = "continue",
		DEFAULT = "default",
		ELSE = "else",
		BLOCK = "block",
		DECLARE = "declare",
		IMPORT = "import",
		RETURN = "return",
		CASE = "case",
		WHILE = "while",
		IF = "if",
		ELIF = "elif",
		SWITCH = "switch",
		FOR = "for",
		STRUCT = "struct",
		MAIN_FUNC = "main",
		INCLUDE = "include",
		VA_ARG = "...";
	
	/** Name of methods' implicit owner-class instance variable. */
	public static final String METHOD_OBJ_INSTANCE = "%inst";
	
	public static final char
		COMMENT = '#',
		ASSIGN = '=',
		GENERIC_DELIMITER = ':',
		BLOCK_EXIT = ';',
		IMPORT_PATH_SEPARATOR = '.';
	
	public static enum Operator {
		ADD("+"), 
		SUBTRACT("-"), 
		MULTIPLY("*"), 
		DIVIDE("/"), 
		POWER("**"), 
		BIT_AND("&"), 
		BIT_OR("|"), 
		BIT_XOR("^"), 
		BIT_NOT("~"), 
		BIT_LSHIFT("<<"),
		BIT_RSHIFT(">>"), 
		GREATER(">"),
		LESS("<"), 
		GREATER_EQUAL(">="), 
		LESS_EQUAL("<="), 
		NOT_EQUAL("!="),
		EQUAL("=="), 
		AND("&&"), 
		OR("||"), 
		NOT("!"), 
		REF("@"), 
		MODULUS("%");

		public final String op;

		Operator(String op) {
			this.op = op;
		}
		
		@Override
		public String toString() {
			return op;
		}
		
		public static Operator get(String op) {
			for (Operator operator : Operator.values()) {
				if (operator.op.equals(op)) return operator;
			}
			return null;
		}
	}
	
	/** Returns the precedence of the operator. */
	public static int precedence(Operator op) {
		switch (op) {
		case REF:
		case BIT_NOT:
		case NOT:
			return 10;
			
		case POWER:
			return 9;
			
		case MULTIPLY:
		case DIVIDE:
		case MODULUS:
			return 8;

		case ADD:
		case SUBTRACT:
			return 7;

		case BIT_LSHIFT:
		case BIT_RSHIFT:
			return 6;

		case BIT_AND:
			return 5;

		case BIT_XOR:
			return 4;

		case BIT_OR:
			return 3;

		case EQUAL:
		case NOT_EQUAL:
		case LESS:
		case LESS_EQUAL:
		case GREATER:
		case GREATER_EQUAL:
			return 2;

		case AND:
			return 1;

		case OR:
			return 0;
		}
		
		return -1;
	}

	/** Operator characters. */
	public static final String ops = "+*-/&^%|>=<";

	/** Reserved characters (i.e. cannot be used in variable names). */
	public static final String reserved = "!~@$?:;()[]{}\\#`,'\"" + ops;

	/** Object visibility modifiers. */
	public enum Visibility {
		PUBLIC(EssprogCompiler.PUBLIC),			// visible to all external sources
		INTERNAL(null), // visible to all within the same library (compiled group). default vis
		PRIVATE(EssprogCompiler.PRIVATE);		// visible to no external sources

		private final String keyword;

		Visibility(String keyword) {
			this.keyword = keyword;
		}
		
		@Override
		public String toString() {
			return keyword;
		}
	}

	/** Whether the character is an Essprog operator. */
	public static boolean isOp(char c) {
		return ops.contains(c + "");
	}

	/** Whether the given character is reserved (a.k.a. cannot be used in naming objects). */
	public static boolean isReserved(char c) {
		return reserved.contains(c + "");
	}

	/** Returns whether the first character of a name is valid. */
	public static boolean isValidNameChar(char c) {
		return !isReserved(c) && !Character.isDigit(c);
	}

	/** First parses the string as a list then parses each item as an OpTree. */
	public static Expr[] parseExprList(String list) {
		String[] split = parseList(list);
		Expr[] result = new Expr[split.length];
		for (int i = 0; i < split.length; i++) {
			result[i] = Expr.parse(split[i]);
		}
		return result;
	}

	/**
	 * Returns the index of the corresponding closing bracket ")".
	 * 
	 * @param value is the string to search in.
	 * @param i is the index of the opening bracket "(".
	 */
	public static int getExitIndex(String value, int i) {
		int level = 0;
		boolean text = false;

		for (int a = i; a < value.length(); a++) {
			char c = value.charAt(a);

			if (c == '"') {
				if (a == 0 || value.charAt(a - 1) != '\\') {
					text = !text;
				}
			} else if (!text) {
				if (c == '(' || c == '[' || c == '{') {
					level++;

				} else if (c == ')' || c == ']' || c == '}') {
					level--;

					if (level == 0) return a;
				}
			}
		}
		return -1;
	}

	/**
	 * Parses a list of comma-separated strings into an array. <br>
	 * e.g: parseList("alpha, beta, 123") -> {"alpha", "beta", "123"}
	 * 
	 * @param str is the string to be parsed.
	 * @return The parsed list.
	 */
	public static String[] parseList(String str) {
		ArrayList<String> list = new ArrayList<String>();
		boolean inText = false;
		int lastComma = -1;
		int level = 0;

		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);

			if (c == '"') {
				if (i == 0 || str.charAt(i - 1) != '\\') {
					inText = !inText;
				}
			} else if (!inText) {
				if (c == ',') {
					if (level == 0) {
						list.add(str.substring(lastComma + 1, i));
						lastComma = i;
					}
				} else if (c == ']' || c == ')' || c == '}') {
					level++;
				} else if (c == '[' || c == '(' || c == '{') {
					level--;
				}
			}
		}
		if (!str.isEmpty()) list.add(str.substring(lastComma + 1));

		// to array
		String[] result = new String[list.size()];
		for (int i = 0; i < list.size(); i++) {
			result[i] = list.get(i);
		}
		return result;
	}

	/**
	 * Returns the index of the target string in the container string if it is not within another string (i.e. quotations "").
	 * 
	 * @param target - should not contain unescaped quotation marks (")
	 * @param fromIndex - should not be an index inside a string ("")
	 */
	public static int getNonStringIndex(String container, String target) {
		return getNonStringIndex(container, target, 0);
	}

	/**
	 * Returns the index of the target character in the container string if it is not within another string (i.e. quotations "").
	 * 
	 * @param fromIndex - should not be an index inside a string ("").
	 */
	public static int getNonStringIndex(String container, char target) {
		return getNonStringIndex(container, target, 0);
	}

	/**
	 * Returns the index of the target string in the container string if it is not within another string (i.e. quotations "").
	 * 
	 * @param target - should not contain unescaped quotation marks (")
	 * @param fromIndex - should not be an index inside a string ("")
	 */
	public static int getNonStringIndex(String container, String target, int fromIndex) {
		boolean text = false;
		int t = 0; // index in target for comparison

		for (int i = fromIndex; i < container.length(); i++) {
			char c = container.charAt(i);

			if (c == '"') {
				if (i == 0 || container.charAt(i - 1) != '\\') {
					text = !text;
				}
			} else if (!text) { // compare with target
				if (c == target.charAt(t)) {
					if (t == target.length() - 1) return i - t; // full match is found
					t++;
				} else { // reset comparison
					t = 0;
				}
			}
		}
		return -1;
	}

	/**
	 * Returns the index of the target character in the container string if it is not within another string (i.e. quotations "").
	 * 
	 * @param fromIndex - should not be an index inside a string ("").
	 */
	public static int getNonStringIndex(String container, char target, int fromIndex) {
		boolean text = false;

		for (int i = fromIndex; i < container.length(); i++) {
			char c = container.charAt(i);

			if (c == '"') {
				if (i == 0 || container.charAt(i - 1) != '\\') {
					text = !text;
				}
			} else if (!text) { // compare with target
				if (c == target) {
					return i; // match found
				}
			}
		}
		return -1;
	}

	/**
	 * Returns the last index of the target character in the container string if it is not within another string (i.e. quotations "").
	 * 
	 * @param fromIndex - should not be an index inside a string ("").
	 */
	public static int getLastNonStringIndex(String container, char target, int fromIndex) {
		boolean text = false;

		for (int i = fromIndex; i > -1; i--) {
			char c = container.charAt(i);

			if (c == '"') {
				if (i == 0 || container.charAt(i - 1) != '\\') {
					text = !text;
				}
			} else if (!text) { // compare with target
				if (c == target) {
					return i; // match found
				}
			}
		}
		return -1;
	}

	/** Parses TParameters from the string. */
	public static TParameter[] parseParams(String str) {
		String[] list = parseList(str);
		TParameter[] params = new TParameter[list.length];

		for (int i = 0; i < list.length; i++) {
			String s = list[i];
			int index = s.indexOf(' ');
			params[i] = new TParameter(s.substring(0, index), s.substring(index + 1));
		}
		return params;
	}

	private final Mapper mapper = new Mapper();
	private final Tokenizer tokenizer = new Tokenizer();
	private final Symbolizer symbolizer = new Symbolizer(mapper);

	/** Whether to report the time that processes take to complete. */
	private boolean printInfo;

	public EssprogCompiler() {}

	public void setPrintInfo(boolean printInfo) {
		this.printInfo = printInfo;
	}

	public Mapper getMapper() {
		return mapper;
	}

	public Tokenizer getTokenizer() {
		return tokenizer;
	}

	public Symbolizer getSymbolizer() {
		return symbolizer;
	}

	/**
	 * Returns whether the string starts with the target string, but begins comparing the strings from the end of the target, working backwards.
	 * <br>
	 * Marginally more efficient than startsWith when the beginning of the two strings are lengthy and very similar up until the end.
	 * 
	 * @param string - the string to check.
	 * @param target - the starting-with phrase to check for.
	 */
	private static boolean startsWithFromEnd(String string, String target) {
		if (target.length() > string.length()) return false;

		for (int i = target.length() - 1; i > -1; i--) {
			if (string.charAt(i) != target.charAt(i)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Runs the full compilation process.
	 * <p>
	 * Mapped files are retained in memory after this process completes until clean() is called.
	 * 
	 * @param files      - list of files to compile.
	 * @param cPackages  - list of C file package specifiers.
	 * @param main       - the main file, if relevant (can be null when producing a library).
	 * @param createLib  - if true, this will produce a static library. if false, this will produce an executable.
	 * @param outputName - the name of the resulting file (library or executable).
	 * @param binDir     - the binaries directory that contains compiled code.
	 * @throws Exception - if an error occurs due to missing or invalid input.
	 * @param clean - whether to delete intermediate files and recompile
	 * @return whether compilation succeeded.
	 */
	public boolean run(ArrayList<File> files, ArrayList<String> cPackages, String main, boolean createLib,
		String outputName, File binDir) {
		ArrayList<EssprogTarget> esspTargets = new ArrayList<EssprogTarget>();
		ArrayList<CTarget> cTargets = new ArrayList<CTarget>();
		long lastCompileTime = 0;

		binDir.mkdir();
		mapper.clean();

		// prepare binaries directory
		if (binDir == DEFAULT_BIN) {
			// clear default binaries directory
			for (File file : DEFAULT_BIN.listFiles()) {
				file.delete();
			}
		} else {
			File binLog = getBinLog(binDir);
			if (binLog.exists()) {// load log details
				try {
					BufferedReader reader = new BufferedReader(new FileReader(binLog));
					String line = null;
					while ((line = reader.readLine()) != null) {
						int separator = line.indexOf('=');
						String var = line.substring(0, separator);
						String value = line.substring(separator + 1);

						switch (var) {
						case "last":
							lastCompileTime = Long.parseLong(value);
							break;
						}
					}
					reader.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			// overwrite log file
			try {
				EssprogOutputStream logWriter = new EssprogOutputStream(new FileOutputStream(binLog));
				logWriter.write("last=" + System.currentTimeMillis()); // last compilation time
				logWriter.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// load standard library
		mapper.loadLibrary(STD_LIB, STRING_FS, SYSFUNCS_FS);

		try {
			// load compile targets
			for (File file : files) {
				if (file.getName().endsWith(CODE_EXT)) {
					EssprogTarget t = new EssprogTarget(file);
					esspTargets.add(t);
				} else if (file.getName().endsWith(C_CODE_EXT)) {
					String _package = null;
					// find package
					for (String cPackage : cPackages) {
						if (startsWithFromEnd(file.getPath(), cPackage)) {
							_package = cPackage;
							break;
						}
					}
					if (_package == null) {
						throw new Error("ERROR: required package specifier for C target \"" + file.getPath() + "\" is missing");
					}

					CTarget cTarget = new CTarget(file, _package);
					// check if compiling of c/c++ files needs to be redone (can save a lot of time! recompiling them is very slow)
					cTarget.preventRecompile(binDir, lastCompileTime);
					cTargets.add(cTarget);

				} else if (file.getName().endsWith(LIBRARY_EXT)) {
					loadLibrary(file);

				} else {
					throw new Error("ERROR: unusable target file type \"" + file.getName() + "\"");
				}
			}

			// compile
			if (createLib) {
				createLibrary(outputName, esspTargets, cTargets, binDir, lastCompileTime);
			} else {
				createExecutable(outputName, main, esspTargets, cTargets, binDir, lastCompileTime);
			}

			return true;
		} catch (Error e) {
			e.print();
			return false;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/** Loads the library. */
	private void loadLibrary(File file) {
		long time = System.currentTimeMillis();

		try {
			mapper.loadLibrary(new ZipFile(file));
		} catch (IOException e) {
			System.out.println("ERROR: failed to load library \"" + file.getPath() + "\"");
		}

		if (printInfo) {
			System.out.println("Loading library \"" + file.getName() + "\" took " + secondsSince(time) + " seconds");
		}
	}

	/**
	 * Compiles the project and creates an executable file.
	 * <br>
	 * NOTE: esspTargets may contain mainTarget.
	 */
	private void createExecutable(String outputName, String main, ArrayList<EssprogTarget> esspTargets, ArrayList<CTarget> cTargets, File binDir, long lastCompileTime) throws IOException, InterruptedException {
		mapper.setMain(main);

		// map essprog files
		long time = System.currentTimeMillis();
		for (EssprogTarget target : esspTargets) {
			mapper.map(target, binDir, lastCompileTime);
		}
		long esspMapTime = (System.currentTimeMillis() - time); // time taken to process Essprog files

		// compile then map all C targets (mapping C target requires LLVM IR code)
		if (!cTargets.isEmpty()) {
			time = System.currentTimeMillis();

			for (CTarget target : cTargets) {
				target.compile(this, null, binDir);
				mapper.map(target, binDir);
			}

			if (printInfo) {
				System.out.println("Compiling C/C++ files took " + secondsSince(time) + " seconds");
			}
		}

		// convert maps into objects
		time = System.currentTimeMillis();
		mapper.objectify();

		// compile targets, placing compiled files next to targets
		for (EssprogTarget target : esspTargets) {
			target.compile(this, null, binDir);
		}
		if (printInfo) {
			System.out.println("Compiling Essprog files took " + secondsSince(time - esspMapTime) + " seconds");
		}

		// FIXME delete all intermediate files that no longer have an existing source file (which was deleted by user)
		/*
		File[] binaries = binDir.listFiles();
		for (File binary : binaries) {
			if (binary.getPath().endsWith(EssprogCompiler.LLVM_IR_EXT)) {
				String declaredPath
			}
		}*/

		// compile all intermediate LLVM IR files into executable
		time = System.currentTimeMillis();
		Runtime rt = Runtime.getRuntime();
		Process proc = rt.exec(new String[] {
			"./scripts/compile-" + OS.nickname + OS.scriptExt,
				binDir.getAbsolutePath(),
				outputName
		});

		// print error output
		InputStreamReader errStream = new InputStreamReader(proc.getErrorStream());
		BufferedReader br = new BufferedReader(errStream);
		String line = null;
		boolean erred = false;
		while ((line = br.readLine()) != null) {
			System.err.println(line);
			erred = true;
		}
		br.close();

		proc.waitFor();
		if (erred || proc.exitValue() != 0) {
			throw new RuntimeException("Failed to create executable");
		} else {
			System.out.println("Created executable: " + outputName);
		}

		if (printInfo) {
			System.out.println("Creating executable took " + secondsSince(time) + " seconds");
		}
	}

	/** Returns the number of seconds since the given time. */
	private double secondsSince(long time) {
		return (System.currentTimeMillis() - time) / 1000.0;
	}

	/** Compiles the project and creates a static library. */
	private void createLibrary(String outputName, ArrayList<EssprogTarget> esspTargets, ArrayList<CTarget> cTargets, File binDir, long lastCompileTime) throws IOException {
		ZipOutputStream lib = new ZipOutputStream(new FileOutputStream(new File(binDir, outputName + LIBRARY_EXT)));

		// map essprog files
		long time = System.currentTimeMillis();
		for (EssprogTarget target : esspTargets) {
			mapper.map(target, binDir, lastCompileTime);
		}
		long esspMapTime = (System.currentTimeMillis() - time); // time taken to process Essprog files

		// write compiled files
		// compile then map all C targets (mapping C target requires LLVM IR code)
		if (!cTargets.isEmpty()) {
			time = System.currentTimeMillis();

			for (CTarget target : cTargets) {
				lib.putNextEntry(new ZipEntry(target.getPath() + LLVM_IR_EXT));
				target.compile(this, lib, binDir);
				mapper.map(target, binDir);
				lib.closeEntry();
			}

			if (printInfo) {
				System.out.println("Compiling C/C++ files took " + secondsSince(time) + " seconds.");
			}
		}

		// convert maps into objects
		time = System.currentTimeMillis();
		mapper.objectify();
		long esspObjectifyTime = System.currentTimeMillis() - time;

		// write manifest
		lib.putNextEntry(new ZipEntry(MANIFEST));
		mapper.writeManifest(lib);
		lib.closeEntry();

		// write compiled files
		for (EssprogTarget target : esspTargets) {
			lib.putNextEntry(new ZipEntry(target.getPath() + LLVM_IR_EXT));
			target.compile(this, lib, binDir);
			lib.closeEntry();
		}
		if (printInfo) {
			System.out.println("Compiling Essprog files took " + secondsSince(time - esspMapTime - esspObjectifyTime) + " seconds");
		}

		// finish
		lib.close();
		System.out.println("Created library: " + outputName);
	}
}