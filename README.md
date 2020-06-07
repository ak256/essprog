# Essprog
A compiler for my own programming language, Essprog.

The language is largely unfinished and I am no longer working on it, but the compiler can do most things a compiler needs to: process files, map object definitions, parse syntax and expressions, check for errors, give feedback and error messages, and produce output files (intermediate binaries, executables, and libraries).

However, due to me trying get a standard library working, I don't think the compiler is currently in a working state.

The compiler produces LLVM IR files, which are then compiled, optimized, and linked into an executable by Clang.

More info on the language can be found <a href="https://andrewklinge.com/projects/essprog/">here</a> and <a href="https://andrewklinge.com/projects/essprog-docs/">here</a>, on my website.
