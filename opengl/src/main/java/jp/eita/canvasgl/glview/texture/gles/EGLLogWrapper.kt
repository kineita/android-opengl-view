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

import android.opengl.GLDebugHelper
import android.opengl.GLException
import java.io.IOException
import java.io.Writer
import javax.microedition.khronos.egl.*

/**
 * For Test
 */
class EGLLogWrapper(egl: EGL, configFlags: Int, var log: Writer?) : EGL11 {

    var logArgumentNames: Boolean = GLDebugHelper.CONFIG_LOG_ARGUMENT_NAMES and configFlags != 0

    var checkError: Boolean = GLDebugHelper.CONFIG_CHECK_GL_ERROR and configFlags != 0

    private val egl10: EGL10 = egl as EGL10

    private var argCount = 0

    override fun eglChooseConfig(display: EGLDisplay, attrib_list: IntArray,
                                 configs: Array<EGLConfig>, config_size: Int, num_config: IntArray): Boolean {
        begin("eglChooseConfig")
        arg("display", display)
        arg("attrib_list", attrib_list)
        arg("config_size", config_size)
        end()
        val result = egl10.eglChooseConfig(display, attrib_list, configs,
                config_size, num_config)
        arg("configs", configs)
        arg("num_config", num_config)
        returns(result)
        checkError()
        return result
    }

    override fun eglCopyBuffers(display: EGLDisplay, surface: EGLSurface,
                                native_pixmap: Any): Boolean {
        begin("eglCopyBuffers")
        arg("display", display)
        arg("surface", surface)
        arg("native_pixmap", native_pixmap)
        end()
        val result = egl10.eglCopyBuffers(display, surface, native_pixmap)
        returns(result)
        checkError()
        return result
    }

    override fun eglCreateContext(display: EGLDisplay, config: EGLConfig,
                                  share_context: EGLContext, attrib_list: IntArray): EGLContext {
        begin("eglCreateContext")
        arg("display", display)
        arg("config", config)
        arg("share_context", share_context)
        arg("attrib_list", attrib_list)
        end()
        val result = egl10.eglCreateContext(display, config,
                share_context, attrib_list)
        returns(result)
        checkError()
        return result
    }

    override fun eglCreatePbufferSurface(display: EGLDisplay,
                                         config: EGLConfig, attrib_list: IntArray): EGLSurface {
        begin("eglCreatePbufferSurface")
        arg("display", display)
        arg("config", config)
        arg("attrib_list", attrib_list)
        end()
        val result = egl10.eglCreatePbufferSurface(display, config,
                attrib_list)
        returns(result)
        checkError()
        return result
    }

    override fun eglCreatePixmapSurface(display: EGLDisplay,
                                        config: EGLConfig, native_pixmap: Any, attrib_list: IntArray): EGLSurface {
        begin("eglCreatePixmapSurface")
        arg("display", display)
        arg("config", config)
        arg("native_pixmap", native_pixmap)
        arg("attrib_list", attrib_list)
        end()
        val result = egl10.eglCreatePixmapSurface(display, config,
                native_pixmap, attrib_list)
        returns(result)
        checkError()
        return result
    }

    override fun eglCreateWindowSurface(display: EGLDisplay,
                                        config: EGLConfig, native_window: Any, attrib_list: IntArray): EGLSurface {
        begin("eglCreateWindowSurface")
        arg("display", display)
        arg("config", config)
        arg("native_window", native_window)
        arg("attrib_list", attrib_list)
        end()
        val result = egl10.eglCreateWindowSurface(display, config,
                native_window, attrib_list)
        returns(result)
        checkError()
        return result
    }

    override fun eglDestroyContext(display: EGLDisplay, context: EGLContext): Boolean {
        begin("eglDestroyContext")
        arg("display", display)
        arg("context", context)
        end()
        val result = egl10.eglDestroyContext(display, context)
        returns(result)
        checkError()
        return result
    }

    override fun eglDestroySurface(display: EGLDisplay, surface: EGLSurface): Boolean {
        begin("eglDestroySurface")
        arg("display", display)
        arg("surface", surface)
        end()
        val result = egl10.eglDestroySurface(display, surface)
        returns(result)
        checkError()
        return result
    }

    override fun eglGetConfigAttrib(display: EGLDisplay, config: EGLConfig,
                                    attribute: Int, value: IntArray): Boolean {
        begin("eglGetConfigAttrib")
        arg("display", display)
        arg("config", config)
        arg("attribute", attribute)
        end()
        val result = egl10.eglGetConfigAttrib(display, config, attribute,
                value)
        arg("value", value)
        returns(result)
        checkError()
        return false
    }

    override fun eglGetConfigs(display: EGLDisplay, configs: Array<EGLConfig>,
                               config_size: Int, num_config: IntArray): Boolean {
        begin("eglGetConfigs")
        arg("display", display)
        arg("config_size", config_size)
        end()
        val result = egl10.eglGetConfigs(display, configs, config_size,
                num_config)
        arg("configs", configs)
        arg("num_config", num_config)
        returns(result)
        checkError()
        return result
    }

