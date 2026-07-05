@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.androidMultiplatformLibrary)
  alias(libs.plugins.mavenPublish)
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
    val commonTest by getting { dependencies { implementation(libs.kotlin.test) } }
    val desktopMain by getting {
      dependencies {
        implementation(libs.kotlinx.coroutines.swing)
        implementation(libs.openhtmltopdf.pdfbox)
        implementation(libs.openhtmltopdf.svg.support)
      }
    }
    val androidMain by getting { dependencies { implementation(libs.kotlinx.coroutines.android) } }
    val webMain by getting { dependencies { implementation(libs.kotlinx.browser) } }
  }

  targets.withType<KotlinJvmTarget> { compilerOptions.jvmTarget.set(JvmTarget.JVM_17) }
}

mavenPublishing {
  publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = false)
  signAllPublications()
}
