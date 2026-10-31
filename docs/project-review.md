# Mathdoku: project review

Reviewed 2026-10-31, against `master` at `153c653` plus the uncommitted working tree.
Line numbers refer to the working tree on that date.

## How far to trust this document

| Label | Meaning |
| --- | --- |
| **Built** | Observed in a real `assembleDebug` + `lintDebug` run on a scratch copy of the working tree |
| **Dex** | Observed by disassembling the shipped `app/release/net.cactii.mathdoku-release.apk` |
| **Read** | Established by reading the code; not run |
| **Unverified** | Inference or an open question, stated as such |

The app was **not run** on a device or emulator, and the repo has no tests, so
all behavioural findings are from reading the code. Coverage was uneven: generation, solving,
storage, sharing and the main activity were read closely. Painters, statistics charts, tips
and swipe input were only skimmed.

## Summary

Mathdoku is a KenKen-style puzzle game for Android, published on Google Play as
`net.cactii.mathdoku`, licensed GPLv3. It is about 24,000 lines of Java across 89 files, written
in Android 3/4-era style (Holo theme, `AsyncTask`, a bundled support-v4 jar). The build tooling
was modernised in September 2024 (Gradle 8.10, AGP 8.6, Kotlin DSL, GitHub Actions), but the
application code was barely touched.

The core is solid and self-contained: the generator, the Dancing Links solver and the SQLite
storage all have clear structure. The main risks are around the code, not in it:

1. **Google Play:** the app targets API 34, below the current requirements. It cannot ship an update until
   it targets API 36, which forces edge-to-edge and predictive-back work.
2. **User data:** the shipped 604 build wipes all puzzles and statistics when upgrading from
   any earlier version.
3. **Source of truth:** what shipped was never committed. `master` says version 600, API 35,
   development mode.
4. **No tests:** nothing guards the generator, solver or save format before a large modernisation.

## Project at a glance

| Item | Value |
| --- | --- |
| Application ID / namespace | `net.cactii.mathdoku` |
| Version (working tree, shipped APK) | 2.14, versionCode 604 |
| Version (`HEAD`) | 2.13, versionCode 600 |
| minSdk / targetSdk / compileSdk | 25 / 34 / 34 (`HEAD`: target and compile 35) |
| Build | Gradle 8.10, AGP 8.6.0, build-tools 34.0.0, Java source level 17 |
| Permissions | None declared, including no `INTERNET` (**Dex**, `aapt2` badging) |
| Dependencies | Three jars in `app/libs`: support-v4, AChartEngine 1.1.0, BugSense 3.5 |
| Languages | English, German, Dutch |
| Tests | None. `app/src/test` and `app/src/androidTest` do not exist |
| Shrinking | Off (`isMinifyEnabled = false`; `proguard-rules.pro` is referenced but missing) |
| History | 22 commits, 2018 to 2024. The 2009 to 2018 history is not in this repo |

### Building

**Built:** with the current working tree, this succeeds and produces a 788 KB debug APK:

```sh
JAVA_HOME=~/.jdks/jbr-21.0.11 ./gradlew assembleDebug lintDebug
```

- The JDK matters. Android Studio's bundled JBR at `/opt/android-studio/jbr` is Java 25, and
  Gradle 8.10 fails on it with a bare `25.0.3` error. The system JDKs (8 and 11) are too old.
- `--offline` fails on this machine because `aapt2` for AGP 8.6.0 is not in the Gradle cache.
- CI (`.github/workflows/CI.yml`) runs `./gradlew build` on JDK 21 and uploads the debug APK.
  Lint cannot fail the build (`abortOnError = false`).
- `val javaVersion = VERSION_21` in `app/build.gradle.kts` is now unused; the compile options
  hardcode 17.

## Architecture

There are no layers in the modern sense. Domain objects read and write the database directly,
and global singletons (`Preferences`, `DatabaseHelper`, `Painter`, `Util`) are initialised in
`AppFragmentActivity.onCreate`. `GridGenerator`'s Javadoc notes this hidden dependency.

