# Journal

Newest entries first. Durable findings belong in their own documents; this file records what
was done, what was decided and what is still open.

## Documents

- [Project review](project-review.md): architecture, findings and recommended order of work.
- [2024 release history](release-history-2024.md): timeline of 600 to 604, the data wipe and the target change.

## 2026-10-31: Baseline committed

- Committed the 604 source unchanged on branch `baseline-604`, followed by these docs. Not pushed
  or merged to `master`.
- Next: build mode from build type, unit tests, M8, then AndroidX and API 36.

## 2026-10-31: Upgrade test finished

- Upgraded a real 2.12 database to the 604 source with the old thresholds on the USB Pixel 5, in
  `PRODUCTION` then `DEVELOPMENT` mode. No crash, no data loss. Undo, finishing a game, and statistics
  all worked, so the 2024 failure was not reproduced. Details are in
  [release-history-2024.md](release-history-2024.md).
- Found bug M8: `hide_operators` is read back wrongly, so hidden-operator games are duplicated and
  never count as solved. It exists in 2.12 as well. Not fixed yet.
- Decided to leave the thresholds at 604.
- Removed the test app from the device. No repo source was changed; the test builds lived in the
  scratch directory.

## 2026-10-31: Upgrade test started; API 36 confirmed as a goal

- The maintainer confirmed that API 36 work matters: a goal is to update the app for modern devices.
- The full Play history shows 2.12 (599) was live from 2013 to 2024, with 2.13K installs.
- Test device: USB Pixel 5, serial `13231FDD4004JG`, Android 14, Mathdoku not previously
  installed. The wireless Pixel 10 must not be used.
- Prepared 2.12 re-signed with the debug key, and a PRODUCTION debug build of the 604 source
  with the old upgrade thresholds. Both are scratch files; the repo is unchanged.
- Blocked: Play Protect rejected the 2.12 install. Nothing was installed and no settings were changed.

## 2026-10-31: Reconstructing the 2024 releases

**Done**

- Matched the maintainer's Play Console list against Studio's log, file timestamps and old APKs on
  this machine (read-only), and wrote [release-history-2024.md](release-history-2024.md).
- Corrected the review. No source files were changed.

**Decided**

- Withdrew the advice to restore the old upgrade thresholds. They stay at 604 until the
  2.12 upgrade failure has been reproduced.
- The working tree is the exact 604 source, so it should be committed unchanged as the baseline.

**Learned**

- The schema has been identical since 2.0.0, so the wipe was not caused by a schema change.
  The maintainer recalls an incompatibility, and the timeline fits, but the real fault is unknown.
- A `CellChange` format difference looked promising but was compiler output.
- The target drop to 34 came minutes after uploading 601, with no device test in between.
- 600 to 603 probably shipped in `DEVELOPMENT` mode.

**Open, for the maintainer**

- Any memory of the actual 2.12 upgrade failure, and any Play Console message about 601.
- Current Play policy status. Whether to commit the baseline, and whether to run the upgrade test.

## 2026-10-31: Initial project review

**Done**

- Reviewed the repo and wrote [project-review.md](project-review.md).
- Built and linted a scratch copy of the working tree: `assembleDebug` and `lintDebug` pass with
  `~/.jdks/jbr-21.0.11`, with 185 lint issues.
- Disassembled the shipped 2.14 (604) APK to compare it with the source.
- No source files were changed. The app was not run on a device, and `docs/` is not yet committed.

**Learned**

- Android Studio's bundled JBR is Java 25, which Gradle 8.10 cannot run. Use JBR 21.
- The uncommitted working tree appears to be the 604 release; `master` is not what shipped.
- The shipped APK wipes all user data when upgrading from below 604.
- Play now requires target API 36 for updates; the app targets 34.
- A suspected crash in `GridCage.showBadCageMath` on shared puzzles is not real, because the method
  has no callers. The related loss of behaviour is recorded as M3.

**Open, for the maintainer**

- Reason for the `oldVersion < 604` change, and whether to restore the old thresholds.
- Reason for dropping the target from 35 to 34.
- Play Console status of the app, and ownership of `mathdoku.net` and `mathdoku.org`.
