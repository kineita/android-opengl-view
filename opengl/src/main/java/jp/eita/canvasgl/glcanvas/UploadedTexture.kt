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

import android.graphics.Bitmap
import android.opengl.GLUtils
import java.util.*
import javax.microedition.khronos.opengles.GL11

// UploadedTextures use a Bitmap for the content of the secondBitmap.
//
// Subclasses should implement onGetBitmap() to provide the Bitmap and
// implement onFreeBitmap(mBitmap) which will be called when the Bitmap
// is not needed anymore.
//
// isContentValid() is meaningful only when the isLoaded() returns true.
// It means whether the content needs to be updated.
//
// The user of this class should call recycle() when the secondBitmap is not
// needed anymore.
//
// By default an UploadedTexture is opaque (so it can be drawn faster without
// blending). The user or subclass can override it using setOpaque().
abstract class UploadedTexture protected constructor(hasBorder: Boolean = false) : BasicTexture(null, 0, STATE_UNLOADED) {

    private var mContentValid = true

    /**
     * Whether the content on GPU is valid.
     */
    val isContentValid: Boolean
        get() = isLoaded && mContentValid

    // indicate this textures is being uploaded in background
    var isUploading = false
        protected set

    private var isThrottled = false

    protected var mBitmap: Bitmap? = null

    private var border = 0

    override val target: Int
        get() = GL11.GL_TEXTURE_2D

    init {
        if (hasBorder) {
            setBorder(true)
            border = 1
        }
    }

    private class BorderKey : Cloneable {

        var vertical = false

        var config: Bitmap.Config? = null

        var length = 0

        override fun hashCode(): Int {
            val x = config.hashCode() xor length

            return if (vertical) x else -x
        }

        override fun equals(other: Any?): Boolean {
            if (other !is BorderKey) {
                return false
            }

            return vertical == other.vertical && config == other.config && length == other.length
        }

        public override fun clone(): BorderKey {
            return try {
                super.clone() as BorderKey
            } catch (e: CloneNotSupportedException) {
                throw AssertionError(e)
            }
        }
    }

    private val bitmap: Bitmap?
        get() {
            if (mBitmap == null) {
                mBitmap = onGetBitmap()
                val w = mBitmap!!.width + border * 2
                val h = mBitmap!!.height + border * 2
                if (width == UNSPECIFIED) {
                    setSize(w, h)
                }
            }

            return mBitmap
        }

    private fun freeBitmap() {
//        Assert.assertTrue(mBitmap != null);
        onFreeBitmap(mBitmap)
        mBitmap = null
    }

    protected abstract fun onGetBitmap(): Bitmap?

    protected abstract fun onFreeBitmap(bitmap: Bitmap?)

    fun invalidateContent() {
        if (mBitmap != null) freeBitmap()
        mContentValid = false
        width = UNSPECIFIED
        height = UNSPECIFIED
    }

    /**
     * Updates the content on GPU's memory.
     * @param canvas
     */
    fun updateContent(canvas: GLCanvas) {
        if (!isLoaded) {
            if (isThrottled && ++UPLOADED_COUNT > UPLOAD_LIMIT) {
                return
            }
            uploadToCanvas(canvas)
        } else if (!mContentValid) {
            val bitmap = bitmap
            val format = GLUtils.getInternalFormat(bitmap)
            val type = GLUtils.getType(bitmap)
            canvas.texSubImage2D(this, border, border, bitmap, format, type)
            freeBitmap()
            mContentValid = true
        }
    }

    private fun uploadToCanvas(canvas: GLCanvas) {
        val bitmap = bitmap
        if (bitmap != null) {
            try {
                val bWidth = bitmap.width
                val bHeight = bitmap.height
                width = bWidth + border * 2
                height = bHeight + border * 2
                val texWidth = textureWidth
                val texHeight = textureHeight

                // Upload the bitmap to a new secondBitmap.
                id = canvas.gLId!!.generateTexture()
                canvas.setTextureParameters(this)
                if (bWidth == texWidth && bHeight == texHeight) {
                    canvas.initializeTexture(this, bitmap)
                } else {
                    val format = GLUtils.getInternalFormat(bitmap)
                    val type = GLUtils.getType(bitmap)
                    val config = bitmap.config
                    canvas.initializeTextureSize(this, format, type)
                    canvas.texSubImage2D(this, border, border, bitmap, format, type)
                    if (border > 0) {
                        // Left border
                        var line = getBorderLine(true, config, texHeight)
                        canvas.texSubImage2D(this, 0, 0, line, format, type)

                        // Top border
                        line = getBorderLine(false, config, texWidth)
                        canvas.texSubImage2D(this, 0, 0, line, format, type)
                    }

                    // Right border
                    if (border + bWidth < texWidth) {
                        val line = getBorderLine(true, config, texHeight)
                        canvas.texSubImage2D(this, border + bWidth, 0, line, format, type)
                    }

                    // Bottom border
                    if (border + bHeight < texHeight) {
                        val line = getBorderLine(false, config, texWidth)
                        canvas.texSubImage2D(this, 0, border + bHeight, line, format, type)
                    }
                }
            } finally {
                freeBitmap()
            }
            // Update secondBitmap state.
            setAssociatedCanvas(canvas)
            state = STATE_LOADED
            mContentValid = true
        } else {
            state = STATE_ERROR
            throw RuntimeException("Texture load fail, no bitmap")
        }
    }

    override fun onBind(canvas: GLCanvas): Boolean {
        updateContent(canvas)

        return isContentValid
    }

    override fun recycle() {
        super.recycle()
        if (mBitmap != null) freeBitmap()
    }

    companion object {

        private const val TAG = "Texture"

        // To prevent keeping allocation the borders, we store those used borders here.
        // Since the length will be power of two, it won't use too much memory.
        private val BORDER_LINES = HashMap<BorderKey, Bitmap?>()

        private val BORDER_KEY = BorderKey()

        private var UPLOADED_COUNT = 0

        private const val UPLOAD_LIMIT = 100

        private fun getBorderLine(
                vertical: Boolean, config: Bitmap.Config, length: Int): Bitmap? {
            val key = BORDER_KEY
            key.vertical = vertical
            key.config = config
            key.length = length
            var bitmap = BORDER_LINES[key]
            if (bitmap == null) {
                bitmap = if (vertical) Bitmap.createBitmap(1, length, config) else Bitmap.createBitmap(length, 1, config)
                BORDER_LINES[key.clone()] = bitmap
            }
            return bitmap
        }

        fun resetUploadLimit() {
            UPLOADED_COUNT = 0
        }

        fun uploadLimitReached(): Boolean {
            return UPLOADED_COUNT > UPLOAD_LIMIT
        }
    }
}