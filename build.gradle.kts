// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    dependencies {
        // Override AGP 9's bundled KGP (2.2.10) so all Kotlin plugins use 2.3.20
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.20")
    }
}

plugins {
    id("com.android.application") version "9.2.0" apply false
    id("com.android.library") version "9.2.0" apply false
    // kotlin.android removed — AGP 9 provides built-in Kotlin compilation;
    // version is overridden to 2.3.20 via buildscript classpath above
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.20" apply false
    id("com.google.dagger.hilt.android") version "2.59.2" apply false
    id("com.google.devtools.ksp") version "2.3.9" apply false
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
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
