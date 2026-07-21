@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.androidMultiplatformLibrary)
  alias(libs.plugins.mavenPublish)
  signing
}

kotlin {
  jvm("desktop")

  androidLibrary {
    namespace = "io.spooler.core"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    minSdk = libs.versions.android.minSdk.get().toInt()
  }

  listOf(iosArm64(), iosSimulatorArm64()).forEach { it.binaries.framework { baseName = "spooler" } }

  wasmJs { browser() }
  js(IR) { browser() }

  applyDefaultHierarchyTemplate()

  compilerOptions { freeCompilerArgs.add("-Xexpect-actual-classes") }

  sourceSets {
    val commonMain by getting { dependencies { implementation(libs.kotlinx.coroutines.core) } }
    val nonWebMain by creating {
      dependsOn(commonMain)
      dependencies { implementation(libs.ktor.network) }
    }
    androidMain.get().dependsOn(nonWebMain)
    iosMain.get().dependsOn(nonWebMain)
    val commonTest by getting { dependencies { implementation(libs.kotlin.test) } }
    val desktopMain by getting {
      dependsOn(nonWebMain)
      dependencies {
        implementation(libs.kotlinx.coroutines.swing)
        implementation(libs.openhtmltopdf.pdfbox)
        implementation(libs.openhtmltopdf.svg.support)
      }
    }
    val desktopTest by getting { dependencies { implementation(libs.kotlinx.coroutines.test) } }
    val androidMain by getting { dependencies { implementation(libs.kotlinx.coroutines.android) } }
    val webMain by getting { dependencies { implementation(libs.kotlinx.browser) } }
  }

  targets.withType<KotlinJvmTarget> { compilerOptions.jvmTarget.set(JvmTarget.JVM_17) }
}

signing { isRequired = true }

gradle.taskGraph.whenReady {
  if (allTasks.any { it.name.contains("MavenLocal", ignoreCase = true) }) {
    tasks.withType<Sign>().configureEach { isEnabled = false }
  }
}

mavenPublishing {
  publishToMavenCentral()
  signAllPublications()
}
