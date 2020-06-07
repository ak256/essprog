package com.ak.essprogc.targets;

import java.io.File;
import java.io.OutputStream;

import com.ak.essprogc.EssprogCompiler;

/**
 * A file targeted for compilation.
 * 
 * @author Andrew Klinge
 */
public abstract class Target {
	private final File file;

	/** Whether this target needs to be compiled. Set to false if has previously been compiled and the binary still exists. */
	private boolean shouldCompile = true;

	/**
	 * @param file - the file corresponding to this target.
	 * @param shouldCompile -
	 */
	public Target(File file) {
		this.file = file;
	}

	/**
	 * Compiles the file.
	 * 
	 * @param compiler - the EssprogCompiler being used.
	 * @param out - OutputStream for compiled code. if null, defaults to a file within the same directory as the source code.
	 * @param binDir - the binaries directory where the output file should be put.
	 */
	public abstract void compile(EssprogCompiler compiler, OutputStream out, File binDir);

	/**
	 * Whether this target needs to be compiled. Set to false if has previously been compiled and the binary still exists.
	 * <br>
	 * Targets are always mapped, but may not need to be recompiled.
	 */
	public final boolean shouldCompile() {
		return shouldCompile;
	}

	/** Checks to see if the target needs to be recompiled, and, if not, disables the compile step for it. */
	public final void preventRecompile(File binDir, long lastCompileTime) {
		File binary = getOutputFile(binDir);

		if (binary.exists() && file.lastModified() < lastCompileTime) {
			shouldCompile = false;
		}
	}

	/**
	 * Returns the compiled LLVM IR code file for this target.
	 * 
	 * @param binDir - the binaries directory where the output file should be put.
	 */
	public abstract File getOutputFile(File binDir);

	/** The declared path of this file. May be different from the path given from getFile(). */
	public abstract String getPath();

	/** The actual file. */
	public File getFile() {
		return file;
	}
}