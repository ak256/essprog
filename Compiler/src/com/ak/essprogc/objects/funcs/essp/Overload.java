package com.ak.essprogc.objects.funcs.essp;

import com.ak.essprogc.EssprogCompiler.Operator;
import com.ak.essprogc.EssprogCompiler.Visibility;
import com.ak.essprogc.errors.Error;
import com.ak.essprogc.map.Mapper;
import com.ak.essprogc.objects.Container;
import com.ak.essprogc.objects.blocks.Filespace;
import com.ak.essprogc.objects.types.Type;
import com.ak.essprogc.objects.types.UserType;

/**
 * An operator overload block.
 * 
 * @author AK
 */
public class Overload extends Function {
	public final Operator op;

	public Overload(Type type, String name, Type[] params, Container parent, Filespace fs, Mapper mapper) {
		super(true, Visibility.PUBLIC, type, name, params, parent, fs, mapper);

		// check for errors
		if (parent.parent() != null) //
			throw new Error("Operator overload functions must be defined within the file's immediate space! You cannot/should-not create them within other structures or blocks.");

		if (params.length < 1 || params.length > 2) //
			throw new Error("Operator overload functions must have exactly one or two parameters! These parameters are the values being used in the operation.");

		if (!(params[0] instanceof UserType) && !(params[1] instanceof UserType)) //
			throw new Error("Can only create operator overload functions that include user defined types! You cannot redefine the built-in primitive operations.");

		// initialize
		if (params[0] instanceof UserType) {
			((UserType) params[0]).addOverload(this);
		}
		if (params.length == 2 && params[1] instanceof UserType) {
			((UserType) params[1]).addOverload(this);
		}

		this.op = Operator.get(name);
	}
}