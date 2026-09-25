#!/bin/sh
set -eu

python3 extract-icons.py
as --32 -o wallpaper.o wallpaper.s
as --32 -o resource.o resource.s
ld -mi386pe --subsystem windows --disable-dynamicbase --disable-nxcompat \
  --disable-reloc-section -s -e _start -o NOS-Pipe-Wallpaper.exe wallpaper.o resource.o
python3 patch-pe.py NOS-Pipe-Wallpaper.exe

echo "Built NOS-Pipe-Wallpaper.exe"
