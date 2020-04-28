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
package jp.eita.canvasgl.glview.texture

import android.content.Context
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import jp.eita.canvasgl.ICanvasGL
import jp.eita.canvasgl.glview.texture.gles.GLThread
import jp.eita.canvasgl.util.Loggers
import java.util.*

/**
 * Used to generate multiple textures or consume textures from others.
 * This will not create [GLThread] automatically. You need to call [.setSharedEglContext] to trigger it.
 * Support providing multiple textures to Camera or Media. <br></br>
 * This can also consume textures from other GL zone( Should be in same GL context) <br></br>
 * And since this inherits [GLMultiTexConsumerView], the [.setSharedEglContext] must be called
 */
abstract class GLMultiTexProducerView : GLMultiTexConsumerView {

    private var producedTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES

    private val producedTextureList: MutableList<GLTexture> = ArrayList()

    private var surfaceTextureCreatedListener: SurfaceTextureCreatedListener? = null

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onGLDraw(canvas: ICanvasGL?, consumedTextures: List<GLTexture>?) {
        onGLDraw(canvas, producedTextureList, consumedTextures)
    }

    override val renderMode: Int
        get() = GLThread.RENDERMODE_WHEN_DIRTY

    internal val initialTexCount: Int = 1


    /**
     * @return The initial produced texture count
     */
    protected open fun getInitialTexCount(): Int {
        return 1
    }

    /**
     * If it is used, it must be called before [GLThread.start] called.
     *
     * @param producedTextureTarget GLES20.GL_TEXTURE_2D or GLES11Ext.GL_TEXTURE_EXTERNAL_OES
     */
    fun setProducedTextureTarget(producedTextureTarget: Int) {
        this.producedTextureTarget = producedTextureTarget
    }

    /**
     * Create a new produced texture and upload it to the canvas.
     */
    fun addProducedGLTexture(width: Int, height: Int, opaque: Boolean, target: Int): GLTexture {
        val glTexture = GLTexture.createRaw(width, height, opaque, target, mCanvas!!)
        producedTextureList.add(glTexture)
        return glTexture
    }

    override fun onSurfaceChanged(width: Int, height: Int) {
        super.onSurfaceChanged(width, height)
        Loggers.d(TAG, "onSurfaceChanged: $width, $height")
        if (producedTextureList.isEmpty()) {
            for (i in 0 until initialTexCount) {
                // This must be in this thread because it relies on the GLContext of this thread
                producedTextureList.add(GLTexture.createRaw(width, height, false, producedTextureTarget, mCanvas!!))
            }
            post {
                if (producedTextureList.isNotEmpty() && surfaceTextureCreatedListener != null) {
                    surfaceTextureCreatedListener!!.onCreated(producedTextureList)
                }
            }
        } else {
            for (glTexture in producedTextureList) {
                glTexture.rawTexture.setSize(width, height)
            }
        }
    }

    override fun onDrawFrame() {
        if (producedTextureTarget != GLES20.GL_TEXTURE_2D) {
            for (glTexture in producedTextureList) {
                glTexture.surfaceTexture.updateTexImage()
                glTexture.rawTexture.isNeedInvalidate = true
            }
        }
        super.onDrawFrame()
    }

    override fun onPause() {
        super.onPause()
        Loggers.d(TAG, "onPause")
        recycleProduceTexture()
        if (mGLThread == null) {
            Log.w(TAG, "!!!!!! You may not call setShareEglContext !!!")
        }
    }

    override fun surfaceDestroyed() {
        super.surfaceDestroyed()
        recycleProduceTexture()
    }

    private fun recycleProduceTexture() {
        for (glTexture in producedTextureList) {
            if (!glTexture.rawTexture.isRecycled) {
                glTexture.rawTexture.recycle()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!glTexture.surfaceTexture.isReleased) {
                    glTexture.surfaceTexture.release()
                }
            } else {
                glTexture.surfaceTexture.release()
            }
        }
        producedTextureList.clear()
    }

    /**
     * Set the listener to listen the texture creation.
     *
     * @param surfaceTextureCreatedListener The texture listener
     */
    fun setSurfaceTextureCreatedListener(surfaceTextureCreatedListener: SurfaceTextureCreatedListener?) {
        this.surfaceTextureCreatedListener = surfaceTextureCreatedListener
    }

    /**
     * If [.setSharedEglContext] is not called, this will not be triggered.
     * The consumedTextures are obtained from [GLMultiTexConsumerView.addConsumeGLTexture]
     *
     * @param canvas           the canvas to draw things
     * @param producedTextures The textures created by itself.
     * @param consumedTextures May be null. This only available when it gets from other GLMultiTexProducerView
     */
    protected abstract fun onGLDraw(canvas: ICanvasGL?, producedTextures: List<GLTexture>?, consumedTextures: List<GLTexture>?)

    /**
     * Listen when the produced textures created.
     */
    interface SurfaceTextureCreatedListener {
        /**
         * You can get the created Textures from this method.
         * The number of textures is decided by [GLMultiTexProducerView.getInitialTexCount]
         *
         * @param producedTextureList The created Textures
         */
        fun onCreated(producedTextureList: List<GLTexture>)
    }

    companion object {
        private const val TAG = "GLMultiTexProducerView"
    }
}