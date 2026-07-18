package com.earnit.app.tags

/**
 * Required on every instrumented test class — exactly one. Drives CI sharding
 * (`instrumented-tests.yml` filters by these via `-e annotation=`) and is enforced by the
 * `checkInstrumentedTestTags` Gradle task. See TESTING.md's "Tagging convention" section.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class RepositoryTest

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class UtilityTest

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class UiTest
