# Changelog

All notable changes to Edl2Chapters. Minor version bumps with every change; major only by decision.

## [1.3.0] - 2026-10-26
### Added
- AppImage build (`./gradlew packageAppImage`) for any x86-64 Linux distribution.
- Windows installer (`.exe`), built with NSIS: per-user install, start-menu entry and desktop shortcut, uninstaller, and `edl2chapters` added to the user's PATH.
- AppImage keeps `~/.local/bin/edl2chapters` pointing at itself, so the command works from a terminal (the `.deb` already linked `/usr/bin/edl2chapters`).
- One-command GitHub release (`./gradlew publishRelease`): creates and pushes the version tag, publishes the release as latest with this full changelog and the `.deb` and `.AppImage`. A GitHub Actions workflow builds the Windows installer on that tag and attaches it.
- Windows and macOS locations for DaVinci Resolve's script and prefs folders.
### Changed
- Gradle build converted from Kotlin DSL to Groovy and the packaging split into `gradle/*.gradle` scripts.

## [1.2.0] - 2026-10-26
### Changed
- DaVinci bridge rewritten in Lua using a file mailbox and `Fusion.prefs` (the same method AutoSubs uses), because Resolve 21 blocks sockets and `io` in scripts.
- The bridge script installs to `Scripts/Utility` so it shows in Workspace > Scripts on every page, and it stops when the app closes.
### Fixed
- Crash on start of the packaged app (`java.net.http` was missing from the bundled runtime).

## [1.1.0] - 2026-10-26
### Added
- "Import from DaVinci" button: pick a timeline and import its markers without exporting an EDL.
### Changed
- The Resolve helper is GUI-only; the CLI no longer touches it.

## [1.0.0] - 2026-10-26
### Added
- EDL marker to YouTube chapter converter with JavaFX GUI and command-line mode (`--offset`, `--intro-title`, `--no-intro`, `-o`).
- Themed interface using the site palette, application icon, and a Debian package that adds a launcher entry and puts `edl2chapters` on the PATH.
