NOS-Pipe 1.0.2.2 - YouTube for old computers
===========================================

NOS-Pipe is a lightweight YouTube search and playback front end for computers
that can no longer handle the modern youtube.com website.

SYSTEM REQUIREMENTS
-------------------
  Windows XP SP3 or newer, 32-bit or 64-bit.
  x86 processor; tested on Pentium III and newer systems.
  Internet connection.

Java and MPlayer are included in the NOS-Pipe folder. System Java is not
required and the application does not modify an existing Java installation.

STARTING NOS-PIPE
-----------------
  Double-click NOS-Pipe.exe.

NOS-Pipe.exe starts the bundled Java without a command window. RUN-NOS-Pipe.bat
is the reserve launcher. RUN-DIAGNOSTIC.bat creates a detailed troubleshooting
log. Do not move NOS-Pipe.jar by itself: the jre folder, mplayer.exe, the
mplayer folder, and gateways.properties must remain beside it.

CONTROLS
--------
  Enter               select or confirm
  Escape              close Search or Settings
  Arrow keys           move through results
  Number keys          quick video selection
  Up/Down in MPlayer   volume up/down

The first start shows popular videos. Later starts show videos related to the
last successfully opened video. Only one video ID is stored; NOS-Pipe does not
build an endless viewing history.

VIDEO
-----
360p is the main compatibility mode and uses one muxed video+audio stream.
Entering another resolution manually enables separate video and audio streams.
Seeking those streams may be unreliable in very old players. Only original or
ordinary unlabelled audio tracks are selected; translated and AI-dubbed tracks
are ignored. If no separate original audio is available, NOS-Pipe uses muxed
playback.

Player modes:
  auto      choose the suitable method automatically
  mplayer   use the bundled MPlayer
  system    send one muxed URL to the operating system
  custom    execute a user-defined command

In a writable portable folder and on Windows XP, settings are stored in
gateways.properties beside the JAR. An installation under Program Files on
Vista-11 uses %APPDATA%\NOS-Pipe\gateways.properties. Normal startup does not
create a debug log. Use RUN-DIAGNOSTIC.bat only when troubleshooting.

WINDOWS 98/ME - EXPERIMENTAL STARTUP
------------------------------------
The bundled OpenJDK 7 and modern mplayer.exe do not run on Windows 98/ME. The
NOS-Pipe classes use the Java 5 bytecode format and can be started manually with
older components:

  Java Runtime Environment 5.0 Update 22, 32-bit:
    jre-1_5_0_22-windows-i586-p.exe

  or Java Development Kit 5.0 Update 22, 32-bit:
    jdk-1_5_0_22-windows-i586-p.exe

  Oracle Java SE 5 Archive:
    https://www.oracle.com/java/technologies/java-archive-javase5-downloads.html

Playback requires an old Windows build:

  MPlayer 1.0rc1 for Windows:
    MPlayer-mingw32-1.0rc1.zip

  MPlayer home page:
    https://www.mplayerhq.hu/

  Old Windows builds:
    https://www.mplayerhq.hu/MPlayer/releases/win32/

Procedure:
  1. Install the 32-bit JRE or JDK 5.0 Update 22.
  2. Rename or remove the bundled jre folder; it is for XP and newer Windows.
  3. Replace mplayer.exe with the file from MPlayer-mingw32-1.0rc1.zip. Keep the
     bundled mplayer folder containing config and input.conf.
  4. Open a command prompt in the NOS-Pipe folder and run:

       javaw -cp NOS-Pipe.jar notpipe.gui.NOSPipeGui

Windows 98/ME are not officially supported. This route is experimental: the
old MPlayer may stutter, reject separate HTTPS streams, or fall back to muxed
360p. Keep video.height=360 for best compatibility. Java 5 no longer receives
security updates and Oracle may require an account for archive downloads.

KNOWN LIMITATIONS
-----------------
NOS-Pipe depends on third-party Invidious/Piped gateways and on YouTube-side
changes. Individual gateways may be temporarily unavailable. NOS-Pipe is not a
Google or YouTube product and is not supported by them.

AUTHOR PAGE
-----------
  https://samlib.ru/z/zwerew_m_a/

Third-party licenses and source notices are in the licenses folder. NOS-Pipe
source code is distributed as a separate archive.
