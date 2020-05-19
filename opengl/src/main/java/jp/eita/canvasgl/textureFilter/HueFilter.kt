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

class HueFilter(@FloatRange(from = 0.0, to = 360.0) hue: Float) : BasicTextureFilter(), OneValueFilter {

    private var mHueLocation = 0

    private var hueAdjust: Float = hue % 360.0f * Math.PI.toFloat() / 180.0f

    override val fragmentShader: String
        get() = HUE_FRAGMENT_SHADER

    override fun onPreDraw(program: Int, texture: BasicTexture, canvas: ICanvasGL) {
        super.onPreDraw(program, texture, canvas)
        mHueLocation = GLES20.glGetUniformLocation(program, UNIFORM_HUE)
        setFloat(mHueLocation, hueAdjust)
    }

    override fun setValue(@FloatRange(from = 0.0, to = 360.0) value: Float) {
        hueAdjust = value % 360.0f * Math.PI.toFloat() / 180.0f
    }

    companion object {

        const val UNIFORM_HUE = "hueAdjust"

        const val HUE_FRAGMENT_SHADER = "" +
                "precision highp float;\n" +
                "varying vec2 " + VARYING_TEXTURE_COORD + ";\n" +
                " uniform float " + ALPHA_UNIFORM + ";\n" +
                "\n" +
                "uniform sampler2D " + TEXTURE_SAMPLER_UNIFORM + ";\n" +
                "uniform mediump float " + UNIFORM_HUE + ";\n" +
                "const highp vec4 kRGBToYPrime = vec4 (0.299, 0.587, 0.114, 0.0);\n" +
                "const highp vec4 kRGBToI = vec4 (0.595716, -0.274453, -0.321263, 0.0);\n" +
                "const highp vec4 kRGBToQ = vec4 (0.211456, -0.522591, 0.31135, 0.0);\n" +
                "\n" +
                "const highp vec4 kYIQToR = vec4 (1.0, 0.9563, 0.6210, 0.0);\n" +
                "const highp vec4 kYIQToG = vec4 (1.0, -0.2721, -0.6474, 0.0);\n" +
                "const highp vec4 kYIQToB = vec4 (1.0, -1.1070, 1.7046, 0.0);\n" +
                "\n" +
                "void main (){\n" +
                "" +
                "    // Sample the input pixel\n" +
                "    highp vec4 color = texture2D(" + TEXTURE_SAMPLER_UNIFORM + ", " + VARYING_TEXTURE_COORD + ");\n" +
                "\n" +
                "    // Convert to YIQ\n" +
                "    highp float YPrime = dot (color, kRGBToYPrime);\n" +
                "    highp float I = dot (color, kRGBToI);\n" +
                "    highp float Q = dot (color, kRGBToQ);\n" +
                "\n" +
                "    // Calculate the hue and chroma\n" +
                "    highp float hue = atan (Q, I);\n" +
                "    highp float chroma = sqrt (I * I + Q * Q);\n" +
                "\n" +
                "    // Make the user's adjustments\n" +
                "    hue += (-" + UNIFORM_HUE + "); //why negative rotation?\n" +
                "\n" +
                "    // Convert back to YIQ\n" +
                "    Q = chroma * sin (hue);\n" +
                "    I = chroma * cos (hue);\n" +
                "\n" +
                "    // Convert back to RGB\n" +
                "    highp vec4 yIQ = vec4 (YPrime, I, Q, 0.0);\n" +
                "    color.r = dot (yIQ, kYIQToR);\n" +
                "    color.g = dot (yIQ, kYIQToG);\n" +
                "    color.b = dot (yIQ, kYIQToB);\n" +
                "\n" +
                "    // Save the result\n" +
                "    gl_FragColor = color;\n" +
                "    gl_FragColor *= " + ALPHA_UNIFORM + ";\n" +
                "}\n"
    }
}