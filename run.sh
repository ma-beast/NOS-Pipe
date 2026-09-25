#!/bin/sh
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
exec java -cp "$SCRIPT_DIR/NOS-Pipe.jar" notpipe.gui.NOSPipeGui "$@"
