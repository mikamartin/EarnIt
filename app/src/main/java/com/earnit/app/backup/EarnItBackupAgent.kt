package com.earnit.app.backup

import android.app.backup.BackupAgent
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.app.backup.FullBackupDataOutput
import android.os.ParcelFileDescriptor
import com.earnit.app.data.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

// allowBackup is a static manifest flag the OS reads once at install time, so the
// Settings > Data & Backup toggle is enforced here instead: skip the full backup pass
// entirely when the user has opted out. Governs both cloud Auto Backup and device-to-device
// transfer, since both go through onFullBackup on API 31+.
class EarnItBackupAgent : BackupAgent() {
    override fun onFullBackup(data: FullBackupDataOutput) {
        val backupEnabled = runBlocking { SettingsRepository(applicationContext).settings.first().cloudBackupEnabled }
        if (backupEnabled) {
            super.onFullBackup(data)
        }
    }

    // Unused: the app relies on full-data Auto Backup (onFullBackup) rather than key/value backup.
    override fun onBackup(
        oldState: ParcelFileDescriptor?,
        data: BackupDataOutput?,
        newState: ParcelFileDescriptor?,
    ) = Unit

    override fun onRestore(
        data: BackupDataInput?,
        appVersionCode: Int,
        newState: ParcelFileDescriptor?,
    ) = Unit
}
