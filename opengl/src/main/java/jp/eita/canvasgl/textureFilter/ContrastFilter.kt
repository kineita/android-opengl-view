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
import androidx.annotation.FloatRange
import jp.eita.canvasgl.ICanvasGL
import jp.eita.canvasgl.OpenGLUtil.setFloat
import jp.eita.canvasgl.glcanvas.BasicTexture

/**
 * Changes the contrast of the image.<br></br>
 * <br></br>
 * contrast value ranges from 0.0 to 4.0, with 1.0 as the normal level
 */
class ContrastFilter(@param:FloatRange(from = 0.0, to = 4.0) private var contrast: Float) : BasicTextureFilter(), OneValueFilter {

    override val fragmentShader: String
        get() = CONTRAST_FRAGMENT_SHADER

    override fun onPreDraw(program: Int, texture: BasicTexture, canvas: ICanvasGL) {
        super.onPreDraw(program, texture, canvas)
        val contrastLocation = GLES20.glGetUniformLocation(program, UNIFORM_CONTRAST)
        setFloat(contrastLocation, contrast)
    }

    override fun setValue(@FloatRange(from = 0.0, to = 4.0) contrast: Float) {
        this.contrast = contrast
    }

    companion object {

        const val UNIFORM_CONTRAST = "contrast"

        const val CONTRAST_FRAGMENT_SHADER = (""
                + "precision mediump float;\n"
                + "varying vec2 " + VARYING_TEXTURE_COORD + ";\n"
                + "uniform float " + ALPHA_UNIFORM + ";\n"
                + "uniform float " + UNIFORM_CONTRAST + ";\n"
                + "uniform sampler2D " + TEXTURE_SAMPLER_UNIFORM + ";\n"
                + "void main() {\n"
                + "  vec4 textureColor = texture2D(" + TEXTURE_SAMPLER_UNIFORM + ", " + VARYING_TEXTURE_COORD + ");\n"
                + "  gl_FragColor = vec4(((textureColor.rgb - vec3(0.5)) * " + UNIFORM_CONTRAST + "+ vec3(0.5)), textureColor.w);\n"
                + "  gl_FragColor *= " + ALPHA_UNIFORM + ";\n"
                + "}\n")
    }

}