@echo off
setlocal
if not exist gateways.properties (
  echo Missing gateways.properties
  exit /b 1
)
set "QUERY=%*"
if "%QUERY%"=="" set "QUERY=HOME"
echo Starting NOS-Pipe 1.0.2 Java5: %QUERY%
java -cp NOS-Pipe.jar notpipe.gui.NOSPipeGui %*
