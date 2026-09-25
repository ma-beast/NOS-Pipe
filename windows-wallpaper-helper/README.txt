NOS-Pipe Wallpaper helper
=========================

Small 32-bit Windows GUI executable with no C runtime and no Java dependency.
It supports Windows XP and newer systems using only system DLLs.

At every launch it:

1. Reads the current primary-screen resolution and desktop colour.
2. Loads assets\NOS-Pipe-cat.png beside the installed executable.
3. Creates an opaque BMP at the exact screen resolution, preserving the PNG
   proportions and centring it over the user's existing desktop colour.
4. Saves the BMP to %APPDATA%\NOS-Pipe\NOS-Pipe-wallpaper.bmp.
5. Selects non-tiled Center mode and applies the BMP as wallpaper.

Build on a GNU binutils host:

  sh build.sh

No prebuilt aspect-ratio backgrounds are used.
