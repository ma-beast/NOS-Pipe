#!/bin/sh
set -eu

python3 extract-icons.py
as --32 -o launcher.o launcher.s
as --32 -o resource.o resource.s
ld -mi386pe --subsystem windows --disable-dynamicbase --disable-nxcompat \
  --disable-reloc-section -s -e _start -o NOS-Pipe.exe launcher.o resource.o
python3 patch-pe.py

echo "Built NOS-Pipe.exe"

