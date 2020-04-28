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
package jp.eita.canvasgl.textureFilter

import jp.eita.canvasgl.ICanvasGL
import jp.eita.canvasgl.glcanvas.BasicTexture
import jp.eita.canvasgl.glcanvas.GLES20Canvas

open class BasicTextureFilter : TextureFilter {

    override val vertexShader: String
        get() = TEXTURE_VERTEX_SHADER

    override val fragmentShader: String
        get() = TEXTURE_FRAGMENT_SHADER

    override val oesFragmentProgram: String
        get() {
            return """
            #extension GL_OES_EGL_image_external : require
            ${fragmentShader.replace(SAMPLER_2D, SAMPLER_EXTERNAL_OES)}
            """.trimIndent()
        }

    override fun onPreDraw(program: Int, texture: BasicTexture, canvas: ICanvasGL) {}

    override fun destroy() {}

    companion object {

        const val MATRIX_UNIFORM = GLES20Canvas.MATRIX_UNIFORM

        const val TEXTURE_MATRIX_UNIFORM = GLES20Canvas.TEXTURE_MATRIX_UNIFORM

        const val POSITION_ATTRIBUTE = GLES20Canvas.POSITION_ATTRIBUTE

        const val VARYING_TEXTURE_COORD = "vTextureCoord"

        const val TEXTURE_VERTEX_SHADER = (""
                + "uniform mat4 " + MATRIX_UNIFORM + ";\n"
                + "uniform mat4 " + TEXTURE_MATRIX_UNIFORM + ";\n"
                + "attribute vec2 " + POSITION_ATTRIBUTE + ";\n"
                + "varying vec2 " + VARYING_TEXTURE_COORD + ";\n"
                + "void main() {\n"
                + "  vec4 pos = vec4(" + POSITION_ATTRIBUTE + ", 0.0, 1.0);\n"
                + "  gl_Position = " + MATRIX_UNIFORM + " * pos;\n"
                + "  " + VARYING_TEXTURE_COORD + " = (" + TEXTURE_MATRIX_UNIFORM + " * pos).xy;\n"
                + "}\n")

        const val ALPHA_UNIFORM = GLES20Canvas.ALPHA_UNIFORM

        const val TEXTURE_SAMPLER_UNIFORM = GLES20Canvas.TEXTURE_SAMPLER_UNIFORM

        const val SAMPLER_2D = "sampler2D"

        const val TEXTURE_FRAGMENT_SHADER = (""
                + "precision mediump float;\n"
                + "varying vec2 " + VARYING_TEXTURE_COORD + ";\n"
                + "uniform float " + ALPHA_UNIFORM + ";\n"
                + "uniform " + SAMPLER_2D + " " + TEXTURE_SAMPLER_UNIFORM + ";\n"
                + "void main() {\n"
                + "  gl_FragColor = texture2D(" + TEXTURE_SAMPLER_UNIFORM + ", " + VARYING_TEXTURE_COORD + ");\n"
                + "  gl_FragColor *= " + ALPHA_UNIFORM + ";\n"
                + "}\n")

        const val SAMPLER_EXTERNAL_OES = "samplerExternalOES"
    }
}