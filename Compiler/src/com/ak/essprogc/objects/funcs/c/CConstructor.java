package com.ak.essprogc.objects.funcs.c;

import java.util.ArrayList;

import com.ak.essprogc.EssprogCompiler.Visibility;
import com.ak.essprogc.map.Mapper;
import com.ak.essprogc.objects.Container;
import com.ak.essprogc.objects.blocks.Filespace;
import com.ak.essprogc.objects.funcs.essp.Constructor;
import com.ak.essprogc.objects.types.TransType;

/**
 * A constructor function used to interface C/C++ with Essprog.
 * 
 * @author Andrew Klinge
 */
public final class CConstructor extends CFunction {

	/** Allows creating a new Constructor object with a TempType as the type. */
	public CConstructor(TransType type, String mangledName, ArrayList<TransType> params, Container parent, Filespace fs, Mapper mapper) {
		super(true, Visibility.INTERNAL, type, Constructor.USAGE_KEYWORD, mangledName, params, parent, fs, mapper);
	}
}
