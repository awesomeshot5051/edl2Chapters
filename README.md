# Edl2Chapters

Edl2Chapters turns the markers on a DaVinci Resolve timeline into a YouTube chapter list. Drop a marker wherever a new section starts, export the timeline as an EDL (or pull the markers straight out of Resolve), and you get text you can paste into your video description.

```
00:00 Intro
00:00 Sneak Peak
00:38 Channel Intro
00:46 Intro
02:05 A call for help
02:52 Mission 16 Briefing
```

It comes as a desktop app with a simple window, and as a command-line tool for when you would rather script it.

## Features

- Reads markers from a Resolve EDL and writes one chapter per line, sorted by time.
- Imports markers directly from an open Resolve timeline, no export step needed.
- Offset support, for timelines that do not start at 00:00:00.
- Optional intro chapter at 00:00, since YouTube wants the first chapter to start there. You can rename it or turn it off.
- Built for Linux. The code also knows Resolve's script folders on Windows and macOS, but releases only ship Linux packages for now.

## Installing

Grab the latest build from the [Releases page](https://github.com/awesomeshot5051/edl2Chapters/releases):

- **Debian and Ubuntu:** the `.deb` package. It adds a launcher entry and links `/usr/bin/edl2chapters`.
- **Any other x86-64 Linux:** the `.AppImage`. Make it executable and run it. It also keeps `~/.local/bin/edl2chapters` pointing at itself so the command works in a terminal.

Nothing else is needed. Each package bundles its own Java runtime.

## Using the app

Launch Edl2Chapters with no arguments to open the window.

1. In Resolve, add markers on your timeline and give each one a name. The name becomes the chapter title.
2. Either export the timeline as an EDL with markers (File > Export > Timeline) and open it in the app, or use the **Import from DaVinci** button.
3. Set an offset or intro title if you need them, then copy or save the result.

### Importing straight from DaVinci Resolve

The first time the app starts, it installs a small script into Resolve's `Scripts/Utility` folder. To use it, open Resolve, go to Workspace > Scripts > Edl2Chapters, then click **Import from DaVinci** in the app and pick a timeline.

Resolve 21 blocks sockets and file access in its scripts, so the script and the app talk through a mailbox folder and Resolve's `Fusion.prefs` file instead. The script shuts itself down when you close the app. If you would rather not use it, the EDL route works the same way without it.

## Using the command line

```
edl2chapters <input.edl> [-o <output.txt>] [--offset <HH:MM:SS>] [--intro-title <text>] [--no-intro]
```

| Option | What it does |
| --- | --- |
| `-o`, `--output` | Write the chapters to a file. Without it, they print to the terminal. |
| `--offset` | Subtract this time from every marker. Useful when your timeline starts at 01:00:00:00, for example. |
| `--intro-title` | Title for the 00:00 chapter. Defaults to `Intro`. |
| `--no-intro` | Skip the automatic 00:00 chapter. |
| `-h`, `--help` | Show usage. |

Example:

```
edl2chapters Mission16Markers.edl --offset 00:00:00 --intro-title "Start" -o chapters.txt
```

If a marker falls before the offset, the command stops with an error rather than producing a negative time.

## Building from source

You need JDK 25. The build uses Gradle and JavaFX, and the wrapper is included.

```
./gradlew run             # start the app
./gradlew test            # run the tests
./gradlew packageDeb      # Debian package
./gradlew packageAppImage # AppImage
```

`jpackage` ships with full JDKs only, so if your IDE's bundled runtime does not have it, pass `-PjdkHome=<path to a full JDK>`. The Windows installer scripts are still in the repo (`./gradlew packageExe` on Windows, or `packaging/windows/build-exe.sh` on Linux), but Windows is not part of releases right now.

### Releasing

`./gradlew publishRelease` tags the current version, publishes a GitHub release with the changelog, the `.deb` and the `.AppImage`.

## Project layout

```
src/main/java/com/awesomeshot5051/edl2chapters/
  app/cli, app/gui    command-line entry point and JavaFX interface
  model               markers and chapter options
  utils/edl           EDL parsing
  utils/chapters      chapter list building
  utils/time          timecode parsing and chapter time formatting
  utils/resolve       Resolve bridge client and script installer
src/main/resources/resolve/   the Lua bridge script
packaging/                    Linux and Windows installer files (Windows is not released yet)
gradle/                       packaging and release build scripts
```

## Changelog

See [CHANGELOG.md](CHANGELOG.md).
