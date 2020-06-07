package com.ak.essprogc.targets;

import java.io.File;
import java.io.OutputStream;

import com.ak.essprogc.EssprogCompiler;

/**
 * An Essprog code file target.
 * 
 * @author Andrew Klinge
 */
public class EssprogTarget extends Target {
	private String path;

	public EssprogTarget(File file) {
		super(file);
	}

	@Override
	public void compile(EssprogCompiler compiler, OutputStream out, File binDir) {
		if (!shouldCompile()) return;

		// Translates the written code into symbols.
		compiler.getTokenizer().tokenize(this);

		// Analyzes symbols from the first pass and links them to data and types, then writes.
		compiler.getSymbolizer().symbolize(this, compiler.getTokenizer(), compiler.getMapper(), out, binDir);
	}

	@Override
	public File getOutputFile(File binDir) {
		return new File(binDir.getAbsolutePath() + "/" + path + EssprogCompiler.LLVM_IR_EXT);
	}

	public void setPath(String path) {
		this.path = path;
	}

	@Override
	public String getPath() {
		return path;
	}
}
