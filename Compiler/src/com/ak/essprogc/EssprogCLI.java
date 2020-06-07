package com.ak.essprogc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Command-line interface. Parses and handles command-line arguments and runs the EssprogCompiler.
 * 
 * @author Andrew Klinge
 */
public final class EssprogCLI {
	// @no format
	private static final String HELP = "=== Essprog Compiler Help ===\n" + "-b <dir> ... set binaries directory\n"
		+ "-c <dir> ... specify a C/C++ root directory\n" + "-h, --help ... view this help page\n"
		+ "--clean ... deletes all intermediate files and recompiles input files\n"
		+ "-i <dir> ... set main input dir. same as using -bcp\n"
		+ "--info ... enables compiler to print compile information\n"
		+ "--lib ... sets the output to be an Essprog library instead of an executable\n"
		+ "-t <path> ... specifies a file containing a list of paths to target files\n"
		+ "-m <declared path> ... specifies the main file with the given declared path\n"
		+ "-o <path> ... specifies the name of the output file\n"
		+ "-p <dir> ... adds all files in the given dir and its sub-dirs to the targets list\n"
		+ "--version ... prints the current version of the compiler\n"
		+ "\nGeneral usage example: essprog Desktop/Project/* --lib\n"
		+ "For more information, visit: andrewklinge.com/projects/essprog-docs/compiler/\n"
		+ "==============================\n";
	// @format

	private File currentBinDir = EssprogCompiler.DEFAULT_BIN;
	private boolean createLib = false; // whether to create a library or executable
	private boolean printInfo = false; // whether to print out how long compiling processes take
	private boolean clean = false;
	private boolean ignoreNoInput = false; // whether to ignore if no input files given (for when --version or etc used)
	private String outputName = "a"; // default output name is "a"
	private String main = null; // main file declared path
	private ArrayList<String> cPackages = new ArrayList<String>(); // c package specifiers
	private ArrayList<File> files = new ArrayList<File>(); // all code targets
	private ArrayList<File> projects = new ArrayList<File>(); // project directories specified by -p

	private EssprogCLI() {
	}

