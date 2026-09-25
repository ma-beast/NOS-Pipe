NOS-Pipe native Windows launcher
================================

NOS-Pipe.exe is a small 32-bit Win32 GUI launcher. It has no .NET or Visual C++
runtime dependency. Place it beside NOS-Pipe.jar and the bundled jre directory.

The launcher:
  - locates its own directory;
  - checks NOS-Pipe.jar and jre\bin\javaw.exe;
  - starts notpipe.gui.NOSPipeGui without a console window;
  - shows a Win32 error dialog when required files are missing;
  - contains the verified seven-size DIB icon compatible with Windows XP.

The checked-in NOS-Pipe.exe was tested by the project owner on Windows.

Rebuilding on Linux
-------------------

Requirements: Python 3 and GNU binutils with i386pe linker emulation.

  sh build.sh

No MinGW headers, import libraries or C runtime are used. launcher.s contains
the Win32 startup code and import table. resource.s contains the icon resource
tree; extract-icons.py supplies its seven DIB image payloads. patch-pe.py moves
the import-directory pointer past GNU ld's leading empty descriptor and updates
the PE checksum.

