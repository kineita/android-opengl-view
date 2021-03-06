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
package jp.eita.canvasgl.glcanvas

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Build
import android.os.ParcelFileDescriptor
import android.text.TextUtils
import android.util.Log
import java.io.Closeable
import java.io.IOException
import java.io.InterruptedIOException
import kotlin.math.min

object GLCanvasUtils {

    private val TAG = this::class.simpleName

    private const val DEBUG_TAG = "GalleryDebug"

    private const val POLY64REV = -0x6a536cd653b4364bL

    private const val INITIALCRC = -0x1L

    private val IS_DEBUG_BUILD = Build.TYPE == "eng" || Build.TYPE == "userdebug"

    private const val MASK_STRING = "********************************"

    private val CRC_TABLE = LongArray(256).apply {
        var part: Long

        for (i in 0..255) {
            part = i.toLong()
            for (j in 0..7) {
                val x = if (part.toInt() and 1 != 0) POLY64REV else 0
                part = part.shr(1) xor x
            }
            this[i] = part
        }
    }

    // Throws AssertionError if the input is false.
    fun assertTrue(cond: Boolean) {
        if (!cond) {
            throw AssertionError()
        }
    }

    // Throws AssertionError with the message. We had a method having the form
    //   assertTrue(boolean cond, String message, Object ... args);
    // However a call to that method will cause memory allocation even if the
    // condition is false (due to autoboxing generated by "Object ... args"),
    // so we don't use that anymore.
    fun fail(message: String?, vararg args: Any?) {
        throw AssertionError(
                if (args.isEmpty()) message else String.format(message!!, *args))
    }

    // Throws NullPointerException if the input is null.
    fun <T> checkNotNull(`object`: T?): T {
        if (`object` == null) throw NullPointerException()
        return `object`
    }

    // Returns true if two input Object are both null or equal
    // to each other.
    fun equals(a: Any?, b: Any?): Boolean {
        return a == b
    }

    // Returns the next power of two.
    // Returns the input if it is already power of 2.
    // Throws IllegalArgumentException if the input is <= 0 or
    // the answer overflows.
    fun nextPowerOf2(n: Int): Int {
        var result = n
        require(!(result <= 0 || result > 1 shl 30)) { "n is invalid: $result" }
        result -= 1
        result = result or (result shr 16)
        result = result or (result shr 8)
        result = result or (result shr 4)
        result = result or (result shr 2)
        result = result or (result shr 1)

        return result + 1
    }

    // Returns the previous power of two.
    // Returns the input if it is already power of 2.
    // Throws IllegalArgumentException if the input is <= 0
    fun prevPowerOf2(n: Int): Int {
        require(n > 0)
        return Integer.highestOneBit(n)
    }

    // Returns the input value x clamped to the range [min, max].
    fun clamp(x: Int, min: Int, max: Int): Int {
        if (x > max) return max
        return if (x < min) min else x
    }

    // Returns the input value x clamped to the range [min, max].
    fun clamp(x: Float, min: Float, max: Float): Float {
        if (x > max) return max
        return if (x < min) min else x
    }

    // Returns the input value x clamped to the range [min, max].
    fun clamp(x: Long, min: Long, max: Long): Long {
        if (x > max) return max
        return if (x < min) min else x
    }

    fun isOpaque(color: Int): Boolean {
        return color ushr 24 == 0xFF
    }

    fun swap(array: IntArray, i: Int, j: Int) {
        val temp = array[i]
        array[i] = array[j]
        array[j] = temp
    }

    /**
     * A function thats returns a 64-bit crc for string
     *
     * @param in input string
     * @return a 64-bit crc value
     */
    fun crc64Long(`in`: String?): Long {
        return if (`in` == null || `in`.isEmpty()) {
            0
        } else crc64Long(getBytes(`in`))
    }

    fun crc64Long(buffer: ByteArray): Long {
        var crc = INITIALCRC
        for (b in buffer) {
            crc = CRC_TABLE[crc.toInt() xor b.toInt() and 0xff] xor (crc shr 8)
        }
        return crc
    }

    fun getBytes(`in`: String): ByteArray {
        val result = ByteArray(`in`.length * 2)
        var output = 0
        for (ch in `in`.toCharArray()) {
            result[output++] = (ch.toInt() and 0xFF).toByte()
            result[output++] = (ch.toInt() shr 8).toByte()
        }
        return result
    }

    fun closeSilently(c: Closeable?) {
        if (c == null) return
        try {
            c.close()
        } catch (t: IOException) {
            Log.w(TAG, "close fail ", t)
        }
    }