	/**
	 * Adds all usable code files (including those within subdirectories) in dir to list.
	 */
	private static void addAllUsableFiles(File dir, File binDir, ArrayList<File> list) {
		File[] files = dir.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				if (!file.equals(binDir)) {
					addAllUsableFiles(file, binDir, list);
				}
			} else if (isValidTarget(file)) {
				list.add(file);
			}
		}
	}

	/** Returns whether the compiler can use the file in compilation. */
	private static boolean isValidTarget(File file) {
		int ind = file.getName().lastIndexOf('.');
		if (ind < 0)
			return false;

		String ext = file.getName().substring(ind);
		switch (ext) {
		case EssprogCompiler.CODE_EXT:
		case EssprogCompiler.C_CODE_EXT:
		case EssprogCompiler.LIBRARY_EXT:
			return true;
		default:
			return false;
		}
	}

	public static void main(String[] args) {
		EssprogCLI cli = new EssprogCLI();

		// parse arguments
		boolean options = true;
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("--")) { // stop receiving options args
				options = false;
			} else if (options && args[i].charAt(0) == '-') { // option arg
				if (cli.parseOption(i, args))
					i++;
			} else { // normal input arg
				cli.parseArg(i, args);
			}
		}

		// add projects' files
		for (File project : cli.projects) {
			addAllUsableFiles(project, cli.currentBinDir, cli.files);
		}
		cli.projects = null;

		if(cli.clean) {
			// delete log
			EssprogCompiler.getBinLog(cli.currentBinDir).delete();

			// delete LLVM IR binaries
			for (File file : cli.currentBinDir.listFiles()) {
				// ensure only binaries are deleted. prevents irrelevant files from being deleted accidentally
				if (file.getPath().endsWith(EssprogCompiler.LLVM_IR_EXT)) {
					file.delete();
				}
			}
		}
		
		// compile
		if (cli.files.isEmpty()) {
			if(!cli.ignoreNoInput) {
				System.out.println("No usable files provided. Try --help.");
			}
			return;
		} else {
			if (!cli.createLib && cli.main == null) {
				System.out.println("ERROR: please specify the file containing the main method for the program starting point");
				return;
			}

			long time = System.currentTimeMillis();
			EssprogCompiler comp = new EssprogCompiler();
			comp.setPrintInfo(cli.printInfo);

			boolean success = comp.run(cli.files, cli.cPackages, cli.main, cli.createLib, cli.outputName, cli.currentBinDir);
			double seconds = (System.currentTimeMillis() - time) / 1000.0;

			if (cli.printInfo) {
				System.out.println((success ? "Completed" : "Failed") + " in total of " + seconds + " seconds");
			}
		}
	}

	private boolean parseArg(int i, String[] args) {
		int star = args[i].indexOf('*');

		if (star != -1) { // asterisk used
			int afterSlash = args[i].lastIndexOf('\\', star) + 1;
			File target = new File(args[i].substring(0, afterSlash));

			if (target.isDirectory()) {
				File[] targets = target.listFiles();
				String prefix = args[i].substring(afterSlash, star);
				String suffix = args[i].substring(star + 1);

				for (File t : targets) {
					if (!t.isDirectory()) {
						String relativePath = t.getPath().substring(target.getPath().length() + 1);

						if (relativePath.startsWith(prefix) && relativePath.endsWith(suffix)) {
							files.add(t);
						}
					}
				}
			} else {
				System.out.println("Error parsing path: " + args[i]);
				return true;
			}
		} else { // normal arg
			File target = new File(args[i]);

			if (!target.exists()) {
				System.out.println("ERROR: file specified as target \"" + args[i] + "\" does not exist");
				return true;
			}

			if (isValidTarget(target)) {
				files.add(target);
			} else {
				System.out.println("ERROR: file specified as target \"" + args[i] + "\" is not an accepted target type");
				return true;
			}
		}

		return false;
	}

	private boolean parseOption(int i, String[] args) {
		if (args[i].startsWith("--")) { // full word option
			switch (args[i].substring(2)) {
			case "info": // enables report times and info printing
				printInfo = true;
				break;

			case "lib": // specifies that a library should be created
				createLib = true;
				break;

			case "clean": // deletes bin files and recompiles everything
				clean = true;
				ignoreNoInput = true;
				break;

			case "help":
				System.out.println(HELP);
				ignoreNoInput = true;
				break;

			case "version":
				System.out.println("Essprog compiler version " + EssprogCompiler.VERSION + " by Andrew Klinge");
				ignoreNoInput = true;
				break;
			}
			return false;
		} else { // characters option
			String option = args[i].substring(1);
			String value = null;
			if (i + 1 < args.length)
				value = args[i + 1];
			boolean needsValue = false;

			for (int j = 0; j < option.length(); j++) {
				if (handleOption(option.charAt(j), value))
					needsValue = true;
			}

			return needsValue;
		}
	}

	private boolean handleOption(char option, String value) {
		switch (option) {
		case 'o': // specifies name of output compiled file
			if (value == null) {
				System.out.println("ERROR: missing target for output argument");
				System.exit(1);
			}
			if (value.contains("\\") || value.contains("/")) {
				System.out.println("ERROR: output file name cannot contain directory paths");
				System.exit(1);
			}

			outputName = value;
			return true;

		case 'm': // specifies main file
			if (value == null) {
				System.out.println("ERROR: missing target for main argument");
				System.exit(1);
			}

			main = value;
			return true;

		case 'c': // c package specifier
			if (value == null) {
				System.out.println("ERROR: missing target for c package argument");
				System.exit(1);
			}
			if (!(new File(value).exists())) {
				System.out.println("ERROR: target file for c package argument does not exist");
				System.exit(1);
			}

			cPackages.add(value);
			return true;

		case 'p':
			if (value == null) {
				System.out.println("ERROR: missing target for project argument");
				System.exit(1);
			}

			File project = new File(value);
			if (!project.exists()) {
				System.out.println("ERROR: target directory for project argument does not exist");
				System.exit(1);
			}

			projects.add(project);
			return true;

		case 'b':
			if (value == null) {
				System.out.println("ERROR: missing target for binary directory argument");
				System.exit(1);
			}

			File binParentDir = new File(value);
			if (!binParentDir.exists()) {
				System.out.println("ERROR: parent directory for target binary directory does not exist");
				System.exit(1);
			}

			currentBinDir = new File(binParentDir, "/bin");
			return true;

		case 'h':
			System.out.println(HELP);
			ignoreNoInput = true;
			return false;

		case 'i':
			if (value == null) {
				System.out.println("ERROR: missing target for input argument");
				System.exit(1);
			}

			File input = new File(value);
			if (!input.exists()) {
				System.out.println(input.getPath());
				System.out.println("ERROR: target directory for input argument does not exist");
				System.exit(1);
			}

			currentBinDir = new File(input, "/bin");
			projects.add(input);
			cPackages.add(value);
			return true;

		case 't': // specifies that the provided target contains a new-line separated list of
							// files to compile
			if (value == null) {
				System.out.println("ERROR: missing target for list argument");
				System.exit(1);
			}

			File targetsList = new File(value);
			if (!targetsList.exists()) {
				System.out.println("ERROR: target file for list argument does not exist");
				System.exit(1);
			}

			// load targets
			try {
				BufferedReader reader = new BufferedReader(new FileReader(targetsList));
				String line = "";
				while ((line = reader.readLine()) != null) {
					File target = new File(line);
					if (!target.exists()) {
						System.out.println("ERROR: file specified in target list file \"" + line + "\" does not exist");
						reader.close();
						System.exit(1);
					}
					files.add(target);
				}
				reader.close();
			} catch (IOException e) {
				System.out.println("ERROR: problem reading from target list file");
				System.exit(1);
			}
			return true;

		default:
			System.out.println("ERROR: unknown option \"-" + option + "\"");
			System.exit(1);
			return false;
		}
	}
}