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

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.awaitClosed
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeout

private const val NETWORK_PRINTER_TIMEOUT_MS = 5_000L

internal suspend fun sendToNetworkPrinter(bytes: ByteArray, host: String, port: Int): PrintResult =
  try {
    withTimeout(NETWORK_PRINTER_TIMEOUT_MS) {
      SelectorManager().use { selector ->
        aSocket(selector).tcp().connect(host, port).use {
          val channel = it.openWriteChannel(autoFlush = true)
          channel.writeFully(bytes)
          channel.flushAndClose()
          it.close()
          it.awaitClosed()
        }
      }
    }
    PrintResult.Success
  } catch (t: TimeoutCancellationException) {
    PrintResult.Failure("Timed out reaching printer at $host:$port", t)
  } catch (c: CancellationException) {
    if (!currentCoroutineContext().isActive) throw c
    PrintResult.Failure("Printer at $host:$port closed the connection", c)
  } catch (t: Throwable) {
    PrintResult.Failure(t.message ?: "Could not reach printer at $host:$port", t)
  }

internal fun NetworkEscPosDriver.toEscPosBytes(html: String): ByteArray =
  buildEscPos(
    htmlToText(html),
    EscPosDriver(charactersPerLine = charactersPerLine, cut = cut, openDrawer = openDrawer),
  )