    fun compare(a: Long, b: Long): Int {
        return a.compareTo(b)
    }

    fun ceilLog2(value: Float): Int {
        var i = 0
        while (i < 31) {
            if (1 shl i >= value) break
            i++
        }
        return i
    }

    fun floorLog2(value: Float): Int {
        var i: Int = 0
        while (i < 31) {
            if (1 shl i > value) break
            i++
        }
        return i - 1
    }

    fun closeSilently(fd: ParcelFileDescriptor?) {
        try {
            fd?.close()
        } catch (t: Throwable) {
            Log.w(TAG, "fail to close", t)
        }
    }

    fun closeSilently(cursor: Cursor?) {
        try {
            cursor?.close()
        } catch (t: Throwable) {
            Log.w(TAG, "fail to close", t)
        }
    }

    fun interpolateAngle(
            source: Float, target: Float, progress: Float): Float {
        // interpolate the angle from source to target
        // We make the difference in the range of [-179, 180], this is the
        // shortest path to change source to target.
        var diff = target - source
        if (diff < 0) diff += 360f
        if (diff > 180) diff -= 360f
        val result = source + diff * progress
        return if (result < 0) result + 360f else result
    }

    fun interpolateScale(
            source: Float, target: Float, progress: Float): Float {
        return source + progress * (target - source)
    }

    fun ensureNotNull(value: String?): String {
        return value ?: ""
    }

    fun parseFloatSafely(content: String?, defaultValue: Float): Float {
        return if (content == null) defaultValue else try {
            content.toFloat()
        } catch (e: NumberFormatException) {
            defaultValue
        }
    }

    fun parseIntSafely(content: String?, defaultValue: Int): Int {
        return if (content == null) defaultValue else try {
            content.toInt()
        } catch (e: NumberFormatException) {
            defaultValue
        }
    }

    fun isNullOrEmpty(exifMake: String?): Boolean {
        return TextUtils.isEmpty(exifMake)
    }

    fun waitWithoutInterrupt(`object`: Object) {
        try {
            `object`.wait()
        } catch (e: InterruptedException) {
            Log.w(TAG, "unexpected interrupt: $`object`")
        }
    }

    fun handleInterrruptedException(e: Throwable?): Boolean {
        // A helper to deal with the interrupt exception
        // If an interrupt detected, we will setup the bit again.
        if (e is InterruptedIOException
                || e is InterruptedException) {
            Thread.currentThread().interrupt()
            return true
        }
        return false
    }

    /**
     * @return String with special XML characters escaped.
     */
    fun escapeXml(s: String): String {
        val sb = StringBuilder()
        var i = 0
        val len = s.length
        while (i < len) {
            when (val c = s[i]) {
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '\"' -> sb.append("&quot;")
                '\'' -> sb.append("&#039;")
                '&' -> sb.append("&amp;")
                else -> sb.append(c)
            }
            ++i
        }
        return sb.toString()
    }

    fun getUserAgent(context: Context): String {
        val packageInfo: PackageInfo
        packageInfo = try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            throw IllegalStateException("getPackageInfo failed")
        }
        return String.format("%s/%s; %s/%s/%s/%s; %s/%s/%s",
                packageInfo.packageName,
                packageInfo.versionName,
                Build.BRAND,
                Build.DEVICE,
                Build.MODEL,
                Build.ID,
                Build.VERSION.SDK_INT,
                Build.VERSION.RELEASE,
                Build.VERSION.INCREMENTAL)
    }

    fun copyOf(source: Array<String?>, newSize: Int): Array<String?> {
        var newSizeEdited = newSize
        val result = arrayOfNulls<String>(newSizeEdited)
        newSizeEdited = min(source.size, newSizeEdited)
        System.arraycopy(source, 0, result, 0, newSizeEdited)

        return result
    }

    // Mask information for debugging only. It returns <code>info.toString()</code> directly
    // for debugging build (i.e., 'eng' and 'userdebug') and returns a mask ("****")
    // in release build to protect the information (e.g. for privacy issue).
    fun maskDebugInfo(info: Any?): String? {
        if (info == null) return null
        val s = info.toString()
        val length = min(s.length, MASK_STRING.length)
        return if (IS_DEBUG_BUILD) s else MASK_STRING.substring(0, length)
    }

    // This method should be ONLY used for debugging.
    fun debug(message: String?, vararg args: Any?) {
        Log.v(DEBUG_TAG, String.format(message!!, *args))
    }
}