package com.ak.essprogc.expr;

import java.util.ArrayList;

import com.ak.essprogc.EssprogCompiler;
import com.ak.essprogc.EssprogCompiler.Operator;
import com.ak.essprogc.errors.MissingError;
import com.ak.essprogc.objects.Memory;
import com.ak.essprogc.objects.blocks.Filespace;
import com.ak.essprogc.objects.funcs.essp.Function;
import com.ak.essprogc.objects.types.CArrayType;
import com.ak.essprogc.objects.types.PrimitiveType;
import com.ak.essprogc.objects.types.TempType;
import com.ak.essprogc.objects.types.Type;
import com.ak.essprogc.objects.types.UserType;
import com.ak.essprogc.objects.vars.GlobalVariable;
import com.ak.essprogc.objects.vars.TypeVariable;
import com.ak.essprogc.objects.vars.Variable;
import com.ak.essprogc.symbol.Symbolizer;
import com.ak.essprogc.symbol.Symbolizer.VariableContext;
import com.ak.essprogc.symbol.symbols.LocalSymbol;
import com.ak.essprogc.symbol.symbols.global.ComplexConstantSymbol;
import com.ak.essprogc.symbol.symbols.global.ConstantSymbol;
import com.ak.essprogc.symbol.symbols.global.DeclareSymbol;
import com.ak.essprogc.symbol.symbols.global.TypeSymbol;
import com.ak.essprogc.symbol.symbols.local.AggGEPSymbol;
import com.ak.essprogc.symbol.symbols.local.AllocaSymbol;
import com.ak.essprogc.symbol.symbols.local.CallSymbol;
import com.ak.essprogc.symbol.symbols.local.GEPSymbol;
import com.ak.essprogc.symbol.symbols.local.LoadSymbol;
import com.ak.essprogc.symbol.symbols.local.StoreSymbol;

/**
 * A single-value expression. <br>
 * Ex: "Hello World!" <br>
 * Ex: 42
 * 
 * @author Andrew Klinge
 */
public class ExprVal extends Expr {
	public final String value;

	ExprVal(String value) {
		this.value = value;
	}

	@Override
	public Result symbolize(Symbolizer p2, ArrayList<LocalSymbol> locals) {
		Type type; // determine type
		if (value.startsWith(EssprogCompiler.STRING_DELIMITER) && value.endsWith(EssprogCompiler.STRING_DELIMITER)) {
			String content = value.substring(1, value.length() - 1);
			int hc = value.hashCode();
			String id = "@s" + hc; // id of actual essprog string global
			String contentID = "sr" + hc; // id of string character data constant
			int contentLength = (content.length() + 1);
			Type charArrayType = new CArrayType(1, PrimitiveType.INT8, contentLength);

			// add raw string character constant
			p2.add(new ConstantSymbol(contentID, charArrayType.id() + " c\"" + content + "\\00\""));

			// create essprog string in global memory using raw string data
			Function strCtor = (Function) EssprogCompiler.STRING_FS.get(Filespace.PREF_FUNC + "" + Filespace.PREF_TYPE + "string:new#int8[]");
			ArrayList<LocalSymbol> dataSymbols = new ArrayList<LocalSymbol>();
			
			// access string data. need to now conform to essprog array type {i32, [0 x i8]}
			if(!p2.containsGlobal("esspStrStruct")) {
				p2.add(new TypeSymbol("%esspStrStruct", "i32, [0 x i8]"));
			}
			TempType esspArrT = new TempType("%esspStrStruct", null);
			
			// allocate esspArr
			LocalSymbol esspArr = new AllocaSymbol(esspArrT);
			dataSymbols.add(esspArr);
			
			// get ptr to length field in struct esspArr
			LocalSymbol lengthPtr = new AggGEPSymbol(esspArrT, esspArr, 0);
			dataSymbols.add(lengthPtr);
			
			// store length of array in esspArr
			dataSymbols.add(new StoreSymbol(PrimitiveType.INT32, new Value(Integer.toString(contentLength)), lengthPtr));
			
			// copy string chars from const global into string esspArr struct
			if (!p2.containsGlobal("@llvm.memcpy.p0i8.p0i8.i64")) { // make sure function is declared
				p2.add(new DeclareSymbol("@llvm.memcpy.p0i8.p0i8.i64", "void", "i8* nocapture writeonly, i8* nocapture readonly, i64, i32, i1", false));
			}
			LocalSymbol contentAccess = new CallSymbol(PrimitiveType.VOID, new Value("@llvm.memcpy.p0i8.p0i8.i64"),
				new Type[] { PrimitiveType.OBJ, PrimitiveType.OBJ, PrimitiveType.INT64, PrimitiveType.INT32, PrimitiveType.BOOL },
				new Memory[] {
					new Value("getelementptr inbounds ([0 x i8], [0 x i8]* %1, i64 1)"),
					new Value("getelementptr inbounds (" + charArrayType.id() + ", " + charArrayType.id() + "* @" + contentID
						+ ", i32 0, i32 0)"),
					new Value(Integer.toString(contentLength)),
					new Value("1"),
					new Value("false")
				});
			dataSymbols.add(contentAccess);
			
			LocalSymbol result = strCtor.generateCall(p2, dataSymbols, new Type[] { esspArrT }, new Memory[] { esspArr });
			p2.add(new ComplexConstantSymbol(id, EssprogCompiler.STRING, new Result(EssprogCompiler.STRING, result), dataSymbols, p2));

			type = EssprogCompiler.STRING;
			p2.setIDType(type);
			return new Result(type, new Value(id));
		} else {
			if (value.equals(EssprogCompiler.NULL_VALUE) || value.isEmpty())
				type = PrimitiveType.VAL;
			else if (value.equals(EssprogCompiler.TRUE_VALUE) || value.equals(EssprogCompiler.FALSE_VALUE))
				type = PrimitiveType.BOOL;
			else if (Character.toString(value.charAt(0)).equals(Operator.SUBTRACT.op) || Character.isDigit(value.charAt(0))) { // numerical
				// constant
				if (value.contains(".")) {
					type = PrimitiveType.FP;
				} else {
					type = PrimitiveType.INT;
				}
			} else {
				VariableContext vc = p2.findVarContext(value, p2.getSpace());

				if (vc.var == null)
					throw new MissingError(value);
				else
					type = vc.var.type();

				Memory id = new Value(vc.var.id());
				if (vc.var instanceof TypeVariable) { // different access if class field
					Variable target;
					if (vc.container == null) { // inside method (function that is within the container class)
						// the only other place a field could be redefined (besides outside using an
						// accessor) is inside the class in a function
						// all methods have the class instance passed in as the first argument in LLVM IR
						target = p2.findVar(EssprogCompiler.METHOD_OBJ_INSTANCE, p2.getSpace());
					} else {
						target = vc.container;
					}

					GEPSymbol gep = new GEPSymbol((UserType) target.type(), new Value(target.id()), ((TypeVariable) vc.var).index);
					locals.add(gep);
					p2.setIDType(null);
					id = gep;
				} else if (vc.var instanceof GlobalVariable) {
					((GlobalVariable) vc.var).reference(p2);
				}

				LoadSymbol ls = new LoadSymbol(type, id);
				locals.add(ls);
				p2.setIDType(type);
				return new Result(type, ls);
			}
			p2.setIDType(type);
			return new Result(type, new Value(value));
		}
	}

	@Override
	public String toString() {
		return value;
	}
}