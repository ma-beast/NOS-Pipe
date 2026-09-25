NOS-Pipe 1.0.2 — Java 5 bytecode / bundled OpenJDK 7 Windows runtime

1.0.2 original-audio fix:
  - dubbed and auto-dubbed separate audio tracks are ignored;
  - original and ordinary untagged audio remain eligible;
  - when no original separate audio exists, playback falls back to muxed.
  - windows-launcher contains the tested native NOS-Pipe.exe, its assembly
    sources, XP-compatible icon and reproducible GNU binutils build scripts.

1.0.1 mini-fix:
  - Windows BAT starts javaw in a detached process and immediately closes CMD;
  - separate audio streams prefer the Java/OS language, then original,
    default and any available track; muxed 360p playback is unchanged.

Universal legacy MPlayer bridge:
  - Windows keeps the tested direct mplayer.exe playback path unchanged;
  - Intel Mac auto-detects mplayer~.x86 beside NOS-Pipe.jar;
  - PowerPC Mac auto-detects mplayer~.ppc beside NOS-Pipe.jar;
  - other Unix systems continue to use mplayer beside NOS-Pipe.jar;
  - bundled Mac mode sends muxed media through a loopback HTTP/1.0 bridge;
  - the first request is simplified to HTTP 200, avoiding the upstream HTTP 206
    parser failure in Tiger-era MPlayer builds;
  - MPlayer Range requests are relayed as compact HTTP 206 responses, allowing
    the MOV demuxer to find remote MP4 headers and seek in the stream;
  - simultaneous loopback requests are supported: the initial stream no longer
    blocks the old MOV demuxer's second header/range connection;
  - the bridge streams bytes immediately and creates no temporary video file;
  - bundled Mac mode is muxed-only in this first reversible implementation.

Thumbnail HTTP compatibility fix:
  - previews are loaded from http://img.youtube.com instead of HTTPS ytimg;
  - this avoids the obsolete Java trust-store failure seen on Mac OS X 10.12;
  - search, card layout, playback resolution and player selection are unchanged;
  - the distribution does not include mplayer.exe; add a suitable player beside
    NOS-Pipe.jar or select system/custom mode in Settings.

Waiting Nonsense mini-fix:
  - after VIDEO LAUNCHED, four randomly selected status messages appear;
  - messages change every three seconds and do not repeat within one sequence;
  - the messages are UI-only and do not add noise to NOS-Pipe.log.

Cross-platform player step:
  - writable/portable folders keep gateways.properties and last.video.id
    beside NOS-Pipe.jar;
  - protected installations copy the configuration to APPDATA/NOS-Pipe;
  - both modes are independent of the process working directory;
  - player.mode order in Settings: auto, mplayer, system, custom;
  - auto uses the platform-specific bundled MPlayer name listed above;
  - when no bundled player is present, auto opens one muxed URL with the OS;
  - system always requests the safe one-URL muxed playback path;
  - custom is the final option and accepts {video} and optional {audio};
  - a custom command containing {audio} may use the dual-stream resolver.

Examples:
  player.mode=custom
  player.command=mplayer -audiofile {audio} {video}

  player.mode=custom
  player.command=open -a "QuickTime Player" {video}

Unix/macOS bundled MPlayer:
  - Intel Mac: mplayer~.x86; PowerPC Mac: mplayer~.ppc;
  - Linux/other Unix: mplayer;
  - run chmod +x for the selected executable and run.sh;
  - Mac uses bridged muxed playback; other Unix retains direct/dual playback.

This build keeps the tested MPlayer legacy playback path and restores BOTH
previous speed changes:
  1) all configured gateway searches run in parallel;
  2) the selected result's gateway is probed first;
  3) the returned media URL is checked with a tiny range request;
  4) HTTP 200/206 launches immediately; rejected URLs use the parallel fallback;
  5) fallback candidates are also checked in score order before launch.

Startup home:
  - empty last.video.id in gateways.properties -> Popular;
  - otherwise -> videos related to the last successfully launched video;
  - if Related is unavailable -> Popular;
  - each successful launch replaces the one stored video ID.

Thumbnail cache N1:
  - the cache contains exactly the visible card count + 2 images;
  - small screens no longer retain a forced minimum of 8 images;
  - this prevents continuous eviction/reload repaint loops on large screens.

Compact header MINI-FIX 2:
  - removes the separate NOS Pipe title and layout/resolution line;
  - combines the current query/feed name with visible and total result numbers;
  - uses a bold font two points larger than the configured base font;
  - card drawing and mouse hit-testing share the same dynamic content top.

Dynamic status MINI-FIX 3:
  - reports completed/total gateways and found items during Search/Home loading;
  - reports visible thumbnail loading progress;
  - reports preferred video probing, media-link checks and HTTP rejection;
  - reports fallback gateway progress and video-player launch;
  - long status text is clipped safely to the available pixel width.

Card text clipping MINI-FIX 4:
  - every preview card has its own hard drawing boundary;
  - large fonts cannot paint title or author text into a neighbouring card;
  - card layout, selection and input geometry are unchanged.

Preview layout MINI-FIX 5:
  - line 1 shows the result number and video duration;
  - the title uses up to two lines and is clipped by pixel width;
  - the final line shows the creator channel;
  - the gateway name is no longer displayed inside preview cards;
  - Piped duration and Invidious lengthSeconds are preserved through merging;
  - row height follows the selected font size so large text remains readable.

Playback backend:
  video.height=360:
    mplayer.exe "MUXED_URL"

  any manually selected height other than 360:
    mplayer.exe -audiofile "AUDIO_URL" "VIDEO_URL"

360p is the safe default and is hard-bound to the tested one-URL muxed path.
Changing video.height manually enables the experimental dual-stream resolver.
It prefers MP4/H.264 video and AAC audio, checks BOTH URLs, and passes them to
MPlayer with -audiofile. Original/default audio is preferred over dubbed audio
when a gateway supplies track metadata. If no complete pair survives resolution/preflight or
MPlayer exits during its startup window, NOS-Pipe retries the tested muxed path.

Deferred MINI-FIX 1:
  - the result number on every preview card is now bold;
  - duration remains plain and separate, so large text cannot collide with it.

Release MINI-FIX 1 — dialogs:
  - Escape and the title-bar close button cancel Search and Settings;
  - Enter in the Search field uses exactly the same action as the OK button;
  - the new query is stored immediately, including when the network search fails.

Release MINI-FIX 2 — MPlayer keys:
  - mplayer/input.conf maps Up to volume +1;
  - mplayer/input.conf maps Down to volume -1.

The MPlayer executable is intentionally not included in this compact package.

Release startup does not create NOS-Pipe.log. Diagnostic logging remains
available with -Dnospipe.log=true and is overwritten at each diagnostic launch.
MPlayer command input is closed after
launch and its merged output is drained in the background so it cannot block
on Java process pipes. The MPlayer quiet=yes and framedrop=yes settings remain.
The log records preferred/fallback probe times, media HTTP status, and total
stream-resolution time.

Java 5 build:
  build-java5.bat

Run:
  run-gui-mplayer-java5.bat
Optional direct startup search:
  run-gui-mplayer-java5.bat "cats"

The full Java source tree under src\\ is included and corresponds to this build.
