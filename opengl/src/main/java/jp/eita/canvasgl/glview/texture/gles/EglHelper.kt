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

import android.util.Log
import jp.eita.canvasgl.util.FileLogger
import jp.eita.canvasgl.util.Loggers
import javax.microedition.khronos.egl.*

class EglHelper(
        private val eglConfigChooser: GLThread.EGLConfigChooser,

        private val eglContextFactory: GLThread.EGLContextFactory,

        private val eglWindowSurfaceFactory: GLThread.EGLWindowSurfaceFactory
) : IEglHelper {

    private var egl: EGL10? = null

    private var eglDisplay: EGLDisplay? = null

    private var eglSurface: EGLSurface? = null

    private var eglConfig: EGLConfig? = null

    private var eglContext1: EGLContext? = null

    /**
     * Initialize EGL for a given configuration spec.
     *
     * @param eglContext
     */
    override fun start(eglContext: EglContextWrapper?): EglContextWrapper? {
        FileLogger.w("EglHelper", "start() tid=" + Thread.currentThread().id)
        /*
         * Get an EGL instance
         */egl = EGLContext.getEGL() as EGL10

        /*
         * Get to the default display.
         */eglDisplay = egl!!.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
        if (eglDisplay === EGL10.EGL_NO_DISPLAY) {
            throw RuntimeException("eglGetDisplay failed")
        }

        /*
         * We can now initialize EGL for that display
         */
        val version = IntArray(2)
        if (!egl!!.eglInitialize(eglDisplay, version)) {
            throw RuntimeException("eglInitialize failed")
        }
        eglConfig = eglConfigChooser.chooseConfig(egl!!, eglDisplay)

        /*
         * Create an EGL context. We want to do this as rarely as we can, because an
         * EGL context is a somewhat heavy object.
         */eglContext1 = eglContextFactory.createContext(egl!!, eglDisplay, eglConfig, eglContext!!.eglContextOld)
        if (eglContext1 == null || eglContext1 === EGL10.EGL_NO_CONTEXT) {
            eglContext1 = null
            throwEglException("createContext", egl!!.eglGetError())
        }
        FileLogger.w("EglHelper", "createContext " + eglContext1 + " tid=" + Thread.currentThread().id)
        eglSurface = null
        val eglContextWrapper = EglContextWrapper()
        eglContextWrapper.eglContextOld = eglContext1!!
        return eglContextWrapper
    }

    /**
     * Create an egl surface for the current SurfaceHolder surface. If a surface
     * already exists, destroy it before creating the new surface.
     *
     * @return true if the surface was created successfully.
     */
    override fun createSurface(surface: Any?): Boolean {
        Loggers.w("EglHelper", "createSurface()  tid=" + Thread.currentThread().id)
        /*
         * Check preconditions.
         */if (egl == null) {
            throw RuntimeException("egl not initialized")
        }
        if (eglDisplay == null) {
            throw RuntimeException("eglDisplay not initialized")
        }
        if (eglConfig == null) {
            throw RuntimeException("mEglConfig not initialized")
        }

        /*
         *  The window size has changed, so we need to create a new
         *  surface.
         */destroySurfaceImp()
        fun throwEglException(function: String) {
            throwEglException(function, egl!!.eglGetError())
        }
        /*
         * Create an EGL surface we can render into.
         */eglSurface = eglWindowSurfaceFactory.createWindowSurface(egl!!,
                eglDisplay, eglConfig, surface)
        if (eglSurface == null || eglSurface === EGL10.EGL_NO_SURFACE) {
            val error = egl!!.eglGetError()
            if (error == EGL10.EGL_BAD_NATIVE_WINDOW) {
                Log.e("EglHelper", "createWindowSurface returned EGL_BAD_NATIVE_WINDOW.")
            }
            return false
        }

        /*
         * Before we can issue GL commands, we need to make sure
         * the context is current and bound to a surface.
         */if (!egl!!.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext1)) {
            /*
             * Could not make the context current, probably because the underlying
             * SurfaceView surface has been destroyed.
             */
            logEglErrorAsWarning("EGLHelper", "eglMakeCurrent", egl!!.eglGetError())
            return false
        }
        return true
    }

    /**
     * Display the current render surface.
     *
     * @return the EGL error code from eglSwapBuffers.
     */
    override fun swap(): Int {
        return if (!egl!!.eglSwapBuffers(eglDisplay, eglSurface)) {
            egl!!.eglGetError()
        } else EGL10.EGL_SUCCESS
    }

    override fun destroySurface() {
        FileLogger.w(TAG, "destroySurface()  tid=" + Thread.currentThread().id)
        destroySurfaceImp()
    }

    private fun destroySurfaceImp() {
        if (eglSurface != null && eglSurface !== EGL10.EGL_NO_SURFACE) {
            egl!!.eglMakeCurrent(eglDisplay, EGL10.EGL_NO_SURFACE,
                    EGL10.EGL_NO_SURFACE,
                    EGL10.EGL_NO_CONTEXT)
            eglWindowSurfaceFactory.destroySurface(egl!!, eglDisplay, eglSurface)
            eglSurface = null
        }
    }

    override fun finish() {
        FileLogger.w(TAG, "finish() tid=" + Thread.currentThread().id)
        if (eglContext1 != null) {
            eglContextFactory.destroyContext(egl!!, eglDisplay!!, eglContext1!!)
            eglContext1 = null
        }
        if (eglDisplay != null) {
            egl!!.eglTerminate(eglDisplay)
            eglDisplay = null
        }
    }

    override fun setPresentationTime(nsecs: Long) {}

    companion object {

        private const val TAG = "EglHelper"

        fun throwEglException(function: String, error: Int) {
            val message = formatEglError(function, error)
            FileLogger.e(TAG, "throwEglException tid=" + Thread.currentThread().id + " "
                    + message)

            throw RuntimeException(message)
        }

        fun logEglErrorAsWarning(tag: String?, function: String, error: Int) {
            Log.w(tag, formatEglError(function, error))
        }

        fun formatEglError(function: String, error: Int): String {
            return function + " failed: " + EGLLogWrapper.getErrorString(error)
        }
    }
}