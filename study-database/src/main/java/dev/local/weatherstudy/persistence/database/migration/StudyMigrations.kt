package dev.local.weatherstudy.persistence.database.migration

import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to the 27 classes in
 * com.samsung.android.weather.persistence.database.migration:
 * `AutoMigration1502to1503`, `AutoMigration1503to1600`, `AutoMigration1601to1602`, …
 * `AutoMigration1628to1629`
 *
 * ### What the original does, and what is preserved
 *
 * The schema has **51 exported versions (v900 → v1629)**. The migration strategy
 * changed partway through, and the change is visible in the decompiled output:
 *
 * - **below 1502** — no `AutoMigration*` class exists, so those steps were
 *   hand-written `Migration` subclasses. Their SQL is not recoverable from the
 *   exported schemas alone, and it is not reconstructed.
 * - **1502 → 1629** — 27 `AutoMigration<from>to<to>` classes, each a Room
 *   `AutoMigrationSpec`. Room generates the ALTER statements by diffing the two
 *   exported schemas; the spec class exists only to carry annotations for the cases
 *   Room cannot infer (column renames and deletes) and to run `onPostMigrate` work.
 * - note the **gap at 1618→1619**: there is an `AutoMigration1617to1618` and an
 *   `AutoMigration1619to1620` but nothing between them, so that one step was handled
 *   another way — a destructive migration or a hand-written one.
 *
 * Two representative specs are wired into [dev.local.weatherstudy.persistence.database.StudyWeatherDatabase];
 * the rest are summarised in `reports/room-schema-analysis.md` as the project's STEP 10
 * explicitly permits. The architecture being preserved is "auto-migrations with spec
 * classes, on a separately versioned database module", not 51 individual column adds.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
class StudyAutoMigration1627to1628 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        // Room has already applied the generated schema diff by this point.
        // The original uses this hook for data repair that a diff cannot express.
    }
}

/** Corresponds conceptually to `…migration.AutoMigration1628to1629` — the current head. */
class StudyAutoMigration1628to1629 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) = Unit
}

/**
 * Reconstruction note, not a class in the original.
 *
 * The full version ladder recovered from `assets/com.samsung.android.weather.persistence
 * .database.WeatherDatabase/`:
 *
 * ```
 * 900  910  920  930  940  950  951  960  970  971  972  973  974  975  976
 * 1000 1001
 * 1500 1501 1502 1503
 * 1600 1601 1602 1603 1604 1605 1606 1607 1608 1609 1610 1611 1612 1613 1614
 * 1615 1616 1617 1618 1619 1620 1621 1622 1623 1624 1625 1626 1627 1628 1629
 * ```
 *
 * The numbering is not sequential: it tracks the app's own release series (9.x, 10.x,
 * 15.x, 16.x), which is why the gaps are where they are.
 */
object StudySchemaVersionLadder {
    val VERSIONS = listOf(
        900, 910, 920, 930, 940, 950, 951, 960, 970, 971, 972, 973, 974, 975, 976,
        1000, 1001,
        1500, 1501, 1502, 1503,
        1600, 1601, 1602, 1603, 1604, 1605, 1606, 1607, 1608, 1609, 1610, 1611, 1612,
        1613, 1614, 1615, 1616, 1617, 1618, 1619, 1620, 1621, 1622, 1623, 1624, 1625,
        1626, 1627, 1628, 1629,
    )

    /** the step with no AutoMigration class in the original */
    val STEP_WITHOUT_AUTO_MIGRATION = 1618 to 1619

    /** auto-migrations begin here; everything below was hand-written */
    const val FIRST_AUTO_MIGRATED_VERSION = 1502
}
