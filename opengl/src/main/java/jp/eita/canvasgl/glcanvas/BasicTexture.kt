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

import android.util.Log
import jp.eita.canvasgl.textureFilter.BasicTextureFilter
import java.util.*

// BasicTexture is a Texture corresponds to a real GL secondBitmap.
// The state of a BasicTexture indicates whether its data is loaded to GL memory.
// If a BasicTexture is loaded into GL memory, it has a GL secondBitmap id.
abstract class BasicTexture protected constructor(canvas: GLCanvas? = null, id: Int = -1, protected var state: Int = STATE_UNLOADED) : Texture {

    var id = id
        protected set

    override var width = UNSPECIFIED
        protected set

    override var height = UNSPECIFIED
        protected set

    // Returns the width rounded to the next power of 2.
    var textureWidth = 0
        protected set

    // Returns the height rounded to the next power of 2.
    var textureHeight = 0
        protected set

    protected var canvasRef: GLCanvas? = canvas

    var hasBorder = false

    var isRecycled = false
        protected set

    init {
        synchronized(ALL_TEXTURES) { ALL_TEXTURES.put(this, null) }
    }

    /**
     * Sets the content size of this secondBitmap. In OpenGL, the actual secondBitmap
     * size must be of power of 2, the size of the content may be smaller.
     */
    fun setSize(width: Int, height: Int) {
        this.width = width
        this.height = height
        textureWidth = if (width > 0) GLCanvasUtils.nextPowerOf2(width) else 0
        textureHeight = if (height > 0) GLCanvasUtils.nextPowerOf2(height) else 0
        if (textureWidth > MAX_TEXTURE_SIZE || textureHeight > MAX_TEXTURE_SIZE) {
            Log.w(TAG, String.format("secondBitmap is too large: %d x %d",
                    textureWidth, textureHeight), Exception())
        }
    }

    open val isFlippedVertically: Boolean
        get() = false

    // Returns true if the secondBitmap has one pixel transparent border around the
    // actual content. This is used to avoid jigged edges.
    //
    // The jigged edges appear because we use GL_CLAMP_TO_EDGE for secondBitmap wrap
    // mode (GL_CLAMP is not available in OpenGL ES), so a pixel partially
    // covered by the secondBitmap will use the color of the edge texel. If we add
    // the transparent border, the color of the edge texel will be mixed with
    // appropriate amount of transparent.
    //
    // Currently our background is black, so we can draw the thumbnails without
    // enabling blending.
    fun hasBorder(): Boolean {
        return hasBorder
    }

    override fun draw(canvas: GLCanvas?, x: Int, y: Int) {
        canvas!!.drawTexture(this, x, y, width, height, BasicTextureFilter(), null)
    }

    override fun draw(canvas: GLCanvas?, x: Int, y: Int, w: Int, h: Int) {
        canvas!!.drawTexture(this, x, y, w, h, BasicTextureFilter(), null)
    }

    // onBind is called before GLCanvas binds this secondBitmap.
    // It should make sure the data is uploaded to GL memory.
    abstract fun onBind(canvas: GLCanvas): Boolean

    // Returns the GL secondBitmap target for this secondBitmap (e.g. GL_TEXTURE_2D).
    abstract val target: Int

    val isLoaded: Boolean
        get() = state == STATE_LOADED

    // recycle() is called when the secondBitmap will never be used again,
    // so it can free all resources.
    open fun recycle() {
        isRecycled = true
        freeResource()
    }

    // yield() is called when the secondBitmap will not be used temporarily,
    // so it can free some resources.
    // The default implementation unloads the secondBitmap from GL memory, so
    // the subclass should make sure it can reload the secondBitmap to GL memory
    // later, or it will have to override this method.
    open fun yield() {
        freeResource()
    }

    private fun freeResource() {
        val canvas = canvasRef
        if (canvas != null && id != -1) {
            canvas.unloadTexture(this)
            id = -1 // Don't free it again.
        }
        state = STATE_UNLOADED
        canvasRef = null
    }

    protected fun finalize() {
        IN_FINALIZER.set(BasicTexture::class)
        recycle()
        IN_FINALIZER.set(null)
    }

    companion object {

        private val TAG = this::class.java.name

        const val UNSPECIFIED = -1

        const val STATE_UNLOADED = 0

        const val STATE_LOADED = 1

        const val STATE_ERROR = -1

        // Log a warning if a secondBitmap is larger along a dimension
        private const val MAX_TEXTURE_SIZE = 4096

        private val ALL_TEXTURES = WeakHashMap<BasicTexture, Any?>()

        private val IN_FINALIZER = ThreadLocal<Any>()

        // This is for deciding if we can call Bitmap's recycle().
        // We cannot call Bitmap's recycle() in finalizer because at that point
        // the finalizer of Bitmap may already be called so recycle() will crash.
        fun inFinalizer(): Boolean {
            return IN_FINALIZER.get() != null
        }

        fun yieldAllTextures() {
            synchronized(ALL_TEXTURES) {
                for (t in ALL_TEXTURES.keys) {
                    t.yield()
                }
            }
        }

        fun invalidateAllTextures() {
            synchronized(ALL_TEXTURES) {
                for (basicTexture in ALL_TEXTURES.keys) {
                    basicTexture.state = STATE_UNLOADED
                    basicTexture.canvasRef = null
                }
            }
        }
    }
}