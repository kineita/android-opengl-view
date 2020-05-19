/*
 * Copyright [2020 - Present] [Lê Trần Ngọc Thành - 瑛太 (eita)] [kineita (Github)]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.eita.canvasgl.util

import android.os.Process
import android.util.Log
import jp.eita.canvasgl.util.FileUtil.createFile
import jp.eita.canvasgl.util.FileUtil.delete
import jp.eita.canvasgl.util.FileUtil.writeToFile
import java.io.File
import java.io.FileFilter
import java.security.InvalidParameterException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

object FileLogger {

    private val LOG_DATE_TIME_FORMAT = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)

    private val logExecutor = Executors.newSingleThreadExecutor()

    var isLogEnable = false

    var logLevel = LogLevel.VERBOSE

    private var logFileManager: LogFileManager? = null

    private val limitLogMap: MutableMap<String, Int> = HashMap()

    fun init(dirPath: String) {
        isLogEnable = true
        val file = File(dirPath)
        if (!file.exists() || !file.isDirectory) {
            throw InvalidParameterException()
        }
        logFileManager = LogFileManager(dirPath)
    }

    /**
     * @param id                   the id for this log. Must be unique
     * @param cntTimesAfterLogOnce example: 1000 log once, then after 1000 call of this will log again
     */
    fun limitLog(id: String, tag: String, message: String?, cntTimesAfterLogOnce: Int) {
        if (!limitLogMap.containsKey(id)) {
            limitLogMap[id] = 0
        } else {
            val currentCnt = limitLogMap[id]
            if (currentCnt!! < cntTimesAfterLogOnce) {
                limitLogMap[id] = currentCnt + 1
                return
            } else {
                limitLogMap[id] = 0
            }
        }
        d(tag, message)
    }

    /**
     * log for debug
     *
     * @param message log message
     * @param tag     tag
     * @see Log.d
     */
    fun d(tag: String, message: String) {
        if (isLogEnable) {
            Log.d(tag, message)
            writeToFileIfNeeded(tag, message, LogLevel.DEBUG)
        }
    }

    /**
     * log for debug
     *
     * @param message   log message
     * @param throwable throwable
     * @param tag       tag
     * @see Log.d
     */
    fun d(tag: String, message: String, throwable: Throwable?) {
        if (isLogEnable) {
            Log.d(tag, message, throwable)
            writeToFileIfNeeded(tag, """
     $message
     ${Log.getStackTraceString(throwable)}
     """.trimIndent(), LogLevel.DEBUG)
        }
    }

    /**
     * log for debug
     *
     * @param tag    tag
     * @param format message format, such as "%d ..."
     * @param params message content params
     * @see Log.d
     */
    fun d(tag: String, format: String?, vararg params: Any?) {
        if (isLogEnable) {
            val msg = String.format(format!!, *params)
            Log.d(tag, msg)
            writeToFileIfNeeded(tag, msg, LogLevel.DEBUG)
        }
    }

    /**
     * log for warning
     *
     * @param message log message
     * @param tag     tag
     * @see Log.w
     */
    fun w(tag: String, message: String) {
        if (isLogEnable) {
            Log.w(tag, message)
            writeToFileIfNeeded(tag, message, LogLevel.WARN)
        }
    }

    /**
     * log for warning
     *
     * @param tag       tag
     * @param throwable throwable
     * @see Log.w
     */
    fun w(tag: String, throwable: Throwable?) {
        if (isLogEnable) {
            Log.w(tag, throwable)
            writeToFileIfNeeded(tag, Log.getStackTraceString(throwable), LogLevel.WARN)
        }
    }

    /**
     * log for warning
     *
     * @param message   log message
     * @param throwable throwable
     * @param tag       tag
     * @see Log.w
     */
    fun w(tag: String, message: String, throwable: Throwable?) {
        if (isLogEnable) {
            Log.w(tag, message, throwable)
            writeToFileIfNeeded(tag, """
     $message
     ${Log.getStackTraceString(throwable)}
     """.trimIndent(), LogLevel.WARN)
        }
    }

    /**
     * log for warning
     *
     * @param tag    tag
     * @param format message format, such as "%d ..."
     * @param params message content params
     * @see Log.w
     */
    fun w(tag: String, format: String?, vararg params: Any?) {
        if (isLogEnable) {
            val msg = String.format(format!!, *params)
            Log.w(tag, msg)
            writeToFileIfNeeded(tag, msg, LogLevel.WARN)
        }
    }

    /**
     * log for error
     *
     * @param message message
     * @param tag     tag
     * @see Log.i
     */
    fun e(tag: String, message: String) {
        if (isLogEnable) {
            Log.e(tag, message)
            writeToFileIfNeeded(tag, message, LogLevel.ERROR)
        }
    }

    /**
     * log for error
     *
     * @param message   log message
     * @param throwable throwable
     * @param tag       tag
     * @see Log.i
     */
    fun e(tag: String, message: String, throwable: Throwable?) {
        if (isLogEnable) {
            Log.e(tag, message, throwable)
            writeToFileIfNeeded(tag, """
     $message
     ${Log.getStackTraceString(throwable)}
     """.trimIndent(), LogLevel.ERROR)
        }
    }

    /**
     * log for error
     *
     * @param tag    tag
     * @param format message format, such as "%d ..."
     * @param params message content params
     * @see Log.e
     */
    fun e(tag: String, format: String?, vararg params: Any?) {
        if (isLogEnable) {
            val msg = String.format(format!!, *params)
            Log.e(tag, msg)
            writeToFileIfNeeded(tag, msg, LogLevel.ERROR)
        }
    }

    /**
     * log for information
     *
     * @param message message
     * @param tag     tag
     * @see Log.i
     */
    fun i(tag: String, message: String) {
        if (isLogEnable) {
            Log.i(tag, message)
            writeToFileIfNeeded(tag, message, LogLevel.INFO)
        }
    }

    /**
     * log for information
     *
     * @param message   log message
     * @param throwable throwable
     * @param tag       tag
     * @see Log.i
     */
    fun i(tag: String, message: String, throwable: Throwable?) {
        if (isLogEnable) {
            Log.i(tag, message, throwable)
            writeToFileIfNeeded(tag, """
     $message
     ${Log.getStackTraceString(throwable)}
     """.trimIndent(), LogLevel.INFO)
        }
    }

    /**
     * log for information
     *
     * @param tag    tag
     * @param format message format, such as "%d ..."
     * @param params message content params
     * @see Log.i
     */
    fun i(tag: String, format: String?, vararg params: Any?) {
        if (isLogEnable) {
            val msg = String.format(format!!, *params)
            Log.i(tag, msg)
            writeToFileIfNeeded(tag, msg, LogLevel.INFO)
        }
    }

    /**
     * log for verbos
     *
     * @param message log message
     * @param tag     tag
     * @see Log.v
     */
    fun v(tag: String, message: String) {
        if (isLogEnable) {
            Log.v(tag, message)
            writeToFileIfNeeded(tag, message, LogLevel.VERBOSE)
        }
    }

    /**
     * log for verbose
     *
     * @param message   log message
     * @param throwable throwable
     * @param tag       tag
     * @see Log.v
     */
    fun v(tag: String, message: String, throwable: Throwable?) {
        if (isLogEnable) {
            Log.v(tag, message, throwable)
            writeToFileIfNeeded(tag, """
             $message
             ${Log.getStackTraceString(throwable)}
             """.trimIndent(), LogLevel.VERBOSE)
        }
    }

    /**
     * log for verbose
     *
     * @param tag    tag
     * @param format message format, such as "%d ..."
     * @param params message content params
     * @see Log.v
     */
    fun v(tag: String, format: String, vararg params: Any?) {
        if (isLogEnable) {
            val msg = String.format(format, *params)
            Log.v(tag, msg)
            writeToFileIfNeeded(tag, msg, LogLevel.VERBOSE)
        }
    }

    private fun writeToFileIfNeeded(tag: String, msg: String?, logLevel: LogLevel) {
        val strBuilder = StringBuilder()
        val stackTrace = Throwable().stackTrace
        val methodStackCnt = 2
        strBuilder
                .append(" ")
                .append(" tid=").append(Thread.currentThread().id)
                .append(" ")
                .append(stackTrace[methodStackCnt].fileName)
                .append("[").append(stackTrace[methodStackCnt].lineNumber)
                .append("] ").append("; ")
                .append(stackTrace[methodStackCnt].methodName)
                .append(": ")
        if (logLevel.value < this.logLevel.value || logFileManager == null) {
            return
        }
        logExecutor.execute { appendLog(strBuilder.toString() + tag, msg) }
    }

    private fun appendLog(tag: String, msg: String?) {
        val logMsg = formatLog(tag, msg)
        flushLogToFile(logMsg)
    }

    private fun flushLogToFile(logMsg: String) {
        logFileManager!!.writeLogToFile(logMsg)
    }

    private fun formatLog(tag: String, msg: String?): String {
        return String.format(Locale.US, "%s pid=%d %s; %s\n", LOG_DATE_TIME_FORMAT.format(Date()), Process.myPid(), tag, msg)
    }

    enum class LogLevel(val value: Int) {
        VERBOSE(Log.VERBOSE), DEBUG(Log.DEBUG), INFO(Log.INFO), WARN(Log.WARN), ERROR(Log.ERROR), ASSERT(Log.ASSERT);

    }

    class LogFileManager internal constructor(private val mLogFileDir: String) {

        private var currentLogFile: File? = null

        private val fileFilter = FileFilter { file ->
            val tmp = file.name.toLowerCase(Locale.getDefault())
            tmp.startsWith("log") && tmp.endsWith(".txt")
        }

        private val newLogFile: File?
            get() {
                val dir = File(mLogFileDir)
                val files = dir.listFiles(fileFilter)
                if (files == null || files.isEmpty()) {
                    return createNewLogFileIfNeed()
                }
                val sortedFiles = sortFiles(files)
                if (files.size > LOG_FILES_MAX_NUM) {
                    delete(sortedFiles[0])
                }

                return createNewLogFileIfNeed()
            }

        fun writeLogToFile(logMessage: String?) {
            if (currentLogFile == null || currentLogFile!!.length() >= LOG_FILE_MAX_SIZE) {
                currentLogFile = newLogFile
            }
            writeToFile(logMessage!!, currentLogFile!!.path)
        }

        private fun createNewLogFileIfNeed(): File? {
            return createFile(mLogFileDir + File.separator + PREFIX + LOG_FILE_DATE_FORMAT.format(Date()) + ".txt")
        }

        private fun sortFiles(files: Array<File>): List<File> {
            val fileList = listOf(*files)
            Collections.sort(fileList, FileComparator())

            return fileList
        }

        private inner class FileComparator : Comparator<File> {

            override fun compare(file1: File, file2: File): Int {
                return if (file1.lastModified() < file2.lastModified()) {
                    -1
                } else {
                    1
                }
            }
        }

        companion object {

            const val PREFIX = "Log"

            private const val LOG_FILES_MAX_NUM = 5

            private const val LOG_FILE_MAX_SIZE = 1000 * 1000 * 20

            private val LOG_FILE_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        }
    }
}