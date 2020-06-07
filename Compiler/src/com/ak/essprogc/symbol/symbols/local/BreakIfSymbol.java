package com.ak.essprogc.symbol.symbols.local;

import com.ak.essprogc.objects.LabelValue;
import com.ak.essprogc.objects.Memory;
import com.ak.essprogc.symbol.EssprogOutputStream;
import com.ak.essprogc.symbol.SymbolContext;
import com.ak.essprogc.symbol.symbols.LocalSymbol;

/**
 * A conditional break to one label or another.
 * 
 * @author Andrew Klinge
 */
public class BreakIfSymbol extends LocalSymbol {
	private final Memory cond;
	private Memory iftrue, iffalse; // may be changed as needed in token symbolizing stage

	// only LabelValue and LabelSymbol are accepted for iftrue and iffalse, hence the duplicate constructors and setters
	
	public BreakIfSymbol(Memory cond, LabelValue iftrue, LabelValue iffalse) {
		this(cond, (Memory) iftrue, (Memory) iffalse);
	}
	
	public BreakIfSymbol(Memory cond, LabelSymbol iftrue, LabelSymbol iffalse) {
		this(cond, (Memory) iftrue, (Memory) iffalse);
	}

	private BreakIfSymbol(Memory cond, Memory iftrue, Memory iffalse) {
		this.iftrue = iftrue;
		this.iffalse = iffalse;
		this.cond = cond;
	}
	
	public void setTrueLabel(LabelValue iftrue) {
		this.iftrue = iftrue;
	}

	public void setFalseLabel(LabelValue iffalse) {
		this.iffalse = iffalse;
	}

	public void setTrueLabel(LabelSymbol iftrue) {
		this.iftrue = iftrue;
	}

	public void setFalseLabel(LabelSymbol iffalse) {
		this.iffalse = iffalse;
	}

	@Override
	public void write(EssprogOutputStream os, SymbolContext sc) {
		os.write("br i1 " + cond.getID() + ", label " + iftrue.getID() + ", label " + iffalse.getID());
	}

	@Override
	public String getID() {
		return null;
	}
}