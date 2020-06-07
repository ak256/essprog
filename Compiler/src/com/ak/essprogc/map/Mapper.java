package com.ak.essprogc.map;

import static com.ak.essprogc.EssprogCompiler.getExitIndex;
import static com.ak.essprogc.EssprogCompiler.getLastNonStringIndex;
import static com.ak.essprogc.EssprogCompiler.getNonStringIndex;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.zip.ZipFile;

import com.ak.essprogc.EssprogCompiler;
import com.ak.essprogc.EssprogCompiler.Visibility;
import com.ak.essprogc.Scanner;
import com.ak.essprogc.errors.AccessError;
import com.ak.essprogc.errors.DuplicateError;
import com.ak.essprogc.errors.Error;
import com.ak.essprogc.errors.ExitError;
import com.ak.essprogc.errors.InternalError;
import com.ak.essprogc.errors.MissingError;
import com.ak.essprogc.objects.BaseContainer;
import com.ak.essprogc.objects.Container;
import com.ak.essprogc.objects.Indexed;
import com.ak.essprogc.objects.blocks.Filespace;
import com.ak.essprogc.objects.funcs.Callable;
import com.ak.essprogc.objects.funcs.c.CFunction;
import com.ak.essprogc.objects.funcs.essp.Function;
import com.ak.essprogc.objects.types.EsspArrayType;
import com.ak.essprogc.objects.types.FuncType;
import com.ak.essprogc.objects.types.Interface;
import com.ak.essprogc.objects.types.PrimitiveType;
import com.ak.essprogc.objects.types.TempType;
import com.ak.essprogc.objects.types.TransType;
import com.ak.essprogc.objects.types.Type;
import com.ak.essprogc.objects.types.UserType;
import com.ak.essprogc.objects.types.VaArgType;
import com.ak.essprogc.objects.vars.GlobalVariable;
import com.ak.essprogc.objects.vars.LocalVariable;
import com.ak.essprogc.objects.vars.TypeVariable;
import com.ak.essprogc.objects.vars.Variable;
import com.ak.essprogc.symbol.EssprogOutputStream;
import com.ak.essprogc.symbol.Symbolizer;
import com.ak.essprogc.targets.CTarget;
import com.ak.essprogc.targets.EssprogTarget;
import com.ak.essprogc.token.tokens.Token;
import com.ak.essprogc.token.tokens.def.DefToken;
import com.ak.essprogc.token.tokens.def.c.DefCFuncToken;

/**
 * Responsible for mapping out the objects of every code file to allow for linking. Also handles imports and filetype declarations.
 * 
 * @author Andrew Klinge
 */
public final class Mapper extends Scanner {
	private static final char MANIFEST_FILESPACE = (char) 29; // group separator
	private static final char SEPARATOR = (char) 31; // unit separator

	/** The ID for the next global object. */
	public int nextGlobalID;

	/** Maps for all code files before objectification, stored by the path of the file they are associated with. */
	private ArrayList<Map> maps = new ArrayList<Map>();

	/** Filespaces for all code files stored by filepath. */
	private HashMap<String, Filespace> filespaces = new HashMap<String, Filespace>();

	/** Whether the current file has had content yet (set to true when a non-whitespace/non-comment line is encountered). */
	private boolean hasContent;

	/** Current map file. */
	private MFile mfile;

	/** The declared path of the main file. */
	private String main;

	/** The program entry point. */
	private Function mainFunction;

	/** Essprog standard library string class. */
	private UserType std_string;

	/** Resets all stored map data. */
	public void clean() {
		nextGlobalID = 0;
		maps = new ArrayList<Map>();
		filespaces = new HashMap<String, Filespace>();
	}

	public UserType std_string() {
		return std_string;
	}

	/** Loads the library (compiled code archive) so it can be referenced in code. */
	public void loadLibrary(ZipFile lib, Filespace... boundFilespaces) {
		ArrayList<Filespace> bindingsList = new ArrayList<Filespace>(boundFilespaces.length);
		for (Filespace bf : boundFilespaces) {
			bindingsList.add(bf);
		}

		// read manifest
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(lib.getInputStream(lib.getEntry(EssprogCompiler.MANIFEST))));
			String line = null;

			Filespace fs = null; // current filespace being loaded
			HashMap<String, Container> containers = null; // <id, container>

