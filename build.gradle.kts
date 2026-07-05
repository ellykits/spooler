plugins {
  alias(libs.plugins.kotlinMultiplatform) apply false
  alias(libs.plugins.androidApplication) apply false
  alias(libs.plugins.androidMultiplatformLibrary) apply false
  alias(libs.plugins.composeMultiplatform) apply false
  alias(libs.plugins.composeCompiler) apply false
  alias(libs.plugins.mavenPublish) apply false
  alias(libs.plugins.spotless)
}

spotless {
  val ktlintOptions =
    mapOf(
      "indent_size" to "2",
      "continuation_indent_size" to "2",
      "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
    )
  kotlin {
    target("**/*.kt")
    targetExclude("**/build/")
    ktlint().editorConfigOverride(ktlintOptions)
    ktfmt().googleStyle()
  }
  kotlinGradle {
    target("*.gradle.kts", "**/*.gradle.kts")
    targetExclude("**/build/")
    ktfmt().googleStyle()
  }
}
