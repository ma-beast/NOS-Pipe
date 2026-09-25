# NecronomicOS / NOS-Pipe — Project Progress

## Current release checkpoint: 1.0

- Universal bundled-player names: Windows `mplayer.exe`, Intel Mac
  `mplayer~.x86`, PowerPC Mac `mplayer~.ppc`, other Unix `mplayer`.
- Tiger-era Mac MPlayer receives muxed video through a Java 5 loopback bridge.
- RANGE FIX: the first request remains local HTTP/1.0 200, while MPlayer byte
  ranges are relayed upstream and returned as compact HTTP/1.0 206 responses.
- MAC BRIDGE FIX 2: each loopback connection is handled independently. The old
  MOV demuxer may open its header/range connection before closing the initial
  stream, so serial handling caused a deadlock before any Range header arrived.
- This makes ordinary MP4 files seekable so the old MOV demuxer can locate a
  movie header stored away from the beginning of the file.
- Data is streamed without a complete download or temporary file.
- Mac bundled mode is intentionally muxed-only for this first testable layer.
- Windows and non-Mac Unix playback behavior remains unchanged.

- Thumbnail compatibility: use `http://img.youtube.com` so legacy Java runtimes
  do not require a working HTTPS trust store for preview images.
- Compact distribution: no bundled `mplayer.exe` and no intermediate build trees.

- Waiting Nonsense mini-fix: four random three-second status messages follow
  `VIDEO LAUNCHED`; they are intentionally cosmetic and are not written to the log.
- Cross-platform player selection: auto, mplayer, system, custom.
- Writable/portable folders keep settings beside NOS-Pipe.jar; protected
  installations use APPDATA/NOS-Pipe automatically.
- Java 5 bytecode (class version 49).
- Global stream resolver retained as the basis.
- Fast parallel gateway SEARCH restored.
- Fast parallel playback-source PROBES restored.
- Selected-result gateway is probed first for immediate playback.
- Parallel global resolution is retained unchanged as fallback.
- Media URLs are preflighted with a tiny range request before MPlayer launch.
- HTTP 403/404 and timed-out URLs fall through to the remaining gateways.
- Legacy MPlayer backend tested on Windows XP / Pentium III.
- Default 360p retains the verified one-URL muxed MPlayer path.
- Any manually selected non-360 height enables experimental video + audio streams.
- Dual video and audio URLs are both preflighted; failure returns to muxed playback.
- The player executable is supplied separately by the user.
- Release startup creates no log; optional diagnostics use -Dnospipe.log=true.
- MPlayer command input is closed and merged output is drained asynchronously.
- Empty last.video.id starts Popular; otherwise startup loads Related.
- Only one last successfully launched video ID is stored.
- MINI-FIX 1: thumbnail cache is exactly visible cards + 2, with no minimum 8.
- MINI-FIX 2: one bold base+2 header combines feed/query and visible range.
- MINI-FIX 3: bottom line reports live gateway, preview, media and launch progress.
- MINI-FIX 4: preview text is clipped to its own card at large font sizes.
- MINI-FIX 5: cards show number/duration, a two-line title and creator channel.
- Deferred MINI-FIX 1: the list number is bold while duration remains plain.
- Release MINI-FIX 1: Search/Settings close on Escape or title-bar X; Search Enter equals OK.
- Release MINI-FIX 2: MPlayer Up/Down keys control volume through input.conf.
- Full Java sources are included under src/.

## Verified legacy path retained
NOS Pipe -> selected-result gateway -> muxed stream -> media preflight -> mplayer.exe.
If preflight fails: parallel fallback probes -> verified muxed stream -> mplayer.exe.
Next start -> Related for the last launched video (or Popular when empty/unavailable).

## Experimental 0.9.7 path
Manual non-360 height -> video-only + audio-only -> two preflights ->
mplayer.exe -audiofile AUDIO_URL VIDEO_URL -> muxed fallback on early failure.
