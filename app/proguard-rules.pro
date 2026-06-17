# Default Android optimisation rules are merged in via proguard-android-optimize.txt.
# This file covers app-specific and library gaps not handled by consumer rules.

# Kotlin metadata — required for reflection-free Kotlin to work correctly after shrinking
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Kotlin coroutines — keep internal debug infrastructure from being confused by R8
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Room — entity and DAO classes are accessed by generated code; keep their members
-keep class com.earnit.app.data.*Entity { *; }
-keep class com.earnit.app.data.*Dao { *; }
-keep class com.earnit.app.data.*Dao_Impl { *; }

# Moshi — KSP codegen generates its own rules for EarnItExport and its adapter,
# but keep the nested data classes used in the export JSON tree
-keep class com.earnit.app.data.EarnItExport { *; }
-keep class com.earnit.app.data.EarnItExport$* { *; }

# Hilt — generated components; Hilt's own consumer rules cover most cases,
# but keep the application entry point explicitly
-keep class com.earnit.app.EarnItApplication { *; }
-keep class * extends dagger.hilt.android.internal.managers.ActivityComponentManager { *; }

# Glance widget — receiver and activities must survive shrinking
-keep class com.earnit.app.widget.EarnItGlanceWidgetReceiver { *; }
-keep class com.earnit.app.widget.WidgetConfigActivity { *; }
-keep class com.earnit.app.widget.WidgetTaskLogActivity { *; }

# Navigation Compose — destination route strings referenced by name at runtime
-keepnames class com.earnit.app.** implements java.io.Serializable

# DataStore — proto/preferences internals
-keep class androidx.datastore.** { *; }
