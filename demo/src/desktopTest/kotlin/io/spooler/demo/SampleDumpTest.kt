package io.spooler.demo

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class SampleDumpTest {
  @Test
  fun dumpsEachSampleToHtmlFile() {
    val outputDir = File("build/samples").apply { mkdirs() }

    for (sample in allSamples()) {
      val slug = sample.label.lowercase().replace(" ", "-")
      val file = File(outputDir, "$slug.html")
      file.writeText(sample.html)

      assertTrue(file.exists(), "$file should exist")
      assertTrue(file.readText().isNotEmpty(), "$file should be non-empty")
    }
  }
}
