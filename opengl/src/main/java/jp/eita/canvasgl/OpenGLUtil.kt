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
package jp.eita.canvasgl

import android.graphics.Bitmap
import android.graphics.Point
import android.opengl.GLES11
import android.opengl.GLES20
import android.opengl.GLException
import android.view.View
import java.nio.IntBuffer
import javax.microedition.khronos.opengles.GL10

/**
 * Some tools for OpenGL
 */
object OpenGLUtil {

    fun setUniformMatrix4f(location: Int, matrix: FloatArray?) {
        GLES20.glUniformMatrix4fv(location, 1, false, matrix, 0)
    }

    @JvmStatic
    fun setFloat(location: Int, floatValue: Float) {
        GLES20.glUniform1f(location, floatValue)
    }

    @JvmStatic
    @Throws(OutOfMemoryError::class)
    fun createBitmapFromGLSurface(x: Int, y: Int, w: Int, h: Int, glHeight: Int): Bitmap? {
        val bitmapBuffer = IntArray(w * h)
        val bitmapSource = IntArray(w * h)
        val intBuffer = IntBuffer.wrap(bitmapBuffer)
        intBuffer.position(0)
        try {
            GLES11.glReadPixels(x, glHeight - h - y, w, h, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, intBuffer)
            var offset1: Int
            var offset2: Int
            for (i in 0 until h) {
                offset1 = i * w
                offset2 = (h - i - 1) * w
                for (j in 0 until w) {
                    val texturePixel = bitmapBuffer[offset1 + j]
                    val blue = texturePixel shr 16 and 0xff
                    val red = texturePixel shl 16 and 0x00ff0000
                    val pixel = texturePixel and -0xff0100 or red or blue
                    bitmapSource[offset2 + j] = pixel
                }
            }
        } catch (e: GLException) {
            return null
        }
        return Bitmap.createBitmap(bitmapSource, w, h, Bitmap.Config.ARGB_8888)
    }

    @JvmStatic
    fun getPointOfView(view: View): Point {
        val location = IntArray(2)
        view.getLocationInWindow(location)
        return Point(location[0], location[1])
    }
}