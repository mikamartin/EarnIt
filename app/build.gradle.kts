import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

val keystoreProps =
    Properties().also { props ->
        val propsFile = rootProject.file("keystore.properties")
        if (propsFile.exists()) props.load(propsFile.inputStream())
    }

android {
    namespace = "com.earnit.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.secondmonday.earnit"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "com.earnit.app.HiltTestRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file(keystoreProps["storeFile"] ?: "KEYSTORE_NOT_CONFIGURED")
            storePassword = keystoreProps["storePassword"] as String? ?: ""
            keyAlias = keystoreProps["keyAlias"] as String? ?: ""
            keyPassword = keystoreProps["keyPassword"] as String? ?: ""
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release")

            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes +=
                setOf("META-INF/DEPENDENCIES", "META-INF/LICENSE", "META-INF/LICENSE.txt", "META-INF/license.txt")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    // KSP2 + Kotlin 2.3.x metadata bug: force kotlin-metadata-jvm to match Kotlin version
    ksp(libs.kotlin.metadata.jvm)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.androidx.glance)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.moshi)
    ksp(libs.moshi.kotlin.codegen)
    implementation(libs.moshi.kotlin)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.play.review.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.androidx.glance.testing)
    testImplementation(libs.androidx.glance.appwidget.testing)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.android.compiler)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// Scans androidTest source text for the required tags, so it runs as part of `check`
// alongside unit tests.
tasks.register("checkInstrumentedTestTags") {
    group = "verification"
    description = "Fails if any androidTest class is missing a required layer tag or an optional tag."

    val testDir = layout.projectDirectory.dir("src/androidTest/java")
    inputs.dir(testDir)

    doLast {
        val requiredTags = listOf("@RepositoryTest", "@UtilityTest", "@UiTest")
        val optionalTags =
            listOf(
                "@Smoke",
                "@Task",
                "@Reward",
                "@Settings",
                "@Widget",
                "@Nudge",
                "@ImportExport",
                "@CleanUp",
            )

        val missingRequired = mutableListOf<String>()
        val missingOptional = mutableListOf<String>()

        testDir.asFile
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                val text = file.readText()
                if (!Regex("@Test(?![A-Za-z0-9_])").containsMatchIn(text)) return@forEach

                if (requiredTags.none { text.contains(it) }) missingRequired += file.name
                if (optionalTags.none { text.contains(it) }) missingOptional += file.name
            }

        if (missingRequired.isNotEmpty() || missingOptional.isNotEmpty()) {
            val message = StringBuilder("Instrumented test tag check failed.\n")
            if (missingRequired.isNotEmpty()) {
                message.append(
                    "Missing a required layer tag (@RepositoryTest / @UtilityTest / @UiTest): " +
                        "${missingRequired.joinToString()}\n",
                )
            }
            if (missingOptional.isNotEmpty()) {
                message.append(
                    "Missing an optional tag (@Smoke / @Task / @Reward / @Settings / @Widget / " +
                        "@Nudge / @ImportExport / @CleanUp): ${missingOptional.joinToString()}\n",
                )
            }
            throw GradleException(message.toString())
        }
    }
}

tasks.named("check") {
    dependsOn("checkInstrumentedTestTags")
}
