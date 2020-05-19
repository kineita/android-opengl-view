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

import android.opengl.EGL14
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLContext

open class EglContextWrapper {

    open var eglContextOld: EGLContext = EGL10.EGL_NO_CONTEXT

    open var eglContext: android.opengl.EGLContext = EGL14.EGL_NO_CONTEXT

    class EGLNoContextWrapper : EglContextWrapper() {

        internal fun setEglContext(eglContext: android.opengl.EGLContext?) {

        }

        internal fun setEglContextOld(eglContextOld: EGLContext) {

        }
    }

    companion object {

        val EGL_NO_CONTEXT_WRAPPER: EglContextWrapper = EGLNoContextWrapper()
    }
}