			while ((line = reader.readLine()) != null) {
				if (line.charAt(0) == MANIFEST_FILESPACE) { // new filespace entry
					FileType ft = FileType.values()[Integer.parseInt(line.substring(1, 2))];
					String path = line.substring(2);

					// check for bound filespaces
					for (int i = 0; i < bindingsList.size(); i++) {
						Filespace bf = bindingsList.get(i);
						
						if (bf.getDeclaredPath().equals(path)) {
							fs = bf;
							bindingsList.remove(i);
							break;
						}
					}

					if (fs == null) { // no bound filespace found
						fs = new Filespace(ft, path);
					}

					filespaces.put(path, fs);
					containers = new HashMap<String, Container>();
				} else { // object entry
					// don't have to worry about consuming time to add objects in correct order (i.e. containers before their contained objects)
					// because this is done during library creation
					String[] data = line.split(Character.toString(SEPARATOR));

					DefType objType = DefType.values()[Integer.parseInt(data[0])];
					String id = data[1];
					String name = data[2];
					Visibility visibility = Visibility.values()[Integer.parseInt(data[3])];
					boolean isStatic = data[4].equals("1");
					BaseContainer parent = data[5].equals("0") ? fs : (BaseContainer) containers.get(data[5]);

					// create object
					Indexed obj = null;
					switch (objType) {
						case TYPE_VAR: {
							boolean isFinal = data[6].equals("1");
							String typeName = data[7];
							int index = Integer.parseInt(data[8]);

							TypeVariable var = new TypeVariable(name, isFinal, visibility, new TempType(typeName, parent), (UserType) parent, null);
							var.index = index;
							obj = var;
							break;
						}

						case GLOBAL_VAR: {
							String typeName = data[6];
							obj = new GlobalVariable(name, visibility, new TempType(typeName, parent), parent, fs, this, null);
							break;
						}

						case LOCAL_VAR: {
							boolean isFinal = data[6].equals("1");
							String typeName = data[7];
							obj = new LocalVariable(name, isFinal, visibility, new TempType(typeName, parent), parent, id);
							break;
						}

						case FUNC: {
							String typeName = data[6];

							int ind = name.indexOf('#');
							String realName = name.substring(0, ind);
							String paramsString = name.substring(ind + 1);
							String[] paramStrings = paramsString.split(",");
							Type[] params = new Type[paramsString.isEmpty() ? 0 : paramStrings.length];
							for (int i = 0; i < params.length; i++) {
								params[i] = new TempType(paramStrings[i], parent);
							}

							obj = new Function(isStatic, visibility, new TempType(typeName, parent), realName, params, parent, fs, this, id);
							break;
						}

						case CFUNC: {
							final int START_INDEX = 6;
							String typeName = data[START_INDEX];
							String realName = name.substring(0, name.indexOf('#'));
							final int length = data.length - (START_INDEX + 1);
							ArrayList<TransType> types = new ArrayList<TransType>(length);
							// parse params
							for (int i = 0; i < length; i++) {
								String[] triple = data[i + START_INDEX + 1].split(" ");
								types.add(new TransType(triple[0], triple[1], triple[2].equals("1")));
							}

							obj = new CFunction(isStatic, visibility, new TempType(typeName, parent), realName, id, types, parent, fs, this);
							break;
						}

						case INTERFACE: {
							final int START_INDEX = 6;

							Type[] parents = new Type[data.length - START_INDEX];
							for (int i = 0; i < parents.length; i++) {
								parents[i] = new TempType(data[i + START_INDEX], parent);
							}

							Interface intr = new Interface(visibility, name, parents, parent, id);
							obj = intr;
							containers.put(id, intr);
							break;
						}

						case TYPE: {
							final int START_INDEX = 6;

							Type[] parents = new Type[data.length - START_INDEX];
							for (int i = 0; i < parents.length; i++) {
								parents[i] = new TempType(data[i + START_INDEX], parent);
							}

							UserType type = new UserType(visibility, name, parents, parent, id);
							obj = type;
							containers.put(id, type);
							break;
						}

						default:
							throw new Error("Invalid object type");
					}

					// add object
					fs.put(obj);
					if (parent != fs) {
						parent.put(obj);
					}
				}
			}
			reader.close();
		} catch (Exception e) {
			System.err.println("Error loading library \"" + lib.getName() + "\" - possibly corrupt or incorrect format");
			e.printStackTrace();
		}
	}

	/** Sorts the array. All objects in the array must be from the given filespace. */
	private void treeSort(Indexed[] arr, Filespace fs) {
		ArrayList<Indexed> sorted = new ArrayList<Indexed>(); // result
		HashMap<Indexed, Integer> topIndices = new HashMap<Indexed, Integer>(); // indices of tops in sorted
		HashMap<String, Indexed> top = new HashMap<String, Indexed>(); // objects with filespace as parent. <id, object>

		// find top objects
		for (int i = 0; i < arr.length; i++) {
			if (arr[i].parent() != fs) continue;

			top.put(arr[i].id(), arr[i]);
			topIndices.put(arr[i], sorted.size());
			sorted.add(arr[i]);
		}

		// add children, level by level (moving from highest to lowest)
		while (sorted.size() != arr.length) {
			HashMap<Indexed, Integer> newTopIndices = new HashMap<Indexed, Integer>();

			// check if is child of any in top. if so, add under their top
			for (int i = 0; i < arr.length; i++) {
				if (arr[i].parent() == fs) continue;

				// check to allow for safe cast from getParent() Container to Indexed
				Indexed parent = top.get(((Indexed) arr[i].parent()).id());
				if (parent == null) continue;

				int index = topIndices.get(parent) + 1;
				newTopIndices.put(arr[i], index);
				sorted.add(index, arr[i]);
			}

			// update top and topIndices to new level
			topIndices.clear();
			top.clear();
			Iterator<Entry<Indexed, Integer>> it = newTopIndices.entrySet().iterator();
			while (it.hasNext()) {
				Entry<Indexed, Integer> entry = it.next();
				top.put(entry.getKey().id(), entry.getKey());
				topIndices.put(entry.getKey(), entry.getValue());
			}
		}

		// overwrite original array
		for (int i = 0; i < arr.length; i++) {
			arr[i] = sorted.get(i);
		}
	}

	/** Gathers and writes static-library manifest data to the output stream. The manifest contains name and type information for accessible objects. */
	public void writeManifest(OutputStream os) {
		EssprogOutputStream out = new EssprogOutputStream(os);

		for (Entry<String, Filespace> entry : filespaces.entrySet()) {
			Filespace fs = entry.getValue();
			int fileTypeID = fs.fileType == FileType.STANDARD ? 0 : 1;
			out.write(MANIFEST_FILESPACE + "" + fileTypeID + entry.getKey());

			// add objects in correct order to save time during library loading
			// i.e. list containers before their contained objects
			// must first sort them
			Indexed[] sorted = fs.getAll().toArray(new Indexed[fs.getAll().size()]);
			treeSort(sorted, fs);

			// write objects
			for (Indexed obj : sorted) {
				int visID = obj.visibility().ordinal();
				int staticID = obj.isStatic() ? 1 : 0;
				int objTypeID = obj.objectType().ordinal();

				StringBuilder data = new StringBuilder();
				data.append(objTypeID);
				data.append(SEPARATOR);
				data.append(obj.id());
				data.append(SEPARATOR);
				data.append(obj.name());
				data.append(SEPARATOR);
				data.append(visID);
				data.append(SEPARATOR);
				data.append(staticID);
				data.append(SEPARATOR);
				data.append(obj.parent() == fs ? 0 : ((Indexed) obj.parent()).id());
				data.append(SEPARATOR);

				switch (obj.objectType()) {
					case TYPE_VAR: {
						TypeVariable var = (TypeVariable) obj;
						data.append(var.isFinal() ? 1 : 0);
						data.append(SEPARATOR);
						data.append(var.type().toString());
						data.append(SEPARATOR);
						data.append(var.index);
						break;
					}

					case GLOBAL_VAR: {
						GlobalVariable var = (GlobalVariable) obj;
						data.append(var.type().toString());
						break;
					}

					case LOCAL_VAR: {
						LocalVariable var = (LocalVariable) obj;
						data.append(var.isFinal() ? 1 : 0);
						data.append(SEPARATOR);
						data.append(var.type().toString());
						break;
					}

					case FUNC: {
						Function func = (Function) obj;
						data.append(func.getType().toString());
						data.append(SEPARATOR);
						break;
					}

					case CFUNC: {
						CFunction func = (CFunction) obj;
						data.append(func.getType().toString());
						data.append(SEPARATOR);

						// write parameters
						Iterator<TransType> it = func.transParameters();
						while (it.hasNext()) {
							TransType t = it.next();
							data.append(t.source);
							data.append(' ');
							data.append(t.name);
							data.append(' ');
							data.append(t.isPointer ? "1" : "0");

							if (it.hasNext()) {
								data.append(SEPARATOR);
							}
						}
						break;
					}

					case INTERFACE:
					case TYPE: {
						UserType type = (UserType) obj;

						for (int i = 0; i < type.getParents().length; i++) {
							data.append(type.getParents()[i].toString());

							if (i != type.getParents().length - 1) {
								data.append(SEPARATOR);
							}
						}
						break;
					}

					default:
						throw new Error("Invalid object type");
				}

				out.write(data.toString());
			}
		}
	}

	/**
	 * Must be called AFTER all EssprogTargets have been mapped.
	 */
	public void map(CTarget target, File binDir) {
		reset();

		BufferedReader reader = null;
		Map map = new Map();
		map.path = target.getPath();
		mfile = new MFile(map);

		try {
			HashMap<String, DefCFuncToken> funcs = new HashMap<String, DefCFuncToken>(); // all parsed functions stored under their name
			
			{ // 1. read raw c files and parse function definitions
				// - TODO struct/union related modifiers (visibility, virtual, etc.)
				reader = new BufferedReader(new FileReader(target.getFile()));
				Token.currentLine = 1;
				char c = (char) -1; // current character
				char lc = c; // last character
				int level = 0; // block level (each '{' increments this, each '}' decrements this)
				StringBuilder mem = new StringBuilder(); // read character storage
				boolean commented = false; // whether in a single line comment
				boolean string = false; // whether in string literal
				while ((c = (char) reader.read()) != (char) -1) {
					if (multiComment) {
						if (lc == '*' && c == '/') {
							multiComment = false;
						}
					} else if (commented) {
						if (c == '\n') {
							commented = false;
						}
					} else if (string) {
						if (c == '"' && lc != '\\') {
							string = false;
						}
					} else {
						if (c == '#' || (lc == '/' && c == '/')) {
							commented = true;
						} else if (c == '{') {
							level++;
							if (mem.length() > 0) {
								final String line = mem.toString();
								final int open = getNonStringIndex(line, '(');

								if (open != -1) { // all functions have a parameter body
									String[] words = line.substring(0, open).split(" ");

									if (words.length >= 2) { // all functions have 2 words: a name and a type. other statements only have a single keyword
										String name = words[words.length - 1];
										funcs.put(name, new DefCFuncToken(Visibility.PUBLIC, name));
									}
								}
							}
						} else if (c == '}') {
							level--;
						} else if (c == ';') {
							mem.setLength(0);
						} else if (c == '"') {
							string = true;
						} else {
							if (level == 0) {
								// only care about mapping filespace functions (so only record when in filespace)
								mem.append(c);
							}
						}
					}
				}
				reader.close();
			}

			{ // 2. read their corresponding compiled .ll files and add LLVM IR information to the func defs
				// Information that is parsed from this step:
				// - function LLVM id (mangled C name)
				// - function LLVM type
				// - function LLVM parameter types
				reader = new BufferedReader(new FileReader(target.getOutputFile(binDir)));
				Token.currentLine = 1;
				String line = null;
				while ((line = reader.readLine()) != null) {
					define: if (line.startsWith("define ")) { // function definition
						line = line.substring(7);

						final int idIndex = getNonStringIndex(line, " @"); // index of function identifier
						final int open = getNonStringIndex(line, '(', idIndex); // index of open parenthesis
						final int close = getExitIndex(line, open); // index of closing parenthesis
						
						final String name = line.substring(idIndex + 2, open); // function identifier
						final String type = line.substring(getLastNonStringIndex(line, ' ', idIndex - 1) + 1, idIndex); // function return type
						final String[] paramTypes = EssprogCompiler.parseList(line.substring(open + 1, close));
						TransType esspType = null; // (string: type name, boolean: whether esspType was originally a pointer type)
						ArrayList<TransType> parameters = new ArrayList<TransType>();

						// ensure this is a mappable function (already in funcs list, defined within the actual c/c++ file). otherwise ignore this func
						DefCFuncToken func = funcs.get(name);
						if (func == null) {
							break define;
						}

						// check if there is supposed to only be a single main function in the Essprog code
						// clang compiler throws error when C files also have a main func
						if (main != null && name.equals(EssprogCompiler.MAIN_FUNC)) {
							throw new Error("Conflicting main function found in \"" + target.getPath() + "\". Change its name or remove it.");
						}

						// parse parameter types
						for (int i = 0; i < paramTypes.length; i++) {
							paramTypes[i] = paramTypes[i].trim();
							String trueType = reverseTypeName(paramTypes[i]);

							if (trueType == null) {
								if (i == 0) { // first parameter with "returned" modifier
									// TODO is this used at all?
								} else {
									throw new Error("Invalid type: " + paramTypes[i]);
								}
							} else if (trueType.charAt(0) == '#') {
								char specifier = trueType.charAt(1);

								switch (specifier) {
									case 'r': // LLVM sret modifier
										if (esspType == null) {
											if (trueType.charAt(2) == '#' && trueType.charAt(3) == 'p') {
												esspType = new TransType(paramTypes[i], replaceReservedChars(trueType.substring(4)), true);
											} else {
												esspType = new TransType(paramTypes[i], replaceReservedChars(trueType.substring(2)), false);
											}
										} else {
											throw new Error("Multiple parameters with modifier \"sret\"");
										}
										break;

									case 'p': // pointer modifier
										parameters.add(new TransType(paramTypes[i], replaceReservedChars(trueType.substring(2)), true));
										break;
								}
							} else {
								if (trueType.equals(EssprogCompiler.VA_ARG)) {
									TransType lastParam = (TransType) parameters.get(parameters.size() - 1);
									trueType = lastParam.toString() + EssprogCompiler.VA_ARG;
									parameters.set(parameters.size() - 1, new TransType(lastParam.source + ", ...", trueType, ((TransType) lastParam).isPointer));
								} else {
									parameters.add(new TransType(paramTypes[i], replaceReservedChars(trueType), false));
								}
							}
						}

						// parse type
						if (esspType == null) {
							String trueName = reverseTypeName(type);
							boolean isPointer = false;
							if (trueName.startsWith("#p")) {
								trueName = trueName.substring(2);
								isPointer = true;
							}
							trueName = replaceReservedChars(trueName);
							esspType = new TransType(type, trueName, isPointer);
						}

						// modify func and create it
						func.setID("@" + name);
						func.setParams(parameters);
						func.setType(esspType);

						mfile.add(func);
						mfile.exit(); // need to close function definition block
					}
					/*else if (line.contains(" = type {")) { // TODO C type definition
					String name = replaceReservedChars(reverseTypeName(line.substring(0, getNonStringIndex(line, ' '))));
					mfile.add(new DefTypeToken(Visibility.PUBLIC, name, new String[] {}));
					mfile.exitType(); // close type definition block
					}*/
					Token.currentLine++;
				}
			}
		} catch (Error e) {
			e.setLocation(Token.currentLine, target);
			throw e;
		} catch (Exception e) {
			System.err.println("Error in mapping C file \"" + target.getFile().getPath() + "\"");
			InternalError ie = new InternalError(e);
			ie.setLocation(Token.currentLine, target);
			throw ie;
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		maps.add(map);
		mfile = null;
	}

	/** Replaces reserved characters (i.e. '$', ':', '?') in the given string with underscores ('_'). */
	private String replaceReservedChars(String string) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < string.length(); i++) {
			char c = string.charAt(i);
			if (c == ' ' || c == '.' || EssprogCompiler.isReserved(c)) {
				result.append('_');
			} else {
				result.append(c);
			}
		}
		return result.toString();
	}

	/**
	 * Turns the LLVM IR assembly type name back into its corresponding Essprog type name.
	 * 
	 * @param asmType - the LLVM IR type string
	 */
	private String reverseTypeName(String asmType) {
		if (asmType.endsWith(" returned")) {
			return null; // ignore implicit return type (the type is for a parameter)
		}
		if (asmType.startsWith("zeroext ")) { // ignore zero-extend
			return reverseTypeName(asmType.substring("zeroext ".length()));
		}
		if (asmType.endsWith(" zeroext")) { // ignore
			return reverseTypeName(asmType.substring(0, asmType.length() - " zeroext".length()));
		}
		if (asmType.endsWith(" noalias")) { // ignore keyword
			return reverseTypeName(asmType.substring(0, asmType.length() - " noalias".length()));
		}
		if (asmType.endsWith(" sret")) { // specify that this type is returned type (parameter type)
			return "#r" + reverseTypeName(asmType.substring(0, asmType.length() - " sret".length()));
		}
		if (asmType.contains(" dereferenceable(") && asmType.charAt(asmType.length() - 1) == ')') { // ignore dereferenceable(*)
			return reverseTypeName(asmType.substring(0, asmType.indexOf(" dereferenceable(")));
		}
		if (asmType.charAt(asmType.length() - 1) == '*') { // pointer type
			if (asmType.charAt(0) == '%') { // user defined type pointer
				// remove the rest of the pointer symbols (e.g. int***, ignore all except)
				int length = asmType.length();
				while (asmType.charAt(length - 1) == '*') {
					length--;
				}
				return "#p" + reverseTypeName(asmType.substring(0, length));
			} else { // primitive type pointer
				return "#p" + PrimitiveType.C_POINTER.toString();
			}
		}
		if (asmType.charAt(0) == '%') { // user defined type
			asmType = asmType.substring(1);

			if (asmType.startsWith("\"")) {
				String name = asmType.substring(1, asmType.lastIndexOf('"'));
				switch (name) {
					default:
						if (name.startsWith("class.")) {
							return name.substring("class.".length()).replace(':', '_');
						}
						if (name.startsWith("union.")) {
							return name.substring("union.".length()).replace(':', '_');
						}
						if (name.startsWith("struct.")) {
							return name.substring("struct.".length()).replace(':', '_');
						}
						return name;
				}
			} else if (asmType.startsWith("class.")) {
				return asmType.substring("class.".length()).replace(':', '_');
			} else if (asmType.startsWith("union.")) {
				return asmType.substring("union.".length()).replace(':', '_');
			} else if (asmType.startsWith("struct.")) {
				return asmType.substring("struct.".length()).replace(':', '_');
			} else {
				return asmType;
			}
		}
		switch (asmType) {
			case "void":
				return PrimitiveType.VOID.toString();

			case "i64":
				return PrimitiveType.INT64.toString();

			case "i32":
				return PrimitiveType.INT32.toString();

			case "i16":
				return PrimitiveType.INT16.toString();

			case "i8":
				return PrimitiveType.INT8.toString();

			case "i1":
				return PrimitiveType.BOOL.toString();

			case "float":
				return PrimitiveType.FLOAT.toString();

			case "double":
				return PrimitiveType.DOUBLE.toString();

			case EssprogCompiler.VA_ARG:
				return EssprogCompiler.VA_ARG;
		}
		throw new Error("Unknown LLVM IR type: " + asmType);
	}

	/** Specifies the main Essprog code file's declared path. This file contains the program entry point. */
	public void setMain(String main) {
		this.main = main;
	}

	public void map(EssprogTarget target, File binDir, long lastCompileTime) {
		reset();
		hasContent = false;

		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(target.getFile()));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			return;
		}
		String line = "";
		Token.currentLine = 1;
		Map map = new Map();
		mfile = new MFile(map);

		try {
			while ((line = reader.readLine()) != null) {
				scan(line, target);
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

		// determine whether the file should compile or not (required mapper to parse declared path for file)
		target.preventRecompile(binDir, lastCompileTime);

		maps.add(map);
		mfile = null;
	}

	public void objectify() {
		// convert maps to filespaces and remove maps from memory
		while (!maps.isEmpty()) {
			Map map = maps.get(maps.size() - 1);
			filespaces.put(map.path, map.convert(this));
			maps.remove(maps.size() - 1);
		}
		maps = null;

		// link types in objects
		for (Filespace fs : filespaces.values()) {
			fs.resolveTempTypes(this);
		}

		// check for and identify correct main function in main file
		if (main != null) {
			final Filespace mainSpace = filespaces.get(main);
			if (mainSpace == null) {
				throw new Error("Main file specified (" + main + ") does not exist!");
			}

			final String mainFuncPath1 = Filespace.PREF_FUNC + EssprogCompiler.MAIN_FUNC + Callable.getString(new Type[] {});
			final Function mainFunc1 = (Function) mainSpace.get(mainFuncPath1);

			final String mainFuncPath2 = Filespace.PREF_FUNC + EssprogCompiler.MAIN_FUNC + Callable.getString(new Type[] { new EsspArrayType(1, EssprogCompiler.STRING) });
			final Function mainFunc2 = (Function) mainSpace.get(mainFuncPath2);

			Error e = null;

			if (mainFunc1 == null && mainFunc2 == null) {
				e = new Error("Main function not found in the given main file! Should appear as: \"void main()\" or \"void main(string[] args)\"");
			} else if (mainFunc1 != null && mainFunc2 != null) {
				e = new Error("Multiple possible main functions found in the given main file! Should only be one");
			} else if (mainFunc1 != null) {
				mainFunction = mainFunc1;
			} else if (mainFunc2 != null) {
				mainFunction = mainFunc2;
			}

			if (e != null) {
				e.setLocation(0, main);
				throw e;
			}
		}
	}

	public Filespace get(String path) {
		return filespaces.get(path);
	}

	public Type findType(String name, Container space, Filespace fs) {
		String[] parts = name.split("\\.");

		if (parts.length == 1) {
			if (name.equals(PrimitiveType.VOID.toString())) return PrimitiveType.VOID;
			else if (name.equals(PrimitiveType.INT8.toString())) return PrimitiveType.INT8;
			else if (name.equals(PrimitiveType.INT16.toString())) return PrimitiveType.INT16;
			else if (name.equals(PrimitiveType.INT32.toString()) || name.equals(PrimitiveType.INT.toString())) return PrimitiveType.INT32;
			else if (name.equals(PrimitiveType.INT64.toString())) return PrimitiveType.INT64;
			else if (name.equals(PrimitiveType.FLOAT.toString())) return PrimitiveType.FLOAT;
			else if (name.equals(PrimitiveType.DOUBLE.toString())) return PrimitiveType.DOUBLE;
			else if (name.equals(EssprogCompiler.STRING.toString())) return EssprogCompiler.STRING;
			else if (name.equals(PrimitiveType.BOOL.toString())) return PrimitiveType.BOOL;
			else if (name.equals(PrimitiveType.OBJ.toString())) return PrimitiveType.OBJ;

			// check for variable argument type
			if (name.endsWith(EssprogCompiler.VA_ARG)) {
				return new VaArgType(findType(name.substring(0, name.length() - EssprogCompiler.VA_ARG.length()), space, fs));
			}

			// check for list types
			if (name.endsWith("[]")) {
				int count = 0;

				while (name.endsWith("[]")) {
					count++;

					name = name.substring(0, name.length() - 2);
				}
				return new EsspArrayType(count, findType(name, space, fs));
			}

			// parameterized type
			if (name.contains("(") && name.endsWith(")")) {
				int ind = name.indexOf('(');
				String nam = name.substring(0, ind);
				String pars = name.substring(ind + 1, name.length() - 1);
				String[] split = pars.split(",");
				Type[] parsarr = new Type[pars.isEmpty() ? 0 : split.length];

				for (int d = 0; d < parsarr.length; d++) {
					parsarr[d] = findType(split[d], space, fs);
				}

				return new FuncType(findType(nam, space, fs), parsarr);

			}

			// check imports
			Container con = getImported(name, fs);
			if (con != null) {
				return (UserType) con;
			}
		}

		// search for UserType
		if (space == null) throw new MissingError(name);
		Container at = space;

		if (parts[0].equals(EssprogCompiler.SUPER)) {
			at = at.parent();
			name = parts[1];
		} else {
			name = parts[0];
		}
		UserType result = null;

		// check imports
		Container con = getImported(name, fs);
		if (con != null) {
			at = con;
		}

		// search ascending scopes
		boolean isInstance = false;
		while (at != null) {
			result = (UserType) fs.get(Filespace.toPath(Filespace.PREF_TYPE, name, at));

			if (result == null) {
				Variable var = (Variable) fs.get(Filespace.toPath(Filespace.PREF_VAR, name, at));

				if (var != null) {
					if (!var.type().isOf(PrimitiveType.OBJ)) throw new AccessError(name);
					at = (UserType) var.type();
					isInstance = true;
					break;
				}
			} else {
				at = result;
				isInstance = false;
				break;
			}

			at = at.parent();
			if (at == null) throw new MissingError(name);
		}

		// locate the rest of the containers (if necessary)
		for (int i = 1; i < parts.length; i++) {
			result = (UserType) findType(parts[i], at, fs);

			if (result == null) {
				Variable var = (Variable) fs.get(Filespace.toPath(Filespace.PREF_VAR, parts[i], at));

				if (var == null) {
					throw new MissingError(parts[i]);

				} else {
					if (!var.type().isOf(PrimitiveType.OBJ)) throw new AccessError(parts[i]);
					var.checkAccessibility(parts[i], at, isInstance);
					at = (UserType) var.type();
					isInstance = true;
				}
			} else {
				result.checkAccessibility(result.name(), at, isInstance);
				isInstance = false;
				at = result;
			}
		}

		return result;
	}

	/** Finds the external space that was imported by the given alias in the given filespace. */
	public Container getImported(String alias, Filespace fs) {
		String path = fs.imports.get(alias);
		if (path == null) return null;

		Filespace importedFS = get(path);
		if (importedFS.fileType == FileType.ELEVATED) {
			return (UserType) importedFS.get(Filespace.PREF_TYPE + path.substring(path.lastIndexOf(EssprogCompiler.IMPORT_PATH_SEPARATOR) + 1));
		} else {
			return importedFS;
		}
	}

	/** Copies all type definitions from the imported space (given by alias) that was imported in the given filespace (fs) into it. */
	public void copyImportedTypes(String alias, Filespace fs, Symbolizer p2) {
		String path = fs.imports.get(alias);
		if (path == null) {
			throw new MissingError(alias);
		}

		Filespace importedFS = get(path);
		importedFS.copyTypeDefs(p2);
	}

	@Override
	protected void autoExit() {
		if (!mfile.tree().isRoot() && mfile.tree().type.isBase()) {
			mfile.exitType();
		} else {
			mfile.exit();
		}
		if (mfile.tree() == null) throw new ExitError();
	}

	@Override
	protected void rescan(String line) {}

	/** Scans a line of code and generates detailed information from it. */
	protected void scan(String line, EssprogTarget target) {
		// format
		line = format(line).trim();
		if (line.isEmpty()) return;

		// set hasContent to true, but hide this until next scan()
		boolean hasContent = this.hasContent;
		this.hasContent = true;

		// manually close block
		if (line.length() == 1 && line.charAt(0) == EssprogCompiler.BLOCK_EXIT) {
			autoExit();
			return;
		}

		// check for label
		if (line.charAt(line.length() - 1) == EssprogCompiler.GENERIC_DELIMITER) return;

		// check for single-keyword line
		switch (line) {
			case EssprogCompiler.DEFAULT:
				mfile.enter(ChainBlock.DEFAULT);
				return;

			case EssprogCompiler.BLOCK:
				mfile.enter(null);
				return;

			case EssprogCompiler.ELSE:
				mfile.enter(ChainBlock.ELSE);
				return;

			// handled by Tokenizer. need this here so interpretation of line stops correctly
			case EssprogCompiler.BREAK:
			case EssprogCompiler.RETURN:
			case EssprogCompiler.CONTINUE:
				return;
		}

		// check for label
		if (line.charAt(line.length() - 1) == EssprogCompiler.GENERIC_DELIMITER) {
			return; // handled by Tokenizer
		}

		// check for detailed blocks
		int spaceIndex = getNonStringIndex(line, " ");
		if (spaceIndex == -1) { // check for condition-less single-line block keywords
			int arrowIndex = getNonStringIndex(line, EssprogCompiler.SINGLE_LINE_DELIMITER);
			if (arrowIndex != -1) {
				String word = line.substring(0, arrowIndex);
				switch (word) {
					case EssprogCompiler.DEFAULT:
					case EssprogCompiler.ELSE:
						autoExit();
						return;
				}
			}
		} else { // check for commands and blocks
			String word = line.substring(0, spaceIndex);
			String info = line.substring(spaceIndex + 1);
			switch (word) {
				// declare
				case EssprogCompiler.DECLARE: {
					if (hasContent) throw new Error("Declares must be the first statement of the file!");

					String path = info;
					int br = path.indexOf(EssprogCompiler.GENERIC_DELIMITER);
					if (br != -1) {
						String ft = path.substring(br + 1);
						path = path.substring(0, br);
						switch (ft) {
							case EssprogCompiler.STRUCT:
							case EssprogCompiler.INTERFACE:
							case EssprogCompiler.CLASS:
								mfile.map.setFileType(FileType.ELEVATED);
								break;
							default:
								throw new Error("Unrecognized file type declaration \"" + ft + "\"");
						}
					}
					path = path.trim();
					target.setPath(path);
					mfile.map.path = path;
					return;
				}

				// import
				case EssprogCompiler.IMPORT: {
					int br = info.indexOf(EssprogCompiler.GENERIC_DELIMITER);
					if (br == -1) {
						scan_import(mfile.map, info.substring(info.lastIndexOf('.') + 1).trim(), info.trim());
					} else {
						scan_import(mfile.map, info.substring(br + 1).trim(), info.substring(0, br).trim());
					}
					return;
				}

				// include
				case EssprogCompiler.INCLUDE: {
					int br = info.indexOf(EssprogCompiler.GENERIC_DELIMITER);
					if (br == -1) {
						int lastDot = info.lastIndexOf('.');
						int lastDot2 = info.lastIndexOf('.', lastDot - 1); // second-to-last dot
						scan_import(mfile.map, info.substring(lastDot2 + 1, lastDot).trim(), info.trim());
					} else {
						scan_import(mfile.map, info.substring(br + 1).trim(), info.substring(0, br).trim());
					}
					return;
				}

				// handled by Tokenizer
				case EssprogCompiler.BREAK:
				case EssprogCompiler.CONTINUE:
				case EssprogCompiler.RETURN:
					return;

				// blocks with single conditions
				case EssprogCompiler.CASE:
					mfile.enter(ChainBlock.CASE);
					checkSingleLine(info);
					return;

				case EssprogCompiler.WHILE:
					mfile.enter(null);
					checkSingleLine(info);
					return;

				case EssprogCompiler.IF:
					mfile.enter(ChainBlock.IF);
					checkSingleLine(info);
					return;

				case EssprogCompiler.ELIF:
					mfile.enter(ChainBlock.ELIF);
					checkSingleLine(info);
					return;

				case EssprogCompiler.SWITCH:
					mfile.enter(null);
					checkSingleLine(info);
					return;

				case EssprogCompiler.FOR:
					mfile.enter(null);
					checkSingleLine(info);
					return;
			}
		}

		// if line is so far unrecognizable, try the last possible thing: an object defintion
		defineObj(line, true);
	}

	private void checkSingleLine(String info) {
		int defIndex = getNonStringIndex(info, EssprogCompiler.SINGLE_LINE_DELIMITER);
		if (defIndex != -1) { // single-line
			autoExit();
		}
	}

	/** Attempts to add an import. */
	private void scan_import(Map map, String alias, String path) {
		// check for duplicate imports
		if (map.imports.get(alias) != null) throw new DuplicateError(alias, "imports");

		// turn path into formal path format and add import
		map.imports.put(alias, path);
	}

	public Function getMainFunction() {
		return mainFunction;
	}

	@Override
	protected void warn(String message) {
		System.err.println("Warning@" + mfile.map.path + ":" + Token.currentLine + "\n\t" + message);
	}

	@Override
	protected Def getTree() {
		return mfile.tree();
	}

	@Override
	protected void add(DefToken token) {
		mfile.add((DefToken) token);
	}
}
