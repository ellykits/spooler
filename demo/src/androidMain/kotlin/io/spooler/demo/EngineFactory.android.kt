package io.spooler.demo

import android.annotation.SuppressLint
import android.content.Context
import io.spooler.core.PrintEngine

@SuppressLint("StaticFieldLeak") lateinit var demoAppContext: Context

actual fun createDemoEngine(): PrintEngine = PrintEngine(demoAppContext)
