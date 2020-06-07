package com.ak.essprogc.symbol;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.ak.essprogc.EssprogCompiler;
import com.ak.essprogc.errors.AccessError;
import com.ak.essprogc.errors.Error;
import com.ak.essprogc.errors.InternalError;
import com.ak.essprogc.errors.MissingError;
import com.ak.essprogc.map.Mapper;
import com.ak.essprogc.objects.BaseContainer;
import com.ak.essprogc.objects.Container;
import com.ak.essprogc.objects.Indexed;
import com.ak.essprogc.objects.blocks.Filespace;
import com.ak.essprogc.objects.funcs.Callable;
import com.ak.essprogc.objects.funcs.essp.Function;
import com.ak.essprogc.objects.types.PrimitiveType;
import com.ak.essprogc.objects.types.Type;
import com.ak.essprogc.objects.types.UserType;
import com.ak.essprogc.objects.vars.TypeVariable;
import com.ak.essprogc.objects.vars.Variable;
import com.ak.essprogc.symbol.symbols.GlobalSymbol;
import com.ak.essprogc.symbol.symbols.LocalSymbol;
import com.ak.essprogc.symbol.symbols.local.LabelSymbol;
import com.ak.essprogc.targets.EssprogTarget;
import com.ak.essprogc.token.Tokenizer;
import com.ak.essprogc.token.tokens.OwnerToken;
import com.ak.essprogc.token.tokens.Token;

/**
 * Compiler for the second compilation pass. <br>
 * Handles tokens resulting from the first pass and produces LLVM IR output files.
 * 
 * @author Andrew Klinge
 */
public final class Symbolizer {
	public static final String GLOBAL_CTORS_PREFIX = "@GCTOR_";
	private ArrayList<String> globalCtors;
	private HashMap<String, GlobalSymbol> globals; // hashmap instead of arraylist for fast duplicate checking
	private ArrayList<LocalSymbol> locals;

	/** The type of the last operation-value on the stack. */
	public Type currentIDType;

	/** The Filespace for the current compilation. */
	private Filespace filespace;

	/** The current codespace. */
	private Container space;

	/** The current function. */
	private Function func;

	/** The path of the current file being compiled. */
	private String currentPath;

	/** The ID for the next local block. */
	private int nextBlockID;

	/** Current token being symbolized. */
	private Token currentToken;

	/** Current function's labels' owner tokens. */
	private HashMap<String, OwnerToken> labelOwners;

	/** Stores local objects for functions and local scopes. */
	private final HashMap<String, Indexed> localObjects = new HashMap<String, Indexed>();

	/** Fields in current classes that are final and have been initialized. Used to keep track of final field initilization in constructors. */
	public final ArrayList<TypeVariable> finalizedFields = new ArrayList<TypeVariable>();

	public final Mapper mapper;

	public Symbolizer(Mapper mapper) {
		this.mapper = mapper;
	}

	public void warn(String message) {
		System.err.println("Warning@" + currentPath + ":" + currentToken.line + "\n\t" + message);
	}

	public void put(Indexed obj) {
		if (obj.parent().isBase()) {
			if (obj.parent() != filespace && !obj.parent().parent().isBase()) {
				// obj is being added to a local base container (likely a class)
				// it has to be local because mapper has already taken care of global cases
				((BaseContainer) obj.parent()).put(obj);
			}
			filespace.put(obj);
		} else {
			localObjects.put(obj.getPath(), obj);
		}
	}

	public Variable getVar(String name, Container at) {
		return getVar(name, at, filespace);
	}

	private Variable getVar(String name, Container at, Filespace filespace) {
		String path = Filespace.toPath(Filespace.PREF_VAR, name, at);
		if (at.isBase()) {
			return (Variable) filespace.get(path);
		} else {
			return (Variable) localObjects.get(path);
		}
	}

	public Function getFunc(String name, Container at) {
		return (Function) getFunc(name, at, filespace);
	}

	private Indexed getFunc(String name, Container at, Filespace filespace) {
		String path = Filespace.toPath(Filespace.PREF_FUNC, name, at);
		if (at.isBase()) {
			return filespace.get(path);
		} else {
			return localObjects.get(path);
		}
	}

	public UserType getType(String name, Container at) {
		return getType(name, at, filespace);
	}

