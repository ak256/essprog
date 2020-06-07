#!/bin/bash
clang -S -emit-llvm "$1" -o "$2"