    override fun eglGetCurrentContext(): EGLContext {
        begin("eglGetCurrentContext")
        end()
        val result = egl10.eglGetCurrentContext()
        returns(result)
        checkError()
        return result
    }

    override fun eglGetCurrentDisplay(): EGLDisplay {
        begin("eglGetCurrentDisplay")
        end()
        val result = egl10.eglGetCurrentDisplay()
        returns(result)
        checkError()
        return result
    }

    override fun eglGetCurrentSurface(readdraw: Int): EGLSurface {
        begin("eglGetCurrentSurface")
        arg("readdraw", readdraw)
        end()
        val result = egl10.eglGetCurrentSurface(readdraw)
        returns(result)
        checkError()
        return result
    }

    override fun eglGetDisplay(native_display: Any): EGLDisplay {
        begin("eglGetDisplay")
        arg("native_display", native_display)
        end()
        val result = egl10.eglGetDisplay(native_display)
        returns(result)
        checkError()
        return result
    }

    override fun eglGetError(): Int {
        begin("eglGetError")
        end()
        val result = egl10.eglGetError()
        returns(getErrorString(result))
        return result
    }

    override fun eglInitialize(display: EGLDisplay, major_minor: IntArray): Boolean {
        begin("eglInitialize")
        arg("display", display)
        end()
        val result = egl10.eglInitialize(display, major_minor)
        returns(result)
        arg("major_minor", major_minor)
        checkError()
        return result
    }

    override fun eglMakeCurrent(display: EGLDisplay, draw: EGLSurface,
                                read: EGLSurface, context: EGLContext): Boolean {
        begin("eglMakeCurrent")
        arg("display", display)
        arg("draw", draw)
        arg("read", read)
        arg("context", context)
        end()
        val result = egl10.eglMakeCurrent(display, draw, read, context)
        returns(result)
        checkError()
        return result
    }

    override fun eglQueryContext(display: EGLDisplay, context: EGLContext,
                                 attribute: Int, value: IntArray): Boolean {
        begin("eglQueryContext")
        arg("display", display)
        arg("context", context)
        arg("attribute", attribute)
        end()
        val result = egl10.eglQueryContext(display, context, attribute,
                value)
        returns(value[0])
        returns(result)
        checkError()
        return result
    }

    override fun eglQueryString(display: EGLDisplay, name: Int): String {
        begin("eglQueryString")
        arg("display", display)
        arg("name", name)
        end()
        val result = egl10.eglQueryString(display, name)
        returns(result)
        checkError()
        return result
    }

    override fun eglQuerySurface(display: EGLDisplay, surface: EGLSurface,
                                 attribute: Int, value: IntArray): Boolean {
        begin("eglQuerySurface")
        arg("display", display)
        arg("surface", surface)
        arg("attribute", attribute)
        end()
        val result = egl10.eglQuerySurface(display, surface, attribute,
                value)
        returns(value[0])
        returns(result)
        checkError()
        return result
    }

    override fun eglSwapBuffers(display: EGLDisplay, surface: EGLSurface): Boolean {
        begin("eglSwapBuffers")
        arg("display", display)
        arg("surface", surface)
        end()
        val result = egl10.eglSwapBuffers(display, surface)
        returns(result)
        checkError()
        return result
    }

    override fun eglTerminate(display: EGLDisplay): Boolean {
        begin("eglTerminate")
        arg("display", display)
        end()
        val result = egl10.eglTerminate(display)
        returns(result)
        checkError()
        return result
    }

    override fun eglWaitGL(): Boolean {
        begin("eglWaitGL")
        end()
        val result = egl10.eglWaitGL()
        returns(result)
        checkError()
        return result
    }

    override fun eglWaitNative(engine: Int, bindTarget: Any): Boolean {
        begin("eglWaitNative")
        arg("engine", engine)
        arg("bindTarget", bindTarget)
        end()
        val result = egl10.eglWaitNative(engine, bindTarget)
        returns(result)
        checkError()
        return result
    }

    private fun checkError() {
        var eglError: Int
        if (egl10.eglGetError().also { eglError = it } != EGL10.EGL_SUCCESS) {
            val errorMessage = "eglError: " + getErrorString(eglError)
            logLine(errorMessage)
            if (checkError) {
                throw GLException(eglError, errorMessage)
            }
        }
    }

    private fun logLine(message: String) {
        log("""
    $message

    """.trimIndent())
    }

    private fun log(message: String) {
        try {
            log!!.write(message)
        } catch (e: IOException) {
            // Ignore exception, keep on trying
        }
    }

    private fun begin(name: String) {
        log("$name(")
        argCount = 0
    }

