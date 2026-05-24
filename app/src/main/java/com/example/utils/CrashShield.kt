package com.example.utils

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

object CrashShield {
    private const val CRASH_FILE_NAME = "fatal_crash.txt"

    fun init(context: Context) {
        val applicationContext = context.applicationContext
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                throwable.printStackTrace(pw)
                val stackTraceString = sw.toString()

                val file = File(applicationContext.filesDir, CRASH_FILE_NAME)
                file.writeText(stackTraceString)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable)
            } else {
                exitProcess(1)
            }
        }
    }

    fun getLatestCrash(context: Context): String? {
        return try {
            val file = File(context.filesDir, CRASH_FILE_NAME)
            if (file.exists()) {
                val content = file.readText().trim()
                if (content.isNotEmpty()) content else null
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun clearCrash(context: Context): Boolean {
        return try {
            val file = File(context.filesDir, CRASH_FILE_NAME)
            if (file.exists()) {
                file.delete()
            } else {
                true
            }
        } catch (e: Exception) {
            false
        }
    }
}
