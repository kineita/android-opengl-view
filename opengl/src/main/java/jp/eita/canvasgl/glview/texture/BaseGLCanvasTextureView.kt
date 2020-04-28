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
import android.graphics.Color
import android.graphics.Rect
import android.util.AttributeSet
import androidx.annotation.ColorInt
import jp.eita.canvasgl.CanvasGL
import jp.eita.canvasgl.ICanvasGL
import jp.eita.canvasgl.OpenGLUtil.createBitmapFromGLSurface
import jp.eita.canvasgl.glview.GLView.GetDrawingCacheCallback
import jp.eita.canvasgl.util.Loggers

/**
 * From init to run: onSizeChange --> onSurfaceTextureAvailable --> createGLThread --> createSurface --> onSurfaceCreated --> onSurfaceChanged
 * From pause to run: onResume --> createSurface --> onSurfaceChanged
 * From stop to run: onResume --> onSurfaceTextureAvailable --> createGLThread --> createSurface  --> onSurfaceCreated --> onSurfaceChanged
 */
abstract class BaseGLCanvasTextureView : BaseGLTextureView, GLViewRenderer {

    protected var mCanvas: ICanvasGL? = null

    internal var backgroundColor = Color.TRANSPARENT

    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun init() {
        super.init()
        setRenderer(this)
    }

    override fun onSurfaceCreated() {
        Loggers.d(TAG, "onSurfaceCreated: ")
        mCanvas = CanvasGL()
    }

    override fun onSurfaceChanged(width: Int, height: Int) {
        Loggers.d(TAG, "onSurfaceChanged: ")
        mCanvas!!.setSize(width, height)
    }

    override fun onDrawFrame() {
        mCanvas!!.clearBuffer(backgroundColor)
        onGLDraw(mCanvas)
    }

    override fun onPause() {
        super.onPause()
        if (mCanvas != null) {
            mCanvas!!.pause()
        }
    }

    abstract override fun onGLDraw(canvas: ICanvasGL?)

    /**
     * If setOpaque(true) used, this method will not work.
     */
    fun setRenderBackgroundColor(@ColorInt color: Int) {
        backgroundColor = color
    }

    fun getDrawingBitmap(rect: Rect, getDrawingCacheCallback: GetDrawingCacheCallback) {
        queueEvent(Runnable {
            onDrawFrame()
            onDrawFrame()
            val bitmapFromGLSurface = createBitmapFromGLSurface(rect.left, rect.top, rect.right - rect.left, rect.bottom - rect.top, height)
            post { getDrawingCacheCallback.onFetch(bitmapFromGLSurface) }
        })
        requestRender()
    }

    companion object {
        private const val TAG = "BaseGLCanvasTextureView"
    }
}