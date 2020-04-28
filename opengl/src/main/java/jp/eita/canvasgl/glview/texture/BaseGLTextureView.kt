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
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.util.Log
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import jp.eita.canvasgl.ICanvasGL
import jp.eita.canvasgl.glview.texture.gles.EglContextWrapper
import jp.eita.canvasgl.glview.texture.gles.GLThread
import jp.eita.canvasgl.glview.texture.gles.GLThread.OnCreateGLContextListener
import jp.eita.canvasgl.util.Loggers
import java.util.*

/**
 * Can be used in ScrollView or ListView.
 * Can make it not opaque by setOpaque(false).
 *
 *
 * The surface of canvasGL is provided by TextureView.
 *
 *
 * onSurfaceTextureSizeChanged onResume onPause onSurfaceTextureDestroyed onSurfaceTextureUpdated
 * From init to run: onSizeChanged --> onSurfaceTextureAvailable --> createGLThread --> createSurface
 * From run to pause: onPause --> destroySurface
 * From pause to run: onResume --> createSurface
 * From run to stop: onPause --> destroySurface --> onSurfaceTextureDestroyed --> EGLHelper.finish --> GLThread.exit
 * From stop to run: onResume --> onSurfaceTextureAvailable --> createGLThread --> createSurface
 */
abstract class BaseGLTextureView : TextureView, SurfaceTextureListener {

    protected open var mGLThread: GLThread? = null

    internal var glThreadBuilder: GLThread.Builder? = null

    protected val cacheEvents: MutableList<Runnable> = ArrayList()

    internal var surfaceTextureListener: SurfaceTextureListener? = null

    private var onCreateGLContextListener: OnCreateGLContextListener? = null

    private var surfaceAvailable = false

    private var renderer: GLViewRenderer? = null

    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        super.setSurfaceTextureListener(this)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        Loggers.d(TAG, "onSizeChanged: ")
        super.onSizeChanged(w, h, oldw, oldh)
        mGLThread?.onWindowResize(w, h)
    }

    open fun onPause() {
        mGLThread?.onPause()
    }

    fun onResume() {
        mGLThread?.onResume()
    }

    fun queueEvent(r: Runnable) {
        if (mGLThread == null) {
            cacheEvents.add(r)
            return
        }
        mGLThread?.queueEvent(r)
    }

    fun requestRender() {
        if (mGLThread != null) {
            mGLThread!!.requestRender()
        } else {
            Log.w(TAG, "GLThread is not created when requestRender")
        }
    }

    /**
     * Wait until render command is sent to OpenGL
     */
    fun requestRenderAndWait() {
        if (mGLThread != null) {
            mGLThread!!.requestRenderAndWait()
        } else {
            Log.w(TAG, "GLThread is not created when requestRender")
        }
    }

    protected fun surfaceCreated() {
        mGLThread!!.surfaceCreated()
    }

    protected open fun surfaceDestroyed() {
        // Surface will be destroyed when we return
        if (mGLThread != null) {
            mGLThread!!.surfaceDestroyed()
            mGLThread!!.requestExitAndWait()
        }
        surfaceAvailable = false
        mGLThread = null
    }

    protected fun surfaceChanged(w: Int, h: Int) {
        mGLThread!!.onWindowResize(w, h)
    }

    protected fun surfaceRedrawNeeded() {
        if (mGLThread != null) {
            mGLThread!!.requestRenderAndWait()
        }
    }

    override fun onDetachedFromWindow() {
        Loggers.d(TAG, "onDetachedFromWindow: ")
        if (mGLThread != null) {
            mGLThread!!.requestExitAndWait()
        }
        super.onDetachedFromWindow()
    }

    @Throws(Throwable::class)
    protected fun finalize() {
        try {
            if (mGLThread != null) {
                // GLThread may still be running if this view was never
                // attached to a window.
                mGLThread!!.requestExitAndWait()
            }
        } finally {

        }
    }

    protected open fun init() {

    }

    /**
     * @return If the context is not created, then EGL10.EGL_NO_CONTEXT will be returned.
     */
    val currentEglContext: EglContextWrapper?
        get() = if (mGLThread == null) null else mGLThread!!.eglContext

    fun setOnCreateGLContextListener(onCreateGLContextListener: OnCreateGLContextListener?) {
        this.onCreateGLContextListener = onCreateGLContextListener
    }

    override fun setSurfaceTextureListener(surfaceTextureListener: SurfaceTextureListener) {
        this.surfaceTextureListener = surfaceTextureListener
    }

    protected open val renderMode: Int
        protected get() = GLThread.RENDERMODE_WHEN_DIRTY

    protected abstract fun onGLDraw(canvas: ICanvasGL?)
    fun setRenderer(renderer: GLViewRenderer?) {
        this.renderer = renderer
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        Loggers.d(TAG, "onSurfaceTextureAvailable: ")
        surfaceAvailable = true
        glThreadBuilder = GLThread.Builder()
        if (mGLThread == null) {
            glThreadBuilder!!.setRenderMode(renderMode)
                    .setSurface(surface)
                    .setRenderer(renderer)
            createGLThread()
        } else {
            mGLThread!!.setSurface(surface)
            freshSurface(width, height)
        }
        surfaceTextureListener?.onSurfaceTextureAvailable(surface, width, height)
    }

    protected open fun createGLThread() {
        Loggers.d(TAG, "createGLThread: ")
        if (!surfaceAvailable) {
            return
        }
        mGLThread = glThreadBuilder!!.createGLThread()
        mGLThread!!.setOnCreateGLContextListener(object : OnCreateGLContextListener {
            override fun onCreate(eglContext: EglContextWrapper?) {
                post {
                    if (onCreateGLContextListener != null) {
                        onCreateGLContextListener!!.onCreate(eglContext)
                    }
                }
            }
        })
        mGLThread!!.start()
        freshSurface(width, height)
        for (cacheEvent in cacheEvents) {
            mGLThread!!.queueEvent(cacheEvent)
        }
        cacheEvents.clear()
    }

    /**
     * surface inited or updated.
     */
    private fun freshSurface(width: Int, height: Int) {
        surfaceCreated()
        surfaceChanged(width, height)
        surfaceRedrawNeeded()
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        Loggers.d(TAG, "onSurfaceTextureSizeChanged: ")
        surfaceChanged(width, height)
        surfaceRedrawNeeded()
        surfaceTextureListener?.onSurfaceTextureSizeChanged(surface, width, height)
    }

    /**
     * This will be called when windows detached. Activity onStop will cause window detached.
     */
    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        Loggers.d(TAG, "onSurfaceTextureDestroyed: ")
        surfaceDestroyed()
        surfaceTextureListener?.onSurfaceTextureSizeChanged(surface, width, height)
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        surfaceTextureListener?.onSurfaceTextureUpdated(surface)
    }

    companion object {

        private const val TAG = "BaseGLTextureView"
    }
}