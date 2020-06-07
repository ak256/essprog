#!/bin/bash
cd "$1"
clang $(find . -maxdepth 1 -iname '*.ll') -o $2 --no-warnings
