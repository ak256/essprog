package com.ak.essprogc.objects.vars;

import com.ak.essprogc.EssprogCompiler.Visibility;
import com.ak.essprogc.map.DefType;
import com.ak.essprogc.map.Mapper;
import com.ak.essprogc.objects.Container;
import com.ak.essprogc.objects.Global;
import com.ak.essprogc.objects.Indexed;
import com.ak.essprogc.objects.blocks.Filespace;
import com.ak.essprogc.objects.types.Type;
import com.ak.essprogc.symbol.Symbolizer;
import com.ak.essprogc.symbol.symbols.global.ExternalSymbol;

/**
 * A global variable (which is implicitly final and static).
 * 
 * @author Andrew Klinge
 */
public final class GlobalVariable extends Variable implements Global {
	public GlobalVariable(String name, Visibility vis, Type type, Container parent, Filespace fs, Mapper mapper, String id) {
		super(name, vis, type, parent, id);
	}

	public GlobalVariable(String name, Visibility vis, Type type, Container parent, Filespace fs, Mapper mapper) {
		this(name, vis, type, parent, fs, mapper, "@" + Indexed.createID(parent, fs, mapper));
	}

	@Override
	public void reference(Symbolizer p2) {
		if (Filespace.of(parent) != p2.getFilespace() && !p2.containsGlobal(id())) {
			p2.add(new ExternalSymbol(id, type));
		}
	}

	@Override
	public boolean isStatic() {
		return true;
	}

	@Override
	public boolean isFinal() {
		return true;
	}

	@Override
	public void resolveTempTypes(Mapper mapper, Filespace fs) {
		type = mapper.findType(type.toString(), type.parent(), fs);
	}

	@Override
	public DefType objectType() {
		return DefType.GLOBAL_VAR;
	}
}
