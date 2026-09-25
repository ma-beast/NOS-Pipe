NOS-Pipe 1.0.2.2 installer source
=================================

This is an installer-only revision of the user-tested NOS-Pipe 1.0.2
SETTINGS-EN application.

Build requirements:

- NSIS 3 Unicode;
- the complete NOS-Pipe-1.0.2.2-Windows payload beside this source tree.

Before building, copy these two files from installer\windows-payload-assets to
the payload:

- NOS-Pipe-Wallpaper.exe -> payload root;
- NOS-Pipe-cat.png -> payload\assets.

Then run from the installer directory:

  makensis NOS-Pipe-1.0.2.2.nsi

NOS-Pipe-Wallpaper.exe is built separately from windows-wallpaper-helper. Its
complete GNU assembler source and reproducible build scripts are included.
It uses no Java, C runtime, prepared wallpaper canvases, or third-party DLLs.
