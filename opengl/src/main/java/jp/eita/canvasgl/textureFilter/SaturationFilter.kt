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
import jp.eita.canvasgl.util.OpenGLUtil.setFloat
import jp.eita.canvasgl.glcanvas.BasicTexture

/**
 * saturation: The degree of saturation or desaturation to apply to the image (0.0 - 2.0, with 1.0 as the default)
 */
class SaturationFilter(@param:FloatRange(from = 0.0, to = 2.0) private var mSaturation: Float) : BasicTextureFilter(), OneValueFilter {

    private var mSaturationLocation = 0

    override val fragmentShader: String
        get() = SATURATION_FRAGMENT_SHADER

    override fun onPreDraw(program: Int, texture: BasicTexture, canvas: ICanvasGL) {
        super.onPreDraw(program, texture, canvas)
        mSaturationLocation = GLES20.glGetUniformLocation(program, UNIFORM_SATURATION)
        setFloat(mSaturationLocation, mSaturation)
    }

    override fun setValue(@FloatRange(from = 0.0, to = 2.0) value: Float) {
        mSaturation = value
    }

    companion object {

        const val UNIFORM_SATURATION = "saturation"

        const val SATURATION_FRAGMENT_SHADER = "" +
                "precision mediump float;\n" +
                " varying vec2 " + VARYING_TEXTURE_COORD + ";\n" +
                " \n" +
                " uniform sampler2D " + TEXTURE_SAMPLER_UNIFORM + ";\n" +
                " uniform float " + ALPHA_UNIFORM + ";\n" +
                " uniform float " + UNIFORM_SATURATION + ";\n" +
                " \n" +
                " // Values from \"Graphics Shaders: Theory and Practice\" by Bailey and Cunningham\n" +
                " const vec3 luminanceWeighting = vec3(0.2125, 0.7154, 0.0721);\n" +
                " \n" +
                " void main() {\n" +
                " " +
                "    vec4 textureColor = texture2D(" + TEXTURE_SAMPLER_UNIFORM + ", " + VARYING_TEXTURE_COORD + ");\n" +
                "    float luminance = dot(textureColor.rgb, luminanceWeighting);\n" +
                "    vec3 greyScaleColor = vec3(luminance);\n" +
                "    \n" +
                "    gl_FragColor = vec4(mix(greyScaleColor, textureColor.rgb, " + UNIFORM_SATURATION + "), textureColor.w);\n" +
                "    gl_FragColor *= " + ALPHA_UNIFORM + ";\n" +
                " }"
    }

}