// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // AGP 9 bundles Kotlin 2.2.x. Override it with a version that understands JDK 25.
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.21")
        classpath("com.google.devtools.ksp:symbol-processing-gradle-plugin:2.3.14")
    }
}

plugins {
    id("com.android.application") version "9.5.2" apply false
    id("com.google.devtools.ksp") version "2.3.14" apply false
}
