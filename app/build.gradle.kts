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
        versionCode = 5
        versionName = "1.3.1"

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
    description =
        "Fails if any androidTest class is missing a required layer tag, carries more than one, " +
        "or is missing an optional tag."

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

        // Strips comments first so a tag merely named in a KDoc block can't satisfy the check.
        fun stripComments(text: String): String =
            text
                .replace(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL), "")
                .replace(Regex("""//[^\n]*"""), "")

        val classRegex = Regex("""\bclass\s+(\w+)""")
        val annotationLineRegex = Regex("""^\s*@[\w.]+(\([^)]*\))?\s*$""")

        val missingRequired = mutableListOf<String>()
        val duplicateRequired = mutableListOf<String>()
        val missingOptional = mutableListOf<String>()

        testDir.asFile
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                val text = stripComments(file.readText())

                classRegex.findAll(text).forEach { match ->
                    val className = match.groupValues[1]
                    val headerEnd = match.range.first

                    // The class body: from its opening brace to the matching close, so a
                    // sibling or nested class's text can't leak into this class's checks.
                    val bodyStart = text.indexOf('{', headerEnd)
                    if (bodyStart == -1) return@forEach
                    var depth = 0
                    var bodyEnd = -1
                    for (i in bodyStart until text.length) {
                        when (text[i]) {
                            '{' -> depth++
                            '}' -> {
                                depth--
                                if (depth == 0) {
                                    bodyEnd = i
                                    break
                                }
                            }
                        }
                    }
                    if (bodyEnd == -1) return@forEach
                    val body = text.substring(bodyStart, bodyEnd + 1)
                    if (!Regex("@Test(?![A-Za-z0-9_])").containsMatchIn(body)) return@forEach

                    // The annotation header: the contiguous run of @Annotation lines directly
                    // above `class`, e.g. `@UiTest` / `@CleanUp` / `@RunWith(...)` / `class Foo {`.
                    val linesBefore = text.substring(0, headerEnd).lines().dropLast(1)
                    val header =
                        linesBefore
                            .asReversed()
                            .takeWhile { it.isBlank() || annotationLineRegex.matches(it) }
                            .joinToString("\n")

                    val label = "${file.name}: $className"
                    val matchedRequired = requiredTags.filter { header.contains(it) }
                    when {
                        matchedRequired.isEmpty() -> missingRequired += label
                        matchedRequired.size > 1 -> duplicateRequired += label
                    }
                    if (optionalTags.none { header.contains(it) }) missingOptional += label
                }
            }

        if (missingRequired.isNotEmpty() || duplicateRequired.isNotEmpty() || missingOptional.isNotEmpty()) {
            val message = StringBuilder("Instrumented test tag check failed.\n")
            if (missingRequired.isNotEmpty()) {
                message.append(
                    "Missing a required layer tag (@RepositoryTest / @UtilityTest / @UiTest): " +
                        "${missingRequired.joinToString()}\n",
                )
            }
            if (duplicateRequired.isNotEmpty()) {
                message.append(
                    "More than one required layer tag (@RepositoryTest / @UtilityTest / @UiTest — " +
                        "exactly one allowed): ${duplicateRequired.joinToString()}\n",
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