    private fun arg(name: String, value: String) {
        if (argCount++ > 0) {
            log(", ")
        }
        if (logArgumentNames) {
            log("$name=")
        }
        log(value)
    }

    private fun end() {
        log(");\n")
        flush()
    }

    private fun flush() {
        try {
            log!!.flush()
        } catch (e: IOException) {
            log = null
        }
    }

    private fun arg(name: String, value: Int) {
        arg(name, value.toString())
    }

    private fun arg(name: String, `object`: Any) {
        arg(name, toString(`object`))
    }

    private fun arg(name: String, `object`: EGLDisplay) {
        if (`object` === EGL10.EGL_DEFAULT_DISPLAY) {
            arg(name, "EGL10.EGL_DEFAULT_DISPLAY")
        } else if (`object` === EGL10.EGL_NO_DISPLAY) {
            arg(name, "EGL10.EGL_NO_DISPLAY")
        } else {
            arg(name, toString(`object`))
        }
    }

    private fun arg(name: String, `object`: EGLContext) {
        if (`object` === EGL10.EGL_NO_CONTEXT) {
            arg(name, "EGL10.EGL_NO_CONTEXT")
        } else {
            arg(name, toString(`object`))
        }
    }

    private fun arg(name: String, `object`: EGLSurface) {
        if (`object` === EGL10.EGL_NO_SURFACE) {
            arg(name, "EGL10.EGL_NO_SURFACE")
        } else {
            arg(name, toString(`object`))
        }
    }

    private fun returns(result: String) {
        log(" returns $result;\n")
        flush()
    }

    private fun returns(result: Int) {
        returns(result.toString())
    }

    private fun returns(result: Boolean) {
        returns(java.lang.Boolean.toString(result))
    }

    private fun returns(result: Any) {
        returns(toString(result))
    }

    private fun toString(obj: Any?): String {
        return obj?.toString() ?: "null"
    }

    private fun arg(name: String, arr: IntArray) {
        if (arr == null) {
            arg(name, "null")
        } else {
            arg(name, toString(arr.size, arr, 0))
        }
    }

    private fun arg(name: String, arr: Array<Any>?) {
        if (arr == null) {
            arg(name, "null")
        } else {
            arg(name, toString(arr.size, arr, 0))
        }
    }

    private fun toString(n: Int, arr: IntArray, offset: Int): String {
        val buf = StringBuilder()
        buf.append("{\n")
        val arrLen = arr.size
        for (i in 0 until n) {
            val index = offset + i
            buf.append(" [").append(index).append("] = ")
            if (index < 0 || index >= arrLen) {
                buf.append("out of bounds")
            } else {
                buf.append(arr[index])
            }
            buf.append('\n')
        }
        buf.append("}")
        return buf.toString()
    }

    private fun toString(n: Int, arr: Array<Any>, offset: Int): String {
        val buf = StringBuilder()
        buf.append("{\n")
        val arrLen = arr.size
        for (i in 0 until n) {
            val index = offset + i
            buf.append(" [").append(index).append("] = ")
            if (index < 0 || index >= arrLen) {
                buf.append("out of bounds")
            } else {
                buf.append(arr[index])
            }
            buf.append('\n')
        }
        buf.append("}")
        return buf.toString()
    }

    companion object {
        private fun getHex(value: Int): String {
            return "0x" + Integer.toHexString(value)
        }

        fun getErrorString(error: Int): String {
            return when (error) {
                EGL10.EGL_SUCCESS -> "EGL_SUCCESS"
                EGL10.EGL_NOT_INITIALIZED -> "EGL_NOT_INITIALIZED"
                EGL10.EGL_BAD_ACCESS -> "EGL_BAD_ACCESS"
                EGL10.EGL_BAD_ALLOC -> "EGL_BAD_ALLOC"
                EGL10.EGL_BAD_ATTRIBUTE -> "EGL_BAD_ATTRIBUTE"
                EGL10.EGL_BAD_CONFIG -> "EGL_BAD_CONFIG"
                EGL10.EGL_BAD_CONTEXT -> "EGL_BAD_CONTEXT"
                EGL10.EGL_BAD_CURRENT_SURFACE -> "EGL_BAD_CURRENT_SURFACE"
                EGL10.EGL_BAD_DISPLAY -> "EGL_BAD_DISPLAY"
                EGL10.EGL_BAD_MATCH -> "EGL_BAD_MATCH"
                EGL10.EGL_BAD_NATIVE_PIXMAP -> "EGL_BAD_NATIVE_PIXMAP"
                EGL10.EGL_BAD_NATIVE_WINDOW -> "EGL_BAD_NATIVE_WINDOW"
                EGL10.EGL_BAD_PARAMETER -> "EGL_BAD_PARAMETER"
                EGL10.EGL_BAD_SURFACE -> "EGL_BAD_SURFACE"
                EGL11.EGL_CONTEXT_LOST -> "EGL_CONTEXT_LOST"
                else -> getHex(error)
            }
        }
    }
}