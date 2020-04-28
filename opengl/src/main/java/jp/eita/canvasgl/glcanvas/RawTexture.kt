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

import android.opengl.GLES20
import android.util.Log
import javax.microedition.khronos.opengles.GL11

class RawTexture constructor(width: Int, height: Int, override val isOpaque: Boolean) : BasicTexture() {

    /**
     * Call this when surfaceTexture calls updateTexImage
     */
    var isNeedInvalidate = false

    /**
     * @param isFlipped whether vertically flip this texture
     */
    override var isFlippedVertically = false

    override val target = GL11.GL_TEXTURE_2D

    init {
        setSize(width, height)
    }

    fun prepare(canvas: GLCanvas) {
        val glId = canvas.gLId
        id = glId!!.generateTexture()
        if (target == GLES20.GL_TEXTURE_2D) {
            canvas.initializeTextureSize(this, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE)
        }
        canvas.setTextureParameters(this)
        state = STATE_LOADED
        setAssociatedCanvas(canvas)
    }

    override fun onBind(canvas: GLCanvas): Boolean {
        if (isLoaded) return true
        Log.w(TAG, "lost the content due to context change")
        return false
    }

    override fun yield() {
        // we cannot free the secondBitmap because we have no backup.
    }

    companion object {

        private val TAG: String = this::class.java.name
    }
}