/*
* Copyright 2026 Spooler Contributors
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package io.spooler.core

/** Style variant of a [RegisteredFont], mirroring the standard CSS `font-style` values. */
enum class FontStyle {
  NORMAL,
  ITALIC,
  OBLIQUE,
}

/**
 * A font file a consumer supplies to a [PrintEngine]. [name] is the CSS font-family name used to
 * reference it; [bytes] holds the TrueType/OpenType file contents. [weight] and [style] let a
 * consumer register several faces (e.g. regular and bold) under the same [name].
 */
data class RegisteredFont(
  val name: String,
  val bytes: ByteArray,
  val weight: Int = 400,
  val style: FontStyle = FontStyle.NORMAL,
)
