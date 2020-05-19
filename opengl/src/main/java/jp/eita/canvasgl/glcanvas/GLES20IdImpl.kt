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

import android.opengl.GLES20

class GLES20IdImpl : GLId {

    private val tempIntArray: IntArray = IntArray(1)

    override fun generateTexture(): Int {
        GLES20.glGenTextures(1, tempIntArray, 0)
        GLES20Canvas.checkError()
        return tempIntArray[0]
    }

    override fun glGenBuffers(n: Int, buffers: IntArray, offset: Int) {
        GLES20.glGenBuffers(n, buffers, offset)
        GLES20Canvas.checkError()
    }

    override fun glDeleteTextures(n: Int, textures: IntArray, offset: Int) {
        GLES20.glDeleteTextures(n, textures, offset)
        GLES20Canvas.checkError()
    }

    override fun glDeleteBuffers(n: Int, buffers: IntArray, offset: Int) {
        GLES20.glDeleteBuffers(n, buffers, offset)
        GLES20Canvas.checkError()
    }

    override fun glDeleteFrameBuffers(n: Int, buffers: IntArray, offset: Int) {
        GLES20.glDeleteFramebuffers(n, buffers, offset)
        GLES20Canvas.checkError()
    }
}