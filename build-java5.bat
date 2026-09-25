@echo off
setlocal
if not exist out mkdir out
echo Building NOS-Pipe 1.0.2 for Java 5...
javac -encoding UTF-8 -source 1.5 -target 1.5 -d out src\notpipe\gui\*.java
if errorlevel 1 (
  echo BUILD FAILED
  exit /b 1
)
jar cfe NOS-Pipe.jar notpipe.gui.NOSPipeGui -C out .
echo BUILD OK
