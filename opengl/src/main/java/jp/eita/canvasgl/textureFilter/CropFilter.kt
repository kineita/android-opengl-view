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

import android.opengl.GLES20
import jp.eita.canvasgl.ICanvasGL
import jp.eita.canvasgl.OpenGLUtil.setFloat
import jp.eita.canvasgl.glcanvas.BasicTexture

class CropFilter(var left: Float, var top: Float, var right: Float, var bottom: Float) : BasicTextureFilter() {

    override val fragmentShader: String
        get() = CROP_FRAGMENT_SHADER

    override fun onPreDraw(program: Int, texture: BasicTexture, canvas: ICanvasGL) {
        super.onPreDraw(program, texture, canvas)
        setFloat(GLES20.glGetUniformLocation(program, UNIFORM_LEFT), left)
        setFloat(GLES20.glGetUniformLocation(program, UNIFORM_TOP), top)
        setFloat(GLES20.glGetUniformLocation(program, UNIFORM_RIGHT), right)
        setFloat(GLES20.glGetUniformLocation(program, UNIFORM_BOTTOM), bottom)
    }

    companion object {

        private const val UNIFORM_LEFT = "left"

        private const val UNIFORM_TOP = "top"

        private const val UNIFORM_RIGHT = "right"

        private const val UNIFORM_BOTTOM = "bottom"

        private const val CROP_FRAGMENT_SHADER = (""
                + "precision mediump float;\n"
                + "varying vec2 " + VARYING_TEXTURE_COORD + ";\n"
                + "uniform float " + ALPHA_UNIFORM + ";\n"
                + "uniform " + SAMPLER_2D + " " + TEXTURE_SAMPLER_UNIFORM + ";\n"
                + "  uniform highp float " + UNIFORM_LEFT + ";\n"
                + "  uniform highp float " + UNIFORM_TOP + ";\n"
                + "  uniform highp float " + UNIFORM_RIGHT + ";\n"
                + "  uniform highp float " + UNIFORM_BOTTOM + ";\n"
                + "void main() {\n"
                + "if( " + VARYING_TEXTURE_COORD + ".x > " + UNIFORM_LEFT + " &&  " + VARYING_TEXTURE_COORD + ".x < " + UNIFORM_RIGHT +
                " &&  " + VARYING_TEXTURE_COORD + ".y > " + UNIFORM_TOP + " &&  " + VARYING_TEXTURE_COORD + ".y < " + UNIFORM_BOTTOM + ") {"
                + " gl_FragColor = texture2D(" + TEXTURE_SAMPLER_UNIFORM + ", " + VARYING_TEXTURE_COORD + ");\n"
                + "} else {"
                + " gl_FragColor = " + "vec4(0, 0, 0, 0)" + ";\n"
                + "}"
                + "}\n")

        fun calc(wantCoord: Int, size: Int): Float {
            return wantCoord.toFloat() / size
        }
    }

}