| Package | Role |
| --- | --- |
| `com.srlee.DLX` | Dancing Links exact-cover solver (`DLX`), and its puzzle mapping (`MathDokuDLX`) |
| `n.c.m.grid` | Domain model: `Grid`, `GridCage`, `GridCell`, `CellChange` (undo) |
| `n.c.m.grid.ui` | Custom views: `GridPlayerView`, `GridViewerView`, `SwipeMotion` |
| `n.c.m.gridGenerating` | `GridGenerator`, `CageTypeGenerator`, `GridCageType` |
| `n.c.m.painter` | Theme-aware `Paint` holders behind the `Painter` singleton |
| `n.c.m.storage` | `database/` adapters, `GameFileConverter` (runs on each upgrade) |
| `n.c.m.statistics` | Per-grid, cumulative and historic statistics |
| `n.c.m.ui` | Activities, fragments, preference screens |
| `n.c.m.tip`, `n.c.m.hint` | One-off tip dialogs and the `TickerTape` hint strip |
| `n.c.m.util` | `Random`, `SharedPuzzle`, `FeedbackEmail`, `FileProvider`, `Screendump` |
| `n.c.m.developmentHelper` | Build-mode switch and developer tools |

The largest classes are `Grid` (1,431 lines), `PuzzleFragment` (1,303), `PuzzleFragmentActivity`
(1,212), `GridGenerator` (1,209) and `GridCell` (1,184).

### Screens

- `PuzzleFragmentActivity` is the launcher and the app's hub. It hosts `PuzzleFragment` for
  active games, or `ArchiveFragment` once a game is finished, plus the navigation drawer.
- `ArchiveFragmentActivity` is a pager over past games; `StatisticsFragmentActivity` shows charts.
- `SharedPuzzleActivity` previews a puzzle from a share link.

Statistics unlock after the first finished game, and the archive after five solved games.
The back button acts as undo during play (`PuzzleFragmentActivity.java:200`).

### Puzzle generation

`GridGenerator` is an `AsyncTask`, and each attempt runs these steps:

1. `randomiseGrid` fills a Latin square, one value at a time, with retries.
2. `createCages` partitions the grid. For maximum cage size 5 or more it first drops one large
   cage at random. It then walks the cells, picking random cage shapes that fit.
3. Shapes are rejected if they repeat a value pair across two rows or columns, because that
   produces an obvious non-unique solution, or if they exceed the permutation limit.
4. `setArithmetic` picks operators. Two-cell cages can use any operator, weighted towards `/`
   and `-`. Larger cages use only `+` and `x`.
5. `MathDokuDLX.hasUniqueSolution` checks uniqueness, stopping at the second solution. If the
   puzzle is not unique, the whole attempt repeats.

Difficulty is set entirely by the inputs. The computed "real complexity" in `MathDokuDLX` is
unfinished and only reachable in development mode.

| Difficulty | Max cage size | Max cage result | Max cage permutations |
| --- | --- | --- | --- |
| Very easy | 2 | 99 | 20 |
| Easy | 3 | 999 | 20 |
| Normal | 4 | 2,500 | 40 |
| Difficult | 5 | 9,999 | 80 |
| Very difficult | 6 | 99,999 | 120 |

Grid sizes are 4x4 to 9x9. `util/Random.java` is a copy of GNU Classpath's `java.util.Random`,
kept so that a stored game seed plus generator revision can reproduce a puzzle. **Do not change
it, or the order of calls to it, without bumping the generator revision.**

### Solver encoding

Each DLX row is one permutation of one cage. Columns are one per cage, plus one for each
(digit, column) pair and one for each (digit, row) pair. That gives `cages + 2 * size²` columns.
Cage permutations come from `GridCage.getPossibleNums`, and cages are sorted by permutation count
because that strongly affects solving time.

### Persistence

One SQLite database, `MathDoku.sqlite`, with its version **tied to the app's versionCode**
(`DatabaseHelper.getVersion`), so every release triggers `onUpgrade`.

| Table | Contents |
| --- | --- |
| `grid` | One row per unique puzzle: `definition` (unique), size, seed, generator revision, complexity, hide-operators, limits |
| `solving_attempt` | One row per play or replay: a text blob of game state, plus status and revision |
| `statistics` | Counters per attempt, with `include_in_statistics` marking the attempt that counts |

The attempt blob is line-based and `:`/`,` delimited: a `GRID` line, one cell line each,
`CAGE` lines, then the move list. Loading tolerates older forms, but anything saved by revision
368 or earlier is rejected.

The upgrade path is `isUpgradeRunning` → `GameFileConverter` (rewrites every attempt on every
upgrade) → `upgradePhase2_UpdatePreferences` → `Preferences.upgrade`.

### Sharing

Share links have the form `http://mathdoku.net/puzzle/2/<definition>/<hashcode>`.

A definition is `complexity:cellCageIds:id,result,action:...`. With hidden operators, the action
is written as 0. The receiver rebuilds the solution by running the solver, so no solution is in
the link. The definition is also what `grid.definition` stores, which is how repeat puzzles are detected.

### Build modes

`DevelopmentHelper.mMode` is `DEVELOPMENT`, `BUG_SENSE` or `PRODUCTION`, edited by hand before a
release. Development mode adds a menu (delete database, generate games, reset preferences,
unlock archive, clear data), schema consistency checks, and deliberate crashes on slow
generation (30 seconds) and database insert failures.

## Findings

### Critical

**C1. The app is below Google Play's target API requirement.** The app targets 34.
Per [Google's policy page](https://support.google.com/googleplay/android-developer/answer/11926878),
from 2026-08-31 new apps and updates must target API 36, and existing apps must target at least
API 35 to stay compliant. So no update can currently be submitted, and the live listing is
probably already restricted. Check Play Console for its actual status and for any extension.

`HEAD` targets 35 and the working tree went back to 34. The drop came minutes after 601 was
uploaded and before any device test, so it was probably not a runtime problem (see
[release history](release-history-2024.md)). Behaviour at 35 is still untested, and the code has
no inset handling at all (no `fitsSystemWindows` or similar anywhere in `app/src/main`).
Moving to 36 will need:

- Inset handling on every screen. The opt-out does not apply at 36, and the app's Holo action
  bar and custom layouts have never been tested under edge-to-edge.
- A new back-handling approach. At target 36, predictive back is on by default and
  `onBackPressed`, which implements undo, is no longer called unless the app opts out in the manifest.
- A check of the 2013-era support library's behaviour under these changes.

**C2. Upgrading to 604 deletes every user's games and statistics.** In all three adapters
the condition was changed from `oldVersion < 432/433/438` to `oldVersion < 604`, for example
`GridDatabaseAdapter.java:132`. Each `upgrade` then drops and recreates its table. **Dex** confirms
this is in the shipped APK.

Anyone last running a version below 604 lost their puzzles, archive and statistics on first open
after updating. Preferences survive, so the archive stays unlocked but empty.

**Updated 2026-10-31:** the maintainer recalls the old database being incompatible, and the
timeline supports the wipe being a deliberate response to an upgrade test from 2.12. However, the
schema is identical from 2.0.0 to today and no save-format change was found. A device test then
upgraded a real 2.12 database cleanly with the old thresholds, so no fault was reproduced. The
earlier advice to restore them is **withdrawn**: nearly all users are past the wipe, so the
thresholds stay at 604. See [release history](release-history-2024.md).

### High

**H1. The shipped source is not committed.** The working tree carries the 604 changes: version
bump, target 34, Java 17, `PRODUCTION` mode, the upgrade change, and the deletion of a stray
`AndroidManifest.xml~`. Commit `153c653`, "Update version for new release", does not contain the
release itself. As a result, a fresh clone and every CI build are in `DEVELOPMENT` mode with the
destructive developer menu. No source or config file has changed since the APK was exported on
2024-09-24, so the working tree is the 604 source. Commit it as it stands. The source of 600 and
602 no longer exists.

**H2. No automated tests.** The generator, solver, cage arithmetic, definition parsing and
storage format are pure enough to test, and they are what a modernisation is most likely to break.
Android classes (`Log`, `AsyncTask`, singletons) are the main obstacle, and `testInstrumentationRunner`
still names a removed runner.

### Medium

**M1. Cancelling generation can crash the app.** `selectRandomCageType` returns null when
cancelled (`GridGenerator.java:663`), but `createCages` dereferences the result at once
(`:584`). The user cancels with the back button, and an exception in `doInBackground` ends the process.
The window is limited to cage creation. `DLX.search` has no cancellation check, so a cancel during
solving is only noticed when it returns. **Read.**

**M2. Malformed share links can crash the app.** `SharedPuzzleActivity` is exported and handles
external URLs, but parsing is unguarded. Non-numeric hash or complexity fields throw
`NumberFormatException` (`SharedPuzzle.java:194`, `Grid.java:1283`). A cage ID with no matching cage
throws `IndexOutOfBoundsException` (`Grid.java:1344`; the null check below it can never fire).
Short cage fields and empty cages throw too. Hash-checked input can still be crafted. Impact
is a crash only. `Grid.load` also runs the solver on the UI thread, so a hard puzzle can stall it.

**M3. Hidden-operator puzzles lose behaviour when shared.** `Grid.load(String)` calls
`setCageResults(..., false)` and leaves `mHideOperators` false, so cages become "operator
visible, action none". `checkCageMathsCorrect` then has no matching case, and wrong cage maths are
never flagged. Reveal-operator is unavailable because the cage reports nothing hidden, and the
`hide_operators` column is stored wrongly. **Read.** (`showBadCageMath`'s "unreachable" `throw`
looks reachable from this state, but that method has no callers.)

**M4. Foreign keys are never enforced.** `PRAGMA foreign_keys=ON` is run once, in `onCreate`
(`DatabaseHelper.java:175`). That callback runs inside a transaction, where SQLite ignores this
pragma, and the setting is per connection anyway. The declared foreign keys do nothing. Switching
them on now needs care, since existing data may contain orphans.

**M5. The database version follows versionCode.** Every release is a schema "upgrade", and
installing a lower versionCode fails because `onDowngrade` is not overridden. This matters for
development and internal testing. Migrations would be safer on an explicit schema version.

**M6. Share links probably do not open the app.** The intent filter uses plain `http` without
`autoVerify`. On Android 12 and later, unverified web links open in the browser by default.
The maintainer still owns both domains, so this can be fixed with an `https` filter, `autoVerify`
and a Digital Asset Links file on `mathdoku.net`.

**M7. An exported provider exposes feedback files to all apps.** `util/FileProvider` is exported
with no permission and serves device info, preferences and screenshots from the files directory to
any installed app that asks, if a feedback file exists. Its comment acknowledges this. Use
`androidx.core.content.FileProvider` with temporary URI grants.

**M8. Hidden-operator games are duplicated, and never count as solved.** Confirmed on a device
in both 2.12 and the 604 source. `GridDatabaseAdapter.insert` stores `hide_operators` from a
boolean, so SQLite holds `1` or `0`. `valueOfSQLiteBoolean` then compares it with `"true"`
(`DatabaseAdapter.java:208`), so it always reads as false. The next `loadStatistics` builds a
definition containing the real operators, finds no match, and inserts a second `grid` row and
a `statistics` row with `replay = -1` and `include_in_statistics` false. All progress goes to that
row, while the empty original is the one statistics counts. A finished hidden-operator game
therefore shows as unfinished forever. The duplicate's definition also exposes the operators, so
sharing such a game after reloading it may reveal them (**Unverified**). The fix should accept the
stored integers, and a migration should merge existing duplicates.

### Low

- **Development code ships in release builds.** `DevelopmentHelper` claims that `mMode` checks are
  removed at compile time, but an enum comparison is not a Java constant expression, and shrinking
  is off. **Dex:** the `DEVELOPMENT` references are still present.
- **BugSense is dead weight.** Only an unused API key references it, yet 32 of its classes are in
  the APK (**Dex**). The app has no `INTERNET` permission, so it could never have reported anything.
  Remove the jar and the key.
- **Random difficulty is uneven.** `nextInt(6)` gives 0 to 5 and both 0 and 1 map to Very easy, so
  it gets double the odds (`PuzzleFragmentActivity.java:948`). Dragging the stars to zero also
  counts as Random.
- **`restartLastGame` ignores `load`'s return value** (`:795`), so a failed load carries on.
- **`InvalidGridException` extends `ExceptionInInitializerError`,** so it is an `Error`, not an
  `Exception`.
- **Javadoc on `isGeneratedBefore` states the opposite of what it returns**
  (`GridGenerator.java:1191`).
- **`String.format("%02d")` uses the default locale** in grid definitions (`Grid.java:564`). Locales with
  non-ASCII digits could yield links that the `\d\d` parser rejects. **Unverified.**
- **Hardcoded English:** difficulty labels and the "Undo." toast.
- **`GameFileConverter` hardcodes `/data/data/net.cactii.mathdoku/`,** which is wrong for
  secondary users. It only affects cleanup of very old files.

### Lint

**Built:** `lintDebug` reports 185 issues. The errors seen among the most common entries:

- 17 `MissingTranslation`, mostly digit-button labels and untranslatable values. `de` and `nl` each
  have 209 strings against 224 in English.
- 3 `StringFormatMatches` in `GridPlayerView`. Line 479 passes an unused argument to a string with no
  placeholder, which is harmless. Line 152 was flagged for argument type and was not investigated further.
- 1 `NotificationPermission`. The app does not appear to post notifications, so this likely
  comes from a bundled jar. **Unverified.**

The largest warning groups are `RtlHardcoded` (67), `SetTextI18n` (23) and `UnusedResources` (15).

### Deprecated platform use

All of this still works at target 34, but a modernisation will touch it: `AsyncTask` (3 files),
`ProgressDialog`, `PreferenceActivity`/`PreferenceFragment`, `android.support.v4` (instead of
AndroidX), `FLAG_FULLSCREEN`, framework `ActionBar`, `onRetainCustomNonConfigurationInstance`,
Holo themes, and AChartEngine, which is no longer maintained.

## What is good

- The solver and generator are compact and independent of most of the UI, so they can be
  extracted and tested.
- Seeds and generator revisions are stored for every puzzle, which makes reproduction possible.
- Comments are generous and explain why, including the Android bugs being worked around.
- `Grid.insertInDatabase` handles failure with real transactions.
- The privacy position is strong and simple: no permissions, no analytics, and feedback only
  through the user's own email client.

## Recommended order of work

1. **Establish the baseline.** Commit the working tree so that `master` matches 604. Make the
   build mode depend on the build type (`BuildConfig.DEBUG`) so the manual edit disappears, and
   pin the JDK with a Gradle toolchain.
2. **Check the outside world.** Play Console policy status for 604.
3. **Add a safety net.** Unit tests for the solver, cage arithmetic, generator determinism (seed
   to puzzle) and round trips of definitions and storage strings.
4. **Fix the small bugs:** M8, M1, M2, M3 and the BugSense removal.
5. **Modernise for API 36.** Confirmed as a main goal by the maintainer. Migrate to AndroidX, then
   raise the target and handle insets and back. This is the largest item, and each step should be
   tested on a device.
6. **Tidy up as needed.** Replace `AsyncTask`, the provider and the chart library, turn on R8, and
   separate the schema version from versionCode.

## Open questions for the maintainer

- What failed when upgrading from 2.12 in September 2024? A device test did not reproduce it.
- Why did the target go from 35 back to 34? Does Play Console show a message or rejection for 601?
- What is the current Play Console policy status, and is there an API level extension?

Answered 2026-10-31: both domains are still owned, and the release history is in
[release-history-2024.md](release-history-2024.md).
