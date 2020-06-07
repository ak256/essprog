package com.ak.essprogc.symbol.symbols.local;

import com.ak.essprogc.objects.Memory;
import com.ak.essprogc.objects.types.Type;
import com.ak.essprogc.symbol.EssprogOutputStream;
import com.ak.essprogc.symbol.SymbolContext;
import com.ak.essprogc.symbol.symbols.LocalSymbol;

/**
 * Extends a smaller type to a larger type.
 * 
 * @author Andrew Klinge
 */
public class ExtendSymbol extends LocalSymbol {
	private final Type from, to;
	private final Memory value; 
	private final String opWord;
	private String id;
	
	public ExtendSymbol(String opWord, Type from, Type to, Memory value) {
		this.from = from;
		this.opWord = opWord;
		this.to = to;
		this.value = value;
	}

	@Override
	public void write(EssprogOutputStream os, SymbolContext sc) {
		this.id = "%" + sc.nextID;
		os.write(id + " = " + opWord + " " + from.id() + " " + value.getID() + " to " + to.id());
		sc.nextID++;
		
		// MARK alternative to sext that could be tried. probably not faster than zext or sext:
		// if you first zext the number to the desired size and then xor with a number of the
		// desired size that is all 1's on the zext'd part and all 0's on the original part,
		// then it is the same as the sext operation.
		// for example:
		// i8 11001110 zext i16 = i16 0000000011001110
		// i16 0000000011001110 xor i16 11111111000000000 = i16 1111111111001110
	}
	
	@Override
	public String getID() {
		return id;
	}
}