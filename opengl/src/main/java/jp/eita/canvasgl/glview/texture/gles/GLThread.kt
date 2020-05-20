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

package jp.eita.canvasgl.glview.texture.gles

import android.annotation.TargetApi
import android.opengl.EGL14
import android.opengl.EGLContext
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.os.Build
import android.util.Log
import android.view.Choreographer
import android.view.Choreographer.FrameCallback
import androidx.annotation.RequiresApi
import jp.eita.canvasgl.glview.texture.GLViewRenderer
import jp.eita.canvasgl.glview.texture.gles.EglContextWrapper.Companion.EGL_NO_CONTEXT_WRAPPER
import jp.eita.canvasgl.glview.texture.gles.EglHelper.Companion.logEglErrorAsWarning
import jp.eita.canvasgl.glview.texture.gles.EglHelper.Companion.throwEglException
import jp.eita.canvasgl.glview.texture.gles.EglHelperFactory.create
import jp.eita.canvasgl.util.FileLogger
import java.util.*
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGL11
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.opengles.GL

/**
 * This is the thread where the gl draw runs in.
 * Create GL Context --> Create Surface
 * And then draw with OpenGL and finally eglSwap to update the screen.
 */
class GLThread internal constructor(private val mEGLConfigChooser: EGLConfigChooser?, private val mEGLContextFactory: EGLContextFactory
                                    , private val mEGLWindowSurfaceFactory: EGLWindowSurfaceFactory, private val mRenderer: GLViewRenderer
                                    , private var mRenderMode: Int, private var mSurface: Any?, sharedEglContext: EglContextWrapper?) : Thread() {

    private val sGLThreadManager = GLThreadManager()

    private var onCreateGLContextListener: OnCreateGLContextListener? = null

    // Once the thread is started, all accesses to the following member
    // variables are protected by the sGLThreadManager monitor
    private var mShouldExit = false

    private var mExited = false

    private var mRequestPaused = false

    private var mPaused = false

    private var mHasSurface = false

    private var mSurfaceIsBad = false

    private var mWaitingForSurface = false

    private var mHaveEglContext = false

    private var mHaveEglSurface = false

    private var mFinishedCreatingEglSurface = false

    private var mWidth = 0

    private var mHeight = 0

    private var mRequestRender = true

    private var mWantRenderNotification = false

    private var mRenderComplete = false

    private val mEventQueue = ArrayList<Runnable>()

    private var mSizeChanged = true

    private var changeSurface = false

    var eglContext: EglContextWrapper? = sharedEglContext
        private set

    private val mChoreographerRenderWrapper = ChoreographerRenderWrapper(this)

    private var frameTimeNanos: Long = 0

    private var mEglHelper: IEglHelper? = null

    fun setSurface(surface: Any) {
        if (mSurface !== surface) {
            changeSurface = true
        }
        mSurface = surface
    }

    override fun run() {
        name = "GLThread $id"
        FileLogger.i(TAG, "starting tid=$id")
        try {
            guardedRun()
        } catch (e: InterruptedException) {
            // fall thru and exit normally
            FileLogger.e(TAG, "", e)
        } finally {
            sGLThreadManager.threadExiting(this)
        }
    }

    /**
     * This private method should only be called inside a
     * synchronized(sGLThreadManager) block.
     */
    private fun stopEglSurfaceLocked() {
        if (mHaveEglSurface) {
            mHaveEglSurface = false
            mEglHelper!!.destroySurface()
        }
    }

    /**
     * This private method should only be called inside a
     * synchronized(sGLThreadManager) block.
     */
    private fun stopEglContextLocked() {
        if (mHaveEglContext) {
            mEglHelper!!.finish()
            mHaveEglContext = false
            sGLThreadManager.releaseEglContextLocked(this)
        }
    }

    @Throws(InterruptedException::class)
    private fun guardedRun() {
        mEglHelper = create(mEGLConfigChooser!!, mEGLContextFactory, mEGLWindowSurfaceFactory)
        mHaveEglContext = false
        mHaveEglSurface = false
        mWantRenderNotification = false
        try {
            var createEglContext = false
            var createEglSurface = false
            var createGlInterface = false
            var lostEglContext = false
            var sizeChanged = false
            var wantRenderNotification = false
            var doRenderNotification = false
            var askedToReleaseEglContext = false
            var w = 0
            var h = 0
            var event: Runnable? = null
            while (true) {
                synchronized(sGLThreadManager) {
                    // Create egl context here
                    while (true) {
                        if (mShouldExit) {
                            return
                        }
                        if (mEventQueue.isNotEmpty() && mHaveEglContext) {
                            event = mEventQueue.removeAt(0)
                            break
                        }

                        // Update the pause state.
                        var pausing = false
                        if (mPaused != mRequestPaused) {
                            pausing = mRequestPaused
                            mPaused = mRequestPaused
                            sGLThreadManager.notifyAll()
                            FileLogger.i(TAG, "mPaused is now $mPaused tid=$id")
                        }

                        // Have we lost the EGL context?
                        if (lostEglContext) {
                            FileLogger.i(TAG, "lostEglContext")
                            stopEglSurfaceLocked()
                            stopEglContextLocked()
                            lostEglContext = false
                        }

                        // When pausing, release the EGL surface:
                        if (pausing && mHaveEglSurface) {
                            FileLogger.i(TAG, "releasing EGL surface because paused tid=$id")
                            stopEglSurfaceLocked()
                        }

                        // Have we lost the SurfaceView surface?
                        if (!mHasSurface && !mWaitingForSurface) {
                            FileLogger.i(TAG, "noticed surfaceView surface lost tid=$id")
                            if (mHaveEglSurface) {
                                stopEglSurfaceLocked()
                            }
                            mWaitingForSurface = true
                            mSurfaceIsBad = false
                            sGLThreadManager.notifyAll()
                        }

                        // Have we acquired the surface view surface?
                        if (mHasSurface && mWaitingForSurface) {
                            FileLogger.i(TAG, "noticed surfaceView surface acquired tid=$id")
                            mWaitingForSurface = false
                            sGLThreadManager.notifyAll()
                        }
                        if (doRenderNotification) {
//                            Log.i(TAG, "sending render notification tid=" + getId());
                            mWantRenderNotification = false
                            doRenderNotification = false
                            mRenderComplete = true
                            sGLThreadManager.notifyAll()
                        }

                        // Ready to draw?
                        if (readyToDraw()) {

                            // If we don't have an EGL context, try to acquire one.
                            if (!mHaveEglContext) {
                                if (askedToReleaseEglContext) {
                                    askedToReleaseEglContext = false
                                } else if (sGLThreadManager.tryAcquireEglContextLocked(this)) {
                                    try {
                                        eglContext = mEglHelper!!.start(eglContext)
                                        if (onCreateGLContextListener != null) {
                                            onCreateGLContextListener!!.onCreate(eglContext)
                                        }
                                    } catch (t: RuntimeException) {
                                        sGLThreadManager.releaseEglContextLocked(this)
                                        throw t
                                    }
                                    mHaveEglContext = true
                                    createEglContext = true
                                    sGLThreadManager.notifyAll()
                                }
                            }
                            if (mHaveEglContext && !mHaveEglSurface) {
                                mHaveEglSurface = true
                                createEglSurface = true
                                createGlInterface = true
                                sizeChanged = true
                            }
                            if (mHaveEglSurface) {
                                if (mSizeChanged) {
                                    sizeChanged = true
                                    w = mWidth
                                    h = mHeight
                                    mWantRenderNotification = true
                                    FileLogger.i(TAG, "noticing that we want render notification tid=$id")

                                    // Destroy and recreate the EGL surface.
                                    createEglSurface = true
                                    mSizeChanged = false
                                }
                                if (changeSurface) {
                                    createEglSurface = true
                                    changeSurface = false
                                }
                                mRequestRender = false
                                sGLThreadManager.notifyAll()
                                if (mWantRenderNotification) {
                                    wantRenderNotification = true
                                }
                                break
                            }
                        }

                        // By design, this is the only place in a GLThread thread where we wait().
                        if (LOG_THREADS) {
                            FileLogger.limitLog("", TAG, "waiting tid=" + id
                                    + " mHaveEglContext: " + mHaveEglContext
                                    + " mHaveEglSurface: " + mHaveEglSurface
                                    + " mFinishedCreatingEglSurface: " + mFinishedCreatingEglSurface
                                    + " mPaused: " + mPaused
                                    + " mHasSurface: " + mHasSurface
                                    + " mSurfaceIsBad: " + mSurfaceIsBad
                                    + " mWaitingForSurface: " + mWaitingForSurface
                                    + " mWidth: " + mWidth
                                    + " mHeight: " + mHeight
                                    + " mRequestRender: " + mRequestRender
                                    + " mRenderMode: " + mRenderMode, 600)
                        }
                        sGLThreadManager.wait()
                    }
                } // end of synchronized(sGLThreadManager)
                if (event != null) {
                    event!!.run()
                    event = null
                    continue
                }
                if (createEglSurface) {
                    FileLogger.w(TAG, "egl createSurface")
                    if (mEglHelper!!.createSurface(mSurface)) {
                        synchronized(sGLThreadManager) {
                            mFinishedCreatingEglSurface = true
                            sGLThreadManager.notifyAll()
                        }
                    } else {
                        synchronized(sGLThreadManager) {
                            mFinishedCreatingEglSurface = true
                            mSurfaceIsBad = true
                            sGLThreadManager.notifyAll()
                        }
                        continue
                    }
                    createEglSurface = false
                }
                if (createGlInterface) {
                    createGlInterface = false
                }

                // Make sure context and surface are created
                if (createEglContext) {
                    FileLogger.w("GLThread", "onSurfaceCreated")
                    mRenderer.onSurfaceCreated()
                    createEglContext = false
                }
                if (sizeChanged) {
                    FileLogger.w(TAG, "onSurfaceChanged($w, $h)")
                    mRenderer.onSurfaceChanged(w, h)
                    sizeChanged = false
                }
                if (mChoreographerRenderWrapper.canSwap()) {
                    if (LOG_RENDERER_DRAW_FRAME) {
                        Log.w(TAG, "onDrawFrame tid=$id")
                    }
                    mRenderer.onDrawFrame()
                    mEglHelper!!.setPresentationTime(frameTimeNanos)
                    val swapError = mEglHelper!!.swap()
                    mChoreographerRenderWrapper.disableSwap()
                    when (swapError) {
                        EGL10.EGL_SUCCESS -> {
                        }
                        EGL11.EGL_CONTEXT_LOST -> {
                            FileLogger.i(TAG, "egl context lost tid=$id")
                            lostEglContext = true
                        }
                        else -> {
                            // Other errors typically mean that the current surface is bad,
                            // probably because the SurfaceView surface has been destroyed,
                            // but we haven't been notified yet.
                            // Log the error to help developers understand why rendering stopped.
                            logEglErrorAsWarning("GLThread", "eglSwapBuffers", swapError)
                            synchronized(sGLThreadManager) {
                                mSurfaceIsBad = true
                                sGLThreadManager.notifyAll()
                            }
                        }
                    }
                }
                if (wantRenderNotification) {
                    doRenderNotification = true
                    wantRenderNotification = false
                }
            }
        } finally {
            /*
             * clean-up everything...
             */
            synchronized(sGLThreadManager) {
                stopEglSurfaceLocked()
                stopEglContextLocked()
            }
        }
    }

    @Synchronized
    override fun start() {
        super.start()
        mChoreographerRenderWrapper.start()
    }

    fun ableToDraw(): Boolean {
        return mHaveEglContext && mHaveEglSurface && readyToDraw()
    }

    private fun readyToDraw(): Boolean {
        return (!mPaused && mHasSurface && !mSurfaceIsBad
                && mWidth > 0 && mHeight > 0
                && mRequestRender)
    }

    fun setOnCreateGLContextListener(onCreateGLContextListener: OnCreateGLContextListener?) {
        this.onCreateGLContextListener = onCreateGLContextListener
    }

    var renderMode: Int
        get() = mRenderMode
        set(renderMode) {
            require(renderMode in RENDERMODE_WHEN_DIRTY..RENDERMODE_CONTINUOUSLY) { "renderMode" }
            synchronized(sGLThreadManager) {
                mRenderMode = renderMode
                sGLThreadManager.notifyAll()
            }
        }

    @JvmOverloads
    fun requestRender(frameTimeNanos: Long = 0) {
        this.frameTimeNanos = frameTimeNanos
        synchronized(sGLThreadManager) {
            mRequestRender = true
            sGLThreadManager.notifyAll()
        }
    }

    fun requestRenderAndWait() {
        synchronized(sGLThreadManager) {

            // If we are already on the GL thread, this means a client callback
            // has caused reentrancy, for example via updating the SurfaceView parameters.
            // We will return to the client rendering code, so here we don't need to
            // do anything.
            if (currentThread() === this) {
                return
            }
            mWantRenderNotification = true
            mRequestRender = true
            mRenderComplete = false
            sGLThreadManager.notifyAll()
            while (!mExited && !mPaused && !mRenderComplete && ableToDraw()) {
                try {
                    sGLThreadManager.wait()
                } catch (ex: InterruptedException) {
                    currentThread().interrupt()
                }
            }
        }
    }

    fun surfaceCreated() {
        synchronized(sGLThreadManager) {
            FileLogger.i(TAG, "surfaceCreated tid=$id")
            mHasSurface = true
            mFinishedCreatingEglSurface = false
            sGLThreadManager.notifyAll()
            while (mWaitingForSurface
                    && !mFinishedCreatingEglSurface
                    && !mExited) {
                try {
                    sGLThreadManager.wait()
                } catch (e: InterruptedException) {
                    currentThread().interrupt()
                }
            }
        }
    }

    /**
     * mHasSurface = false --> mWaitingForSurface = true
     * -->
     */
    fun surfaceDestroyed() {
        synchronized(sGLThreadManager) {
            FileLogger.i(TAG, "surfaceDestroyed tid=$id")
            mHasSurface = false
            sGLThreadManager.notifyAll()
            while (!mWaitingForSurface && !mExited) {
                try {
                    sGLThreadManager.wait()
                } catch (e: InterruptedException) {
                    currentThread().interrupt()
                }
            }
        }
    }

    /**
     * mRequestPaused --> mPaused, pausing
     * --> pausing && mHaveEglSurface, stopEglSurfaceLocked()
     * --> pausing && mHaveEglContext, preserve context or not.
     */
    fun onPause() {
        synchronized(sGLThreadManager) {
            FileLogger.i(TAG, "onPause tid=$id")
            mRequestPaused = true
            sGLThreadManager.notifyAll()
            while (!mExited && !mPaused) {
                FileLogger.i(TAG, "onPause waiting for mPaused.")
                try {
                    sGLThreadManager.wait()
                } catch (ex: InterruptedException) {
                    currentThread().interrupt()
                }
            }
            mChoreographerRenderWrapper.stop()
        }
    }

    fun onResume() {
        synchronized(sGLThreadManager) {
            FileLogger.i(TAG, "onResume tid=$id")
            mRequestPaused = false
            mRequestRender = true
            mRenderComplete = false
            sGLThreadManager.notifyAll()
            while (!mExited && mPaused && !mRenderComplete) {
                FileLogger.i(TAG, "onResume waiting for !mPaused.")
                try {
                    sGLThreadManager.wait()
                } catch (ex: InterruptedException) {
                    currentThread().interrupt()
                }
            }
            mChoreographerRenderWrapper.start()
        }
    }

    fun onWindowResize(w: Int, h: Int) {
        synchronized(sGLThreadManager) {
            FileLogger.d(TAG, "width:$w height:$h")
            mWidth = w
            mHeight = h
            mSizeChanged = true
            mRequestRender = true
            mRenderComplete = false

            // If we are already on the GL thread, this means a client callback
            // has caused reentrancy, for example via updating the SurfaceView parameters.
            // We need to process the size change eventually though and update our EGLSurface.
            // So we set the parameters and return so they can be processed on our
            // next iteration.
            if (currentThread() === this) {
                return
            }
            sGLThreadManager.notifyAll()

            // Wait for thread to react to resize and render a frame
            while (!mExited && !mPaused && !mRenderComplete
                    && ableToDraw()) {
                FileLogger.i(TAG, "onWindowResize waiting for render complete from tid=$id")
                try {
                    sGLThreadManager.wait()
                } catch (ex: InterruptedException) {
                    currentThread().interrupt()
                }
            }
        }
    }

    fun requestExitAndWait() {
        // don't call this from GLThread thread or it is a guaranteed
        // deadlock!
        synchronized(sGLThreadManager) {
            mShouldExit = true
            sGLThreadManager.notifyAll()
            while (!mExited) {
                try {
                    sGLThreadManager.wait()
                } catch (ex: InterruptedException) {
                    currentThread().interrupt()
                }
            }
        }
    }

    /**
     * Queue an "event" to be run on the GL rendering thread.
     *
     * @param r the runnable to be run on the GL rendering thread.
     */
    fun queueEvent(r: Runnable?) {
        requireNotNull(r) { "r must not be null" }
        synchronized(sGLThreadManager) {
            mEventQueue.add(r)
            sGLThreadManager.notifyAll()
        }
    }

    // End of member variables protected by the sGLThreadManager monitor.
    interface OnCreateGLContextListener {

        fun onCreate(eglContext: EglContextWrapper?)
    }

    interface GLWrapper {
        /**
         * Wraps a gl interface in another gl interface.
         *
         * @param gl a GL interface that is to be wrapped.
         * @return either the input argument or another GL object that wraps the input argument.
         */
        fun wrap(gl: GL?): GL?
    }

    interface EGLConfigChooser {
        /**
         * Choose a configuration from the list. Implementors typically
         * implement this method by calling
         * [EGL10.eglChooseConfig] and iterating through the results. Please consult the
         * EGL specification available from The Khronos Group to learn how to call eglChooseConfig.
         *
         * @param egl     the EGL10 for the current display.
         * @param display the current display.
         * @return the chosen configuration.
         */
        fun chooseConfig(egl: EGL10, display: EGLDisplay?): EGLConfig

        fun chooseConfig(display: android.opengl.EGLDisplay?, recordable: Boolean): android.opengl.EGLConfig?
    }

    interface EGLContextFactory {

        fun createContext(egl: EGL10, display: EGLDisplay?, eglConfig: EGLConfig?, eglContext: javax.microedition.khronos.egl.EGLContext?): javax.microedition.khronos.egl.EGLContext?

        fun destroyContext(egl: EGL10, display: EGLDisplay, context: javax.microedition.khronos.egl.EGLContext)

        fun createContextAPI17(display: android.opengl.EGLDisplay?, eglConfig: android.opengl.EGLConfig?, eglContext: EGLContext?): EGLContext?

        fun destroyContext(display: android.opengl.EGLDisplay, context: EGLContext)
    }

    interface EGLWindowSurfaceFactory {
        /**
         * @return null if the surface cannot be constructed.
         */
        fun createWindowSurface(egl: EGL10, display: EGLDisplay?, config: EGLConfig?,
                                nativeWindow: Any?): javax.microedition.khronos.egl.EGLSurface?

        fun destroySurface(egl: EGL10, display: EGLDisplay?, surface: javax.microedition.khronos.egl.EGLSurface?)
        fun createWindowSurface(display: android.opengl.EGLDisplay?, config: android.opengl.EGLConfig?,
                                nativeWindow: Any?): EGLSurface?

        fun destroySurface(display: android.opengl.EGLDisplay?, surface: EGLSurface?)
    }

    private class GLThreadManager : Object() {

        private var eglOwner: GLThread? = null

        @Synchronized
        fun threadExiting(thread: GLThread) {
            FileLogger.i(TAG, "exiting tid=" + thread.id)
            thread.mExited = true
            if (eglOwner === thread) {
                eglOwner = null
            }
            notifyAll()
        }

        /*
         * Tries once to acquire the right to use an EGL
         * context. Does not block. Requires that we are already
         * in the sGLThreadManager monitor when this is called.
         *
         * @return true if the right to use an EGL context was acquired.
         */
        fun tryAcquireEglContextLocked(thread: GLThread): Boolean {
            if (eglOwner === thread || eglOwner == null) {
                eglOwner = thread
                notifyAll()
                return true
            }
            return true
        }

        /*
         * Releases the EGL context. Requires that we are already in the
         * sGLThreadManager monitor when this is called.
         */
        fun releaseEglContextLocked(thread: GLThread) {
            if (eglOwner === thread) {
                eglOwner = null
            }
            notifyAll()
        }
    }

    abstract class BaseConfigChooser(configSpec: IntArray, private val contextClientVersion: Int) : EGLConfigChooser {

        private val EGL_RECORDABLE_ANDROID = 0x3142

        protected var configSpec: IntArray  = filterConfigSpec(configSpec)

        override fun chooseConfig(egl: EGL10, display: EGLDisplay?): EGLConfig {
            val numConfig = IntArray(1)
            require(egl.eglChooseConfig(display, configSpec, null, 0,
                    numConfig)) { "eglChooseConfig failed" }
            val numConfigs = numConfig[0]
            require(numConfigs > 0) { "No configs match configSpec" }
            val configs = arrayOfNulls<EGLConfig>(numConfigs)
            require(egl.eglChooseConfig(display, configSpec, configs, numConfigs,
                    numConfig)) { "eglChooseConfig#2 failed" }
            return chooseConfig(egl, display, configs)
                    ?: throw IllegalArgumentException("No config chosen")
        }

        abstract fun chooseConfig(egl: EGL10, display: EGLDisplay?,
                                  configs: Array<EGLConfig?>): EGLConfig?

        private fun filterConfigSpec(configSpec: IntArray): IntArray {
            if (contextClientVersion != 2 && contextClientVersion != 3) {
                return configSpec
            }
            /* We know none of the subclasses define EGL_RENDERABLE_TYPE.
             * And we know the configSpec is well formed.
             */
            val len = configSpec.size
            val newConfigSpec = IntArray(len + 2)
            System.arraycopy(configSpec, 0, newConfigSpec, 0, len - 1)
            newConfigSpec[len - 1] = EGL10.EGL_RENDERABLE_TYPE
            if (contextClientVersion == 2) {
                newConfigSpec[len] = EGL14.EGL_OPENGL_ES2_BIT /* EGL_OPENGL_ES2_BIT */
            } else {
                newConfigSpec[len] = EGLExt.EGL_OPENGL_ES3_BIT_KHR /* EGL_OPENGL_ES3_BIT_KHR */
            }
            newConfigSpec[len + 1] = EGL10.EGL_NONE

            return newConfigSpec
        }

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
        override fun chooseConfig(display: android.opengl.EGLDisplay?, recordable: Boolean): android.opengl.EGLConfig? {
            var renderableType = EGL14.EGL_OPENGL_ES2_BIT
            if (contextClientVersion >= 3) {
                renderableType = renderableType or EGLExt.EGL_OPENGL_ES3_BIT_KHR
            }

            // The actual surface is generally RGBA or RGBX, so situationally omitting alpha
            // doesn't really help.  It can also lead to a huge performance hit on glReadPixels()
            // when reading into a GL_RGBA buffer.
            val attribList = intArrayOf(
                    EGL14.EGL_RED_SIZE, 8,
                    EGL14.EGL_GREEN_SIZE, 8,
                    EGL14.EGL_BLUE_SIZE, 8,
                    EGL14.EGL_ALPHA_SIZE, 8,  //EGL14.EGL_DEPTH_SIZE, 16,
                    //EGL14.EGL_STENCIL_SIZE, 8,
                    EGL14.EGL_RENDERABLE_TYPE, renderableType,
                    EGL14.EGL_NONE, 0,  // placeholder for recordable [@-3]
                    EGL14.EGL_NONE
            )
            if (recordable) {
                attribList[attribList.size - 3] = EGL_RECORDABLE_ANDROID
                attribList[attribList.size - 2] = 1
            }

            val configs = arrayOfNulls<android.opengl.EGLConfig>(1)
            val numConfigs = IntArray(1)
            if (!EGL14.eglChooseConfig(display, attribList, 0, configs, 0, configs.size,
                            numConfigs, 0)) {
                Log.w("GLThread", "unable to find RGB8888 / $contextClientVersion EGLConfig")
                return null
            }

            return configs[0]
        }
    }

    /**
     * Choose a configuration with exactly the specified r,g,b,a sizes,
     * and at least the specified depth and stencil sizes.
     */
    open class ComponentSizeChooser(// Subclasses can adjust these values:
            protected var redSize: Int,

            protected var greenSize: Int,

            protected var blueSize: Int,

            protected var alphaSize: Int,

            protected var depthSize: Int,

            protected var stencilSize: Int,

            contextClientVersion: Int

    ) : BaseConfigChooser(intArrayOf(
            EGL10.EGL_RED_SIZE, redSize,
            EGL10.EGL_GREEN_SIZE, greenSize,
            EGL10.EGL_BLUE_SIZE, blueSize,
            EGL10.EGL_ALPHA_SIZE, alphaSize,
            EGL10.EGL_DEPTH_SIZE, depthSize,
            EGL10.EGL_STENCIL_SIZE, stencilSize,
            EGL10.EGL_NONE), contextClientVersion) {

        private val value: IntArray = IntArray(1)

        override fun chooseConfig(egl: EGL10, display: EGLDisplay?,
                                  configs: Array<EGLConfig?>): EGLConfig? {
            for (config in configs) {
                val d = findConfigAttrib(egl, display, config,
                        EGL10.EGL_DEPTH_SIZE, 0)
                val s = findConfigAttrib(egl, display, config,
                        EGL10.EGL_STENCIL_SIZE, 0)
                if (d >= depthSize && s >= stencilSize) {
                    val r = findConfigAttrib(egl, display, config,
                            EGL10.EGL_RED_SIZE, 0)
                    val g = findConfigAttrib(egl, display, config,
                            EGL10.EGL_GREEN_SIZE, 0)
                    val b = findConfigAttrib(egl, display, config,
                            EGL10.EGL_BLUE_SIZE, 0)
                    val a = findConfigAttrib(egl, display, config,
                            EGL10.EGL_ALPHA_SIZE, 0)
                    if (r == redSize && g == greenSize
                            && b == blueSize && a == alphaSize) {
                        return config
                    }
                }
            }

            return null
        }

        private fun findConfigAttrib(egl: EGL10, display: EGLDisplay?,
                                     config: EGLConfig?, attribute: Int, defaultValue: Int): Int {
            return if (egl.eglGetConfigAttrib(display, config, attribute, value)) {
                value[0]
            } else {
                defaultValue
            }
        }
    }

    /**
     * This class will choose a RGB_888 surface with
     * or without a depth buffer.
     */
    class SimpleEGLConfigChooser : ComponentSizeChooser {

        constructor(
                withDepthBuffer: Boolean,
                contextClientVersion: Int
        ) : super(8, 8, 8, 0, if (withDepthBuffer) 16 else 0, 0, contextClientVersion)

        constructor(
                redSize: Int,
                greenSize: Int,
                blueSize: Int,
                alphaSize: Int,
                depthSize: Int,
                stencilSize: Int,
                contextClientVersion: Int
        ) : super(redSize, greenSize, blueSize, alphaSize, depthSize, stencilSize, contextClientVersion)

        companion object {
            fun createConfigChooser(withDepthBuffer: Boolean, contextClientVersion: Int): SimpleEGLConfigChooser {
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    SimpleEGLConfigChooser(withDepthBuffer, contextClientVersion)
                } else {
                    SimpleEGLConfigChooser(5, 6, 5, 8, 0, 0, contextClientVersion)
                }
            }
        }
    }

    class DefaultContextFactory(private val contextClientVersion: Int) : EGLContextFactory {

        @Suppress("PrivatePropertyName")
        private val EGL_CONTEXT_CLIENT_VERSION = 0x3098

        override fun createContext(egl: EGL10, display: EGLDisplay?, eglConfig: EGLConfig?, eglContext: javax.microedition.khronos.egl.EGLContext?): javax.microedition.khronos.egl.EGLContext? {
            val attribList = intArrayOf(
                    EGL_CONTEXT_CLIENT_VERSION, contextClientVersion,
                    EGL10.EGL_NONE)
            return egl.eglCreateContext(display, eglConfig, eglContext,
                    if (contextClientVersion != 0) attribList else null)
        }

        override fun destroyContext(egl: EGL10, display: EGLDisplay,
                                    context: javax.microedition.khronos.egl.EGLContext) {
            if (!egl.eglDestroyContext(display, context)) {
                FileLogger.e(TAG, "DefaultContextFactory display:$display context: $context")
                throwEglException("eglDestroyContext", egl.eglGetError())
            }
        }

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
        override fun createContextAPI17(display: android.opengl.EGLDisplay?, eglConfig: android.opengl.EGLConfig?, eglContext: EGLContext?): EGLContext? {
            val attribList = intArrayOf(
                    EGL14.EGL_CONTEXT_CLIENT_VERSION, contextClientVersion,
                    EGL14.EGL_NONE)
            return EGL14.eglCreateContext(display, eglConfig, eglContext, attribList, 0)
        }

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
        override fun destroyContext(display: android.opengl.EGLDisplay, context: EGLContext) {
            if (!EGL14.eglDestroyContext(display, context)) {
                FileLogger.e(TAG, "DefaultContextFactory display:$display context: $context")
                throwEglException("eglDestroyContext", EGL14.eglGetError())
            }
        }

    }

    class DefaultWindowSurfaceFactory : EGLWindowSurfaceFactory {

        override fun createWindowSurface(egl: EGL10, display: EGLDisplay?,
                                         config: EGLConfig?, nativeWindow: Any?): javax.microedition.khronos.egl.EGLSurface? {
            val surfaceAttribs = intArrayOf(
                    EGL10.EGL_NONE
            )
            var result: javax.microedition.khronos.egl.EGLSurface? = null
            try {
                result = egl.eglCreateWindowSurface(display, config, nativeWindow, surfaceAttribs)
            } catch (e: IllegalArgumentException) {
                // This exception indicates that the surface flinger surface
                // is not valid. This can happen if the surface flinger surface has
                // been torn down, but the application has not yet been
                // notified via SurfaceHolder.Callback.surfaceDestroyed.
                // In theory the application should be notified first,
                // but in practice sometimes it is not. See b/4588890
                Log.e("DefaultWindow", "eglCreateWindowSurface", e)
            }
            return result
        }

        override fun destroySurface(egl: EGL10, display: EGLDisplay?,
                                    surface: javax.microedition.khronos.egl.EGLSurface?) {
            egl.eglDestroySurface(display, surface)
        }

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
        override fun createWindowSurface(display: android.opengl.EGLDisplay?, config: android.opengl.EGLConfig?, nativeWindow: Any?): EGLSurface? {
            val surfaceAttribs = intArrayOf(
                    EGL14.EGL_NONE
            )
            var result: EGLSurface? = null
            try {
                result = EGL14.eglCreateWindowSurface(display, config, nativeWindow, surfaceAttribs, 0)
            } catch (e: IllegalArgumentException) {
                // This exception indicates that the surface flinger surface
                // is not valid. This can happen if the surface flinger surface has
                // been torn down, but the application has not yet been
                // notified via SurfaceHolder.Callback.surfaceDestroyed.
                // In theory the application should be notified first,
                // but in practice sometimes it is not. See b/4588890
                Log.e("DefaultWindow", "eglCreateWindowSurface", e)
            }
            return result
        }

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
        override fun destroySurface(display: android.opengl.EGLDisplay?, surface: EGLSurface?) {
            EGL14.eglDestroySurface(display, surface)
        }
    }

    class Builder {

        private var configChooser: EGLConfigChooser? = null

        private var eglContextFactory: EGLContextFactory? = null

        private var eglWindowSurfaceFactory: EGLWindowSurfaceFactory? = null

        private var renderer: GLViewRenderer? = null

        private var eglContextClientVersion = 2

        private var renderMode = RENDERMODE_WHEN_DIRTY

        private var surface: Any? = null

        private var eglContext = EGL_NO_CONTEXT_WRAPPER

        fun setSurface(surface: Any?): Builder {
            this.surface = surface
            return this
        }

        fun setEGLConfigChooser(needDepth: Boolean): Builder {
            setEGLConfigChooser(SimpleEGLConfigChooser.createConfigChooser(needDepth, eglContextClientVersion))
            return this
        }

        fun setEGLConfigChooser(configChooser: EGLConfigChooser?): Builder {
            this.configChooser = configChooser
            return this
        }

        fun setEGLConfigChooser(redSize: Int, greenSize: Int, blueSize: Int,
                                alphaSize: Int, depthSize: Int, stencilSize: Int): Builder {
            setEGLConfigChooser(ComponentSizeChooser(redSize, greenSize,
                    blueSize, alphaSize, depthSize, stencilSize, eglContextClientVersion))
            return this
        }

        fun setEglContextFactory(eglContextFactory: EGLContextFactory?): Builder {
            this.eglContextFactory = eglContextFactory
            return this
        }

        fun setEglWindowSurfaceFactory(eglWindowSurfaceFactory: EGLWindowSurfaceFactory?): Builder {
            this.eglWindowSurfaceFactory = eglWindowSurfaceFactory
            return this
        }

        fun setRenderer(renderer: GLViewRenderer?): Builder {
            this.renderer = renderer
            return this
        }

        fun setGLWrapper(mGLWrapper: GLWrapper?): Builder {
            return this
        }

        fun setEglContextClientVersion(eglContextClientVersion: Int): Builder {
            this.eglContextClientVersion = eglContextClientVersion
            return this
        }

        fun setRenderMode(renderMode: Int): Builder {
            this.renderMode = renderMode
            return this
        }

        fun setSharedEglContext(sharedEglContext: EglContextWrapper): Builder {
            eglContext = sharedEglContext
            return this
        }

        fun createGLThread(): GLThread {
            if (renderer == null) {
                throw NullPointerException("renderer has not been set")
            }
            if (surface == null && eglWindowSurfaceFactory == null) {
                throw NullPointerException("surface has not been set")
            }
            if (configChooser == null) {
                configChooser = SimpleEGLConfigChooser.createConfigChooser(true, eglContextClientVersion)
            }
            if (eglContextFactory == null) {
                eglContextFactory = DefaultContextFactory(eglContextClientVersion)
            }
            if (eglWindowSurfaceFactory == null) {
                eglWindowSurfaceFactory = DefaultWindowSurfaceFactory()
            }

            return GLThread(configChooser, eglContextFactory!!, eglWindowSurfaceFactory!!, renderer!!, renderMode, surface, eglContext)
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    class ChoreographerRender @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN) constructor(private val glThread: GLThread) : FrameCallback {

        // Only used when render mode is RENDERMODE_CONTINUOUSLY
        private var canSwap = true

        override fun doFrame(frameTimeNanos: Long) {
            if (glThread.mRenderMode == RENDERMODE_CONTINUOUSLY) {
                canSwap = true
                glThread.requestRender(frameTimeNanos)
                Choreographer.getInstance().postFrameCallback(this)
            }
        }

        fun start() {
            Choreographer.getInstance().postFrameCallback(this)
        }

        fun stop() {
            Choreographer.getInstance().removeFrameCallback(this)
        }

        fun isCanSwap(): Boolean {
            return canSwap || glThread.mRenderMode == RENDERMODE_WHEN_DIRTY
        }

        fun setCanSwap(canSwap: Boolean) {
            this.canSwap = canSwap
        }

    }

    class ChoreographerRenderWrapper(glThread: GLThread) {

        private var choreographerRender: ChoreographerRender = ChoreographerRender(glThread)

        fun start() {
            choreographerRender.start()
        }

        fun stop() {
            choreographerRender.start()
        }

        fun canSwap(): Boolean {
            return choreographerRender.isCanSwap()
        }

        fun disableSwap() {
            choreographerRender.setCanSwap(false)
        }
    }

    companion object {

        const val LOG_RENDERER_DRAW_FRAME = false

        const val LOG_THREADS = false

        const val RENDERMODE_WHEN_DIRTY = 0

        const val RENDERMODE_CONTINUOUSLY = 1

        private const val TAG = "GLThread"
    }
}