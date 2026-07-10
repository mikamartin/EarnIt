package com.earnit.app

import android.Manifest
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

class HiltTestRunner : AndroidJUnitRunner() {
    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        // MainActivity requests POST_NOTIFICATIONS on launch (Android 13+). Without pre-granting
        // it here, the real system permission dialog pops during instrumented UI tests and
        // breaks Compose's semantics tree lookup ("No compose hierarchies found in the app")
        // across nearly every test that launches MainActivity.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            uiAutomation.grantRuntimePermission(targetContext.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun newApplication(
        cl: ClassLoader?,
        name: String?,
        context: Context?,
    ): Application = super.newApplication(cl, HiltTestApplication::class.java.name, context)
}
