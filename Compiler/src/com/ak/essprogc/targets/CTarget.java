package com.ak.essprogc.targets;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

import com.ak.essprogc.EssprogCompiler;

/**
 * A C code file target.
 * 
 * @author Andrew Klinge
 */
public class CTarget extends Target {
	private final String path;

	/** @param _package - declared package directory for the target. */
	public CTarget(File file, String _package) {
		super(file);
		this.path = file.getPath().substring(_package.length() + 1)
			.replace('\\', EssprogCompiler.IMPORT_PATH_SEPARATOR)
			.replace('/', EssprogCompiler.IMPORT_PATH_SEPARATOR);
	}

	@Override
	public File getOutputFile(File binDir) {
		// CTarget paths end with ".c", so this must be removed and changed to LLVM_IR_EXT
		return new File(binDir.getAbsolutePath() + "/" + path + EssprogCompiler.LLVM_IR_EXT);
	}

	@Override
	public void compile(EssprogCompiler compiler, OutputStream out, File binDir) {
		Runtime rt = Runtime.getRuntime();

		File outputFile = getOutputFile(binDir);

		try {
			if (shouldCompile()) {
				Process proc = rt.exec(new String[] {
					"./scripts/c-" + EssprogCompiler.OS.nickname + EssprogCompiler.OS.scriptExt,
					getFile().getPath(),
					outputFile.getPath()
				});

				// print error output
				InputStreamReader errStream = new InputStreamReader(proc.getErrorStream());
				BufferedReader br = new BufferedReader(errStream);
				String line = null;
				boolean erred = false;
				while ((line = br.readLine()) != null) {
					System.err.println(line);
					erred = true;
				}
				br.close();

				// print output
				InputStreamReader outStream = new InputStreamReader(proc.getInputStream());
				br = new BufferedReader(outStream);
				while ((line = br.readLine()) != null) {
					System.out.println(line);
				}
				br.close();

				proc.waitFor();
				if (erred || proc.exitValue() != 0) {
					throw new RuntimeException("Compiling C file \"" + getFile().getPath() + "\" failed");
				}
			}

			if (out != null) {
				// copy the resulting file into the library zip file
				try {
					BufferedReader reader = new BufferedReader(new FileReader(outputFile));
					String line = null;
					while ((line = reader.readLine()) != null) {
						out.write(line.getBytes());
						out.write('\n');
					}
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getPath() {
		return path;
	}
}