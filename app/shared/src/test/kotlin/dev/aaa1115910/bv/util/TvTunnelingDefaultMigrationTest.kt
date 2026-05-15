package dev.aaa1115910.bv.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TvTunnelingDefaultMigrationTest {
    @Test
    fun newInstallWithoutSavedSettingWritesDisabled() {
        val valueToWrite = TvTunnelingDefaultMigration.valueToWrite(
            lastVersionCode = 0,
            migrationDone = false,
            hasTvTunnelingPreference = false
        )

        assertEquals(false, valueToWrite)
    }

    @Test
    fun oldUserWithoutSavedSettingWritesEnabled() {
        val valueToWrite = TvTunnelingDefaultMigration.valueToWrite(
            lastVersionCode = 1,
            migrationDone = false,
            hasTvTunnelingPreference = false
        )

        assertEquals(true, valueToWrite)
    }

    @Test
    fun existingEnabledSettingIsPreserved() {
        val valueToWrite = TvTunnelingDefaultMigration.valueToWrite(
            lastVersionCode = 1,
            migrationDone = false,
            hasTvTunnelingPreference = true
        )

        assertNull(valueToWrite)
    }

    @Test
    fun existingDisabledSettingIsPreserved() {
        val valueToWrite = TvTunnelingDefaultMigration.valueToWrite(
            lastVersionCode = 1,
            migrationDone = false,
            hasTvTunnelingPreference = true
        )

        assertNull(valueToWrite)
    }

    @Test
    fun completedMigrationDoesNotRepeat() {
        val valueToWrite = TvTunnelingDefaultMigration.valueToWrite(
            lastVersionCode = 1,
            migrationDone = true,
            hasTvTunnelingPreference = false
        )

        assertNull(valueToWrite)
    }
}
