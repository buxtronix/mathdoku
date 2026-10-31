# The 2024 release: what happened

Reconstructed 2026-10-31 from the Play Console release list, the Android Studio log
(`~/.cache/Google/AndroidStudio2024.1/log/idea.log`), file timestamps, and disassembly of old
APKs kept on the development machine. Inferences are marked as such.

## Summary

- **Five uploads in nine days** took the app from 2.12 (599, built 2013) to 2.14 (604).
- **604 is exactly the working tree.** No source or config file has changed since it was exported.
- **The target dropped to 34 minutes after 601 was uploaded**, before any device test of that
  change. The reason was not recorded.
- **The data wipe followed an upgrade test from 2.12**, which fits the maintainer's memory that the
  old database was incompatible. But **the schema and save format are unchanged**, so the actual
  fault is still unknown.
- **A device test could not reproduce any failure:** a real 2.12 database upgraded cleanly with the
  old thresholds. The wipe stays in place, because removing it now would help few people.

## Timeline

All times are AEST (UTC+10). Play Console times match Studio's log only when read as UTC, and they
then match all five uploads to the minute.

| When | Event | Play |
| --- | --- | --- |
| 2013-09-08 | 2.0.0 uploaded; 2.0.1 followed on 09-11 | **569**, **575** |
| 2013-10-22 | 2.1 uploaded | **592** |
| 2013-11-02 | 2.11 uploaded (Play: Nov 1, 23:35 UTC) | **598** |
| 2013-11-05 | 2.12 uploaded (Play: Nov 4, 21:15 UTC), target 17 | **599** |
| 2018-06 | Code imported to git; gradle says 600 / 2.12, manifest says 600 / 2.13 | |
| 2024-09-15 11:41 | Old checkout opened in Studio; the work moves to `/tmp/mathdoku` | |
| 2024-09-15 18:24 | Signed APK exported from `/tmp/mathdoku` | **600** (2.12) |
| 2024-09-15 18:31 | Signed APK exported from `/tmp/mathdoku` | **602** (2.13) |
| 2024-09-17 17:08 | PR #1 merged; this repo cloned at 17:10 | |
| 2024-09-17 17:21 | Commit `153c653`, target 35, 2.13 | |
| 2024-09-17 17:28 | Debug build run on the Pixel 6, target 35 | |
| 2024-09-17 18:09 | Signed APK exported, still target 35 | **601** (2.13) |
| 2024-09-17 18:16 | SDK Platform 34 installed | |
| 2024-09-17 18:18 | Project synced on API 34; changelog changed to "SDK 34"; run on Pixel 6 | |
| 2024-09-17 18:21 | Signed APK exported | **603** (2.13) |
| 2024-09-21 17:36 | `~/Downloads/599.apk` saved, byte-identical to 2.12; Pixel 6 attached at 17:41 | |
| 2024-09-22 09:29 | `SolvingAttemptDatabaseAdapter` edited | |
| 2024-09-22 15:55 | Version set to 604 / 2.14 | |
| 2024-09-22 15:56 | Other two adapters edited; a debug build fails | |
| 2024-09-22 16:05, 16:07 | Debug installs fail: `INSTALL_FAILED_UPDATE_INCOMPATIBLE` (signature mismatch) | |
| 2024-09-22 16:06 | Signed APK exported | |
| 2024-09-22 16:13 | `mMode` set to `PRODUCTION`; last source edit | |
| 2024-09-24 08:52 | Signed APK exported | **604** (2.14) |

Play lists 4.09K for 604, 2.13K for 599, and at most 100 for every other version. All except 604
are inactive. Nothing was uploaded between 599 and 600, so 2.12 was the live version for nearly 11
years, and it is the upgrade path that matters.

## The database wipe

**What the maintainer recalls:** the old database was not backwards compatible.

**What supports that:** the 599 APK was fetched on the evening of 21 September and the upgrade code
was edited the next morning. Together with the `DROP` logging added at 15:58, this looks like
an upgrade from 2.12 being tested, something going wrong, and the tables being reset to fix it. This is
inference: no app logcat survives, and Studio's log shows nothing from the app itself.

**What was ruled out**, by comparing the old APKs with the current source:

| Possible cause | Result |
| --- | --- |
| Table definitions changed | No. The `buildCreateSQL` strings match the source in 2.0.0, 2.0.1, 2.1rc1, 2.1 and 2.12 |
| A migration was missing | No. Upgrade thresholds are the same in all of them, and git shows no schema change since 2018 |
| `CellChange` format changed | No. A difference in the disassembly was compiler output; the format and regex match |
| Cell, cage or grid save lines changed | Not found. The current writers match; the one new field (`mMaybeMode`) is optional when read |
| Complexity values changed | No. The `PuzzleComplexity` names are identical |
| Development-mode "Database is inconsistent" dialog | No. It compares `CREATE` text, and that text is identical |

The cell and cage comparison used the current source and `CellChange` used both disassemblies, so a subtle
difference in the 2.12 readers or query code is not fully excluded.

**What remains possible:** something specific to a long-lived real database or the device used
in 2024. The later device test ruled out the simple cases.

**Consequence:** almost every update to 604 came straight from 599, the version with 2.13K
installs. Anyone on it who updated lost their games and statistics. How many of those were still
active in 2024 is unknown, and Play's figures do not show it.

### Upgrade test method

1. Install 2.12 on a test device and create a few games.
2. Update to a 604 build with the old thresholds restored, signed with the same key, and capture
   logcat. Try `DEVELOPMENT` and `PRODUCTION` builds, as they behave differently.

Android 14 and later refuse low-target installs, so 2.12 (target 17) needs
`adb install --bypass-low-target-sdk-block`, or an older device. No emulator images are installed
on this machine. This replaces the app's data on the device, so use a device set aside for it.

### Result, 2026-10-31: the failure did not reproduce

Run on a USB Pixel 5 with Android 14, after the maintainer allowed the install in Play Protect. Both
builds were signed with the debug key, and the test app was removed afterwards.

1. Installed the real 2.12 (599) and created two games: one solved, and one hidden-operator game in
   progress with digits, pencil marks, flagged errors and undo history.
2. Updated in place to the 604 source with the old thresholds, in `PRODUCTION` mode.
3. Updated again to the same source in `DEVELOPMENT` mode. This was versionCode 605, so it ran
   the upgrade path over the same database, but from 604 rather than from 599.
4. Used the restored game: undo, finishing it, the archive view and statistics.

| Check | Result |
| --- | --- |
| Crashes | None, in any step |
| Tables after upgrade | Kept, with 2.12's rows intact; `integrity_check` ok |
| In-progress game | Restored exactly, including pencil marks, highlights, selection and timer |
| Undo history from 2.12 | Worked |
| "Database is inconsistent" dialog | Did not appear |
| Statistics screen | Opened and read the data |

So with this data, on this device, **the old thresholds work and the wipe was not needed.**

This does not prove that nothing went wrong in 2024. The test could not cover:

- a database built up over years, or created on a 2013-era device and restored from backup
- a large history (the converter rewrites every attempt)
- Android 15, or the Pixel 6 of the time
- an update through Play, or a build other than this source

The test did find a real bug that exists in 2.12 as well, recorded as M8 in the
[review](project-review.md). It affects hidden-operator games only, and it may or may not be what was
seen in 2024.

**Decision:** leave the thresholds at 604. Nearly all users have already passed through the wipe, so
changing them now would help few people and has an unproven benefit. Future schema changes need real
migrations (see M5).

## The target SDK drop

The change came six minutes after 601 went up, with no build or device run in between. The same
Studio session then installed Platform 34, which was not previously on the machine. So this was
a deliberate choice, and it was probably prompted by something outside the app, such as a Play
Console message. **That part is inference.** Android 15 had not reached devices yet, and 34 already
met that year's requirement. Nothing connects it to a runtime problem such as edge-to-edge, so the
review's suggestion to that effect is weakened. It is still unknown how the app behaves at 35 or later.

## Other things this explains

- **600 to 603 probably shipped in `DEVELOPMENT` mode.** The commit has that mode and the switch
  to `PRODUCTION` was only made on 22 September. Its file's timestamp shows the last edit only, so an
  earlier temporary change cannot be excluded. If so, about 100 users got the developer menu, including
  delete database, and deliberate crashes on slow generation.
- **The source of 600 and 602 is lost.** They were built from `/tmp/mathdoku`, which no longer exists.
- **Reused version names.** 599 and 600 are both 2.12. In 2013 that name went with 599, and the
  2018 gradle file kept it.
- **Upload order differs from version order** because 602 was uploaded two days before 601.
- **The test Pixel had a differently signed copy on 22 September,** hence the install failures.
  It may have been the 2.12 or Play release version. The 16:06 signed build may have been used
  to get around that, but no install of it was logged.
