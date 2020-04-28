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

// This mimics corresponding GL functions.
interface GLId {

    fun generateTexture(): Int

    fun glGenBuffers(n: Int, buffers: IntArray, offset: Int)

    fun glDeleteTextures(n: Int, textures: IntArray, offset: Int)

    fun glDeleteBuffers(n: Int, buffers: IntArray, offset: Int)

    fun glDeleteFrameBuffers(n: Int, buffers: IntArray, offset: Int)
}