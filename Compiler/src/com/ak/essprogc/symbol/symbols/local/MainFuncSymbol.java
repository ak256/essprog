package com.ak.essprogc.symbol.symbols.local;

import com.ak.essprogc.objects.funcs.essp.Function;
import com.ak.essprogc.symbol.EssprogOutputStream;
import com.ak.essprogc.symbol.SymbolContext;
import com.ak.essprogc.symbol.symbols.LocalSymbol;

/**
 * Represents the program entry point main function definition.
 * 
 * @author Andrew Klinge
 */
public class MainFuncSymbol extends LocalSymbol {
	protected final Function func;

	/**
	 * @param func - the main function (program entry point). Must be named "main" and have no parameters or string[] as a parameter.
	 */
	public MainFuncSymbol(Function func) {
		this.func = func;
	}

	@Override
	public void write(EssprogOutputStream os, SymbolContext sc) {
		// define hidden real main func (whose purpose is to be the actual entry point
		// and translates program input arguments into Essprog types and gives them
		// to the specified Essprog main function)
		switch (func.getParamCount()) {
			case 0: // ()
				os.write("define dso_local i32 @main() {");
				os.write("call void " + func.id() + "()");
				os.write("ret i32 0");
				os.write("}");
				break;

			case 1: // (string[])
				os.write("define dso_local i32 @main(i32, i8**) {");
				// translate (i32, i8**) into string[]
				os.write(""); // FIXME MainFuncSymbol translation
				os.write("%4 = call void " + func.id() + "([0 x i8] %3)");
				os.write("ret i32 0");
				os.write("}");
				break;

			default:
				throw new IllegalArgumentException("Main function has invalid number of parameters: " + func.getParamCount());
		}

		// define function
		os.write("define dso_local " + func.getType().id() + " " + func.id() + "(" + func.getParamIDString() + ") {");
		sc.nextID = func.getParamCount() + 1; // implicit ids for parameters and first LLVM IR 'block'
	}

	@Override
	public String getID() {
		return null;
	}
}
