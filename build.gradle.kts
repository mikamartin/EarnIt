// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    dependencies {
        // Override AGP 9's bundled KGP (2.2.10) so all Kotlin plugins use the version pinned in
        // gradle/libs.versions.toml. Version catalog accessors aren't available in this block, so
        // this stays a literal — keep it in sync with the `kotlin` entry in the catalog.
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.20")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    // kotlin.android removed — AGP 9 provides built-in Kotlin compilation;
    // version is overridden via buildscript classpath above
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.ktlint)
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
}

tasks.register("printGradleVersion") {
    doLast {
        println("Gradle version: ${gradle.gradleVersion}")
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