	private UserType getType(String name, Container at, Filespace filespace) {
		String path = Filespace.toPath(Filespace.PREF_TYPE, name, at);
		if (at.isBase()) {
			return (UserType) filespace.get(path);
		} else {
			return (UserType) localObjects.get(path);
		}
	}

	public void addLabel(String label, OwnerToken owner) {
		if (labelOwners == null) labelOwners = new HashMap<String, OwnerToken>();
		labelOwners.put(label, owner);
	}

	public OwnerToken getLabelOwner(String label) {
		if (labelOwners == null) return null;
		return labelOwners.get(label);
	}

	/** Removes all local objects (within a function and now inaccessible and unnecessary for compilation). */
	public void clearLocals() {
		localObjects.clear();
		labelOwners = null;
	}

	/**
	 * Writes out the compiled code using tokens generated by the Tokenizer.
	 */
	public File symbolize(EssprogTarget target, Tokenizer p1, Mapper mapper, OutputStream out, File binDir) {
		currentIDType = PrimitiveType.VOID;
		filespace = mapper.get(target.getPath());
		currentPath = target.getPath();
		space = filespace;
		nextBlockID = 0;
		globalCtors = new ArrayList<String>();
		globals = new HashMap<String, GlobalSymbol>(Math.max(16, p1.tfile.size() / 16));
		locals = new ArrayList<LocalSymbol>(p1.tfile.size());

		// copy all imported type definitions
		for (String alias : filespace.imports.keySet()) {
			mapper.copyImportedTypes(alias, filespace, this);
		}

		// symbolize tokens
		Iterator<Token> it = p1.tfile.tokens();

		while (it.hasNext()) {
			Token token = it.next();
			currentToken = token;

			try {
				token.symbolize(this);

			} catch (Error e) {
				e.setLocation(token.line, target);
				throw e;

			} catch (Exception e) {
				InternalError ie = new InternalError(e);
				ie.setLocation(token.line, target);
				throw ie;
			}
		}

		// begin writing
		boolean defaultOut = false;
		File result = null;
		if (out == null) { // use default output if none given (will only be non-null if writing to library archive)
			result = target.getOutputFile(binDir);
			try {
				out = new FileOutputStream(result);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			defaultOut = true;
		}

		EssprogOutputStream os = new EssprogOutputStream(out);

		// write header
		os.write("source_filename = \"" + target.getPath() + EssprogCompiler.CODE_EXT + "\"");

		// write internal symbols
		for (GlobalSymbol s : globals.values()) {
			s.write(os);
		}
		boolean needsGlobalCtor = !globalCtors.isEmpty();
		if (needsGlobalCtor) {
			final String globalCtor = GLOBAL_CTORS_PREFIX + target.getPath();
			os.write("@llvm.global_ctors = appending global [1 x { i32, void ()*, i8* }] [{ i32, void ()*, i8* } { i32 65535, void ()* " + globalCtor + ", i8* null }]");
		}
		SymbolContext sc = new SymbolContext();
		for (LocalSymbol s : locals) {
			s.write(os, sc);
		}
		if (needsGlobalCtor) {
			final String globalCtor = GLOBAL_CTORS_PREFIX + target.getPath();
			os.write("define internal void " + globalCtor + "() {");
			for (String ctor : globalCtors) {
				os.write("call void " + GLOBAL_CTORS_PREFIX + ctor + "()");
			}
			os.write("ret void");
			os.write("}");
		}

		// cleanup
		if (defaultOut) { // do not close unless the outputstream was created within this method
			try {
				os.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		globals = null;
		locals = null;
		globalCtors = null;
		return result;
	}

	public void addGlobalCtor(String id) {
		globalCtors.add(id);
	}

	public void setIDType(Type type) {
		currentIDType = type;
	}

	public ArrayList<LocalSymbol> getLocals() {
		return locals;
	}

	public void add(LocalSymbol ls) {
		locals.add(ls);
	}

	/** Returns the current filespace. */
	public Filespace getFilespace() {
		return filespace;
	}

	/** Returns the current codespace container. */
	public Container getSpace() {
		return space;
	}

	/** Sets the current codespace container. */
	public void setSpace(Container space) {
		this.space = space;
	}

	/** Sets the current codespace container function. */
	public void setSpace(Function space) {
		setSpace((Container) space);
		this.func = space;
	}

	/** Returns the current function if within one. Null otherwise. */
	public Function getFunction() {
		return func;
	}

	/** Returns the next unique LLVM IR block ID and increments for the next call. */
	public String nextBlockID() {
		return "" + (nextBlockID++);
	}

	public String getLastBlockID() {
		if (nextBlockID == 0) nextBlockID++;
		return "" + (nextBlockID - 1);
	}

	/** Returns the next block ID. */
	public String getNextBlockID() {
		return "" + nextBlockID;
	}

	public void add(GlobalSymbol gs) {
		globals.put(gs.toString(), gs);
	}

	public void add(LabelSymbol ls, OwnerToken owner) {
		if (func == null) throw new Error("Labels can only be created inside functions!");
		addLabel(ls.getID(), owner);
		add(ls);
	}

	/** Returns true if a GlobalSymbol with the given id already exists. */
	public boolean containsGlobal(String id) {
		return globals.containsKey(id);
	}

	/**
	 * Returns whether the parameters match the function's.
	 * 
	 * @param name is the full name (e.g. function#3) of the function.
	 * @param match is the parameter string (e.g. 3,5,4) to compare with.
	 */
	public boolean functionsMatch(String name, String match, Container space) {
		String[] params = name.substring(name.indexOf('#') + 1).split(",");
		String[] mparams = match.split(",");

		if (params.length != mparams.length) return false;
		if (params.length == 1 && params[0].equals("")) {
			return mparams[0].equals("");
		}
		if (mparams.length == 1 && mparams[0].equals("")) {
			return params[0].equals("");
		}

		for (int i = 0; i < params.length; i++) {
			Type t1 = findType(mparams[i], space);
			Type t2 = findType(params[i], space);

			if (!t1.isOf(t2)) return false;
		}

		return true;
	}

	/** Returns the UserType or Variable. */
	public Indexed findObj(String name, Container space) {
		return findObj(name, space, filespace);
	}

	/** Returns the UserType or Variable. */
	private Indexed findObj(String name, Container space, Filespace filespace) {
		String[] parts = name.split("\\.");

		if (parts[0].equals(EssprogCompiler.SUPER)) {
			return findObj(name.substring(EssprogCompiler.SUPER.length() + 2), space.parent(), filespace);
		}

		Container at = space;
		name = parts[0];
		boolean foundType = false; // whether at is an instance (of the actual type value) equals !foundType
		Variable var = null;

		// check imports
		Container con = mapper.getImported(name, filespace);
		if (con == null) {
			// search ascending scopes
			while (at != null) {
				var = getVar(name, at, filespace);

				if (var == null) {
					UserType type = getType(name, at, filespace);

					if (type != null) {
						at = type;
						foundType = true;
						break;
					}
				} else {
					if (parts.length > 1) {
						if (!var.type().isOf(PrimitiveType.OBJ)) throw new AccessError(name);
						at = (UserType) var.type();
					}
					break;
				}

				at = at.parent();
				if (at == null) return null;
			}
		} else {
			filespace = Filespace.of(con);
			at = con;

			if (con.isBase() && con != filespace) {
				foundType = true;
			}
		}

		// locate the rest of the objects (if necessary)
		if (parts.length - 1 > 0) {
			foundType = false;
		}
		for (int i = 1; i < parts.length; i++) {
			var = findVar(parts[i], at, filespace);

			if (var == null) {
				UserType type = getType(parts[i], at, filespace);

				if (type != null) {
					type.checkAccessibility(parts[i], at, !foundType);
					foundType = true;
					at = type;
				}
			} else {
				var.checkAccessibility(parts[i], at, !foundType);

				if (i != parts.length - 1) {
					if (!var.type().isOf(PrimitiveType.OBJ)) throw new AccessError(parts[i]);
					at = (UserType) var.type();
					foundType = false;
				}
			}
		}

		if (var == null && foundType) return (UserType) at;
		else return var;
	}

	/** A variable and its container variable. */
	public class VariableContext {
		public final Variable var;
		public final Variable container;

		public VariableContext(Variable var, Variable container) {
			this.var = var;
			this.container = container;
		}
	}

	/** Same as findVar() except returns the variable AND the string name of the container it was accessed through. */
	public VariableContext findVarContext(String name, Container space) {
		return findVarContext(name, space, filespace);
	}

	/** Same as findVar() except returns the variable AND the string name of the container it was accessed through. */
	private VariableContext findVarContext(String name, Container space, Filespace filespace) {
		String[] parts = name.split("\\.");

		if (parts[0].equals(EssprogCompiler.SUPER)) {
			return findVarContext(name.substring(EssprogCompiler.SUPER.length() + 1), space.parent(), filespace);
		}

		Container at = space;
		name = parts[0];
		Variable lastVar = null;
		Variable var = null;
		boolean isInstance = false;

		// check imports
		Container con = mapper.getImported(name, filespace);
		if (con == null) {
			// search ascending scopes
			while (at != null) {
				var = getVar(name, at, filespace);

				if (var == null) {
					UserType type = getType(name, at, filespace);

					if (type != null) {
						if (at != type.parent()) warn("Class definitions are static and should be accessed as such!");
						at = type;
						isInstance = false;
						break;
					}
				} else {
					if (parts.length > 1) {
						if (!var.type().isOf(PrimitiveType.OBJ)) throw new AccessError(name);
						at = (UserType) var.type();
						isInstance = true;
					}
					break;
				}

				at = at.parent();
				if (at == null) {
					return null;
				}
			}
		} else {
			filespace = Filespace.of(con);
			at = con;
		}

		// locate the rest of the variables (if necessary)
		for (int i = 1; i < parts.length; i++) {
			var = findVar(parts[i], at, filespace);

			if (var == null) {
				UserType type = getType(parts[i], at, filespace);

				if (type != null) {
					if (isInstance) warn("Class definitions are static and should be accessed as such!");
					type.checkAccessibility(parts[i], at, isInstance);
					at = type;
					isInstance = false;
				}
			} else {
				var.checkAccessibility(parts[i], at, isInstance);

				if (i != parts.length - 1) {
					if (!var.type().isOf(PrimitiveType.OBJ)) throw new AccessError(parts[i]);
					at = (UserType) var.type();
					isInstance = true;
				}
			}

			lastVar = var;
		}
		return new VariableContext(var, lastVar);
	}

	public Variable findVar(String name, Container space) {
		return findVar(name, space, filespace);
	}

	private Variable findVar(String name, Container space, Filespace filespace) {
		String[] parts = name.split("\\.");

		if (parts[0].equals(EssprogCompiler.SUPER)) {
			return findVar(name.substring(EssprogCompiler.SUPER.length() + 1), space.parent(), filespace);
		}

		Container at = space;
		name = parts[0];
		Variable var = null;
		boolean isInstance = false;

		// check imports
		Container con = mapper.getImported(name, filespace);
		if (con == null) {
			// search ascending scopes
			while (at != null) {
				var = getVar(name, at, filespace);

				if (var == null) {
					UserType type = getType(name, at, filespace);

					if (type != null) {
						if (isInstance) warn("Class definitions are static and should be accessed as such!");
						at = type;
						isInstance = false;
						break;
					}
				} else {
					if (parts.length > 1) {
						if (!var.type().isOf(PrimitiveType.OBJ)) throw new AccessError(name);
						at = (UserType) var.type();
						isInstance = true;
					}
					break;
				}

				at = at.parent();
				if (at == null) return null;
			}
		} else {
			filespace = Filespace.of(con);
			at = con;
		}

		// locate the rest of the variables (if necessary)
		for (int i = 1; i < parts.length; i++) {
			var = findVar(parts[i], at, filespace);

			if (var == null) {
				UserType type = getType(parts[i], at, filespace);

				if (type != null) {
					if (isInstance) warn("Class definitions are static and should be accessed as such!");
					type.checkAccessibility(parts[i], at, isInstance);
					at = type;
					isInstance = false;
				}
			} else {
				var.checkAccessibility(parts[i], at, isInstance);

				if (i != parts.length - 1) {
					if (!var.type().isOf(PrimitiveType.OBJ)) throw new AccessError(parts[i]);
					at = (UserType) var.type();
					isInstance = true;
				}
			}
		}
		return var;
	}

	/**
	 * Attempts to locate and return the function.
	 * 
	 * @param name - should be the full string representation of the function, e.g. "var.x.myFunc#string,bool,num"
	 * @param space - the space to search for the function in
	 * @return the function if it is found, otherwise null
	 */
	public Callable findFunc(String name, Container space) {
		return findFunc(name, space, 0, filespace, null).func;
	}

	/** Finds the function and returns information about the context it is being accessed from. */
	public CallContext findCallContext(String name, Container space) {
		return findFunc(name, space, 0, filespace, null);
	}

	/**
	 * A function and contextual information about where it is being called from.
	 * 
	 * @author Andrew Klinge
	 */
	public final class CallContext {
		/** The function being called. */
		public final Callable func;

		/** The container where the function call is taking place. */
		public final Container caller;

		/** The variable the caller is stored in. null if not stored in a variable. */
		public final Variable callerVar;

		public CallContext(Callable func, Container caller, Variable callerVar) {
			this.callerVar = callerVar;
			this.func = func;
			this.caller = caller;
		}

		/** Returns whether the call was made from an object instance. */
		public boolean fromInstance() {
			return callerVar != null;
		}
	}

	/**
	 * Attempts to locate and return the function ONLY in the given space (not in any of its parents).
	 * 
	 * @param name - should be the full string representation of the function, e.g. "var.x.myFunc#string,bool,num"
	 * @param space - the space to search for the function in
	 * @return the function if it is found, otherwise null
	 */
	public Callable findFuncImm(String name, Container space) {
		return findFunc(name, space, 1, filespace, null).func;
	}

	/** Recursive findFunc method. */
	private CallContext findFunc(String name, Container space, int recursions, Filespace filespace, Variable instance) {
		final boolean isInstance = instance != null;
		final int dot = name.indexOf('.');
		
		if (dot == -1) {
			Container at = space;

			// search for exact match
			while (at != null) {
				Indexed func = getFunc(name, at, filespace);

				if (func != null) {
					func.checkAccessibility(name, at, isInstance);
					return new CallContext((Callable) func, at, instance);
				}

				if (recursions == 0) at = at.parent();
				else break;
			}

			// search for general match
			int index = name.indexOf('#');
			String lookfor = name.substring(0, index);
			String params = name.substring(index + 1);
			at = space;

			while (at != null) {
				Function result = (Function) filespace.returnFirst(at, (String ename, Indexed obj, Container con) -> {
					if (ename.charAt(0) == Filespace.PREF_FUNC && ename.substring(0, ename.indexOf('#')).equals(lookfor)) {
						Function f = (Function) obj;

						if (functionsMatch(f.name(), params, con)) {
							f.checkAccessibility(name, con, isInstance);
							return f;
						}
					}
					return null;
				});
				if (result != null) return new CallContext(result, at, null);
				if (recursions == 0) at = at.parent();
				else break;
			}

			// search for Essprog stdlib function
			// prevent infinite recursion searching for stdlib func
			if (recursions == 0) {
				if (space == EssprogCompiler.SYSFUNCS_FS)
					return new CallContext(null, EssprogCompiler.SYSFUNCS_FS, null);

				return findFunc(name, EssprogCompiler.SYSFUNCS_FS, recursions + 1, EssprogCompiler.SYSFUNCS_FS, null);
			} else {
				return new CallContext(null, at, instance);
			}
		} else {
			String search = name.substring(0, dot);

			if (search.equals(EssprogCompiler.SUPER)) {
				return findFunc(name.substring(dot + 1), space.parent(), recursions + 1, filespace, null);
			}

			// check for imported spaces
			if (recursions == 0) {
				Container con = mapper.getImported(search, filespace);
				if (con != null) {
					return findFunc(name.substring(dot + 1), con, recursions + 1, Filespace.of(con), null);
				}
			}

			// check objects
			Variable var = findVar(search, space, filespace);
			if (var == null) {
				UserType type = (UserType) findType(search, space);

				if (type == null) throw new MissingError(search);
				else {
					return findFunc(name.substring(dot + 1), type, recursions + 1, Filespace.of(type), null);
				}
			} else {
				return findFunc(name.substring(dot + 1), (UserType) var.type(), recursions + 1, filespace, var);
			}
		}
	}

	/** Locates the type in the current space and filespace. */
	public Type findType(String name) {
		return mapper.findType(name, space, filespace);
	}

	/** Locates the type in the current filespace. */
	public Type findType(String name, Container space) {
		return mapper.findType(name, space, filespace);
	}
}