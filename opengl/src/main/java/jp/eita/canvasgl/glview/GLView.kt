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

package jp.eita.canvasgl.glview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Rect
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import jp.eita.canvasgl.CanvasGL
import jp.eita.canvasgl.ICanvasGL
import jp.eita.canvasgl.util.OpenGLUtil.createBitmapFromGLSurface
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

abstract class GLView : GLSurfaceView, GLSurfaceView.Renderer {

    protected var canvas: CanvasGL? = null

    protected var gl: GL10? = null

    var onSizeChangeCallback: OnSizeChangeCallback? = null

    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    init {
        initGLView()
    }

    private fun initGLView() {
        apply {
            setZOrderOnTop(true)
            setEGLContextClientVersion(2)
            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
            preserveEGLContextOnPause = true
            holder.setFormat(PixelFormat.TRANSLUCENT)
            setRenderer(this)
            renderMode = RENDERMODE_WHEN_DIRTY
        }
    }

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        canvas = CanvasGL()
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        canvas?.let {
            it.setSize(width, height)
            it
        }
    }

    override fun onDrawFrame(gl: GL10) {
        this.gl = gl
        canvas?.let {
            it.clearBuffer()
            onGLDraw(it)
        }
    }

    fun restart() {
        onResume()
    }

    fun stop() {
        onPause()
        canvas?.let {
            it.pause()
            it
        }
    }

    open fun destroy() { }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        onSizeChangeCallback?.onSizeChange(w, h, oldw, oldh)
    }

    fun getDrawingBitmap(rect: Rect, getDrawingCacheCallback: GetDrawingCacheCallback) {
        queueEvent {
            gl?.let {
                onDrawFrame(it)
                onDrawFrame(it)
                val bitmapFromGLSurface = createBitmapFromGLSurface(rect.left, rect.top, rect.right, rect.bottom, height)
                post { getDrawingCacheCallback.onFetch(bitmapFromGLSurface) }
            }
        }
        requestRender()
    }

    /**
     * May call twice at first.
     */
    protected abstract fun onGLDraw(canvas: ICanvasGL)

    interface OnSizeChangeCallback {

        fun onSizeChange(w: Int, h: Int, oldw: Int, oldh: Int)
    }

    interface GetDrawingCacheCallback {

        fun onFetch(bitmap: Bitmap?)
    }
}