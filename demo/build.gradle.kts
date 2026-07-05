@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.androidApplication)
  alias(libs.plugins.composeMultiplatform)
  alias(libs.plugins.composeCompiler)
}

kotlin {
  androidTarget()
  jvm("desktop")
  listOf(iosArm64(), iosSimulatorArm64()).forEach {
    it.binaries.framework {
      baseName = "demo"
      isStatic = true
    }
  }
  wasmJs {
    browser()
    binaries.executable()
  }
  js(IR) {
    browser()
    binaries.executable()
  }

  applyDefaultHierarchyTemplate()

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(project(":spooler"))
        implementation(compose.runtime)
        implementation(compose.foundation)
        implementation(compose.material3)
        implementation(libs.androidx.lifecycle.runtimeCompose)
      }
    }
    val commonTest by getting { dependencies { implementation(libs.kotlin.test) } }
    val androidMain by getting { dependencies { implementation(libs.androidx.activity.compose) } }
    val desktopMain by getting { dependencies { implementation(compose.desktop.currentOs) } }
  }
}

android {
  namespace = "io.spooler.demo"
  compileSdk = libs.versions.android.compileSdk.get().toInt()
  defaultConfig {
    applicationId = "io.spooler.demo"
    minSdk = libs.versions.android.minSdk.get().toInt()
    targetSdk = libs.versions.android.compileSdk.get().toInt()
    versionCode = 1
    versionName = "1.0.0-alpha01"
  }
}

compose.desktop { application { mainClass = "io.spooler.demo.MainKt" } }
