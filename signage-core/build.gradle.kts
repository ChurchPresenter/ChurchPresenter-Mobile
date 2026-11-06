// The signage players' wire types and the decision of what a screen shows.
//
// A copy of ChurchPresenter-Signage's :core, kept to its commit by hand, as the
// desktop keeps its own (see README.md here). Change it there first.
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "2.3.0"
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
}

dependencies {
    implementation(libs.kotlinx.serialization)
}
