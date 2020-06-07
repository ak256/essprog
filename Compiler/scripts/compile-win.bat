@ECHO off
cd %1
clang *.ll -o %2 --no-warnings
