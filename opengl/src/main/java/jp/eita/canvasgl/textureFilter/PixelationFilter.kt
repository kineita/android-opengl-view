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

class PixelationFilter(@param:FloatRange(from = 1.0, to = 100.0) private var mPixel: Float) : BasicTextureFilter(), OneValueFilter {

    private var mImageWidthFactorLocation = 0

    private var mImageHeightFactorLocation = 0

    private var mPixelLocation = 0

    override val fragmentShader: String
        get() = PIXELATION_FRAGMENT_SHADER

    override fun onPreDraw(program: Int, texture: BasicTexture, canvas: ICanvasGL) {
        super.onPreDraw(program, texture, canvas)
        mImageWidthFactorLocation = GLES20.glGetUniformLocation(program, UNIFORM_IMAGE_WIDTH_FACTOR)
        mImageHeightFactorLocation = GLES20.glGetUniformLocation(program, UNIFORM_IMAGE_HEIGHT_FACTOR)
        mPixelLocation = GLES20.glGetUniformLocation(program, UNIFORM_PIXEL)
        setFloat(mImageWidthFactorLocation, 1.0f / texture.width)
        setFloat(mImageHeightFactorLocation, 1.0f / texture.height)
        setFloat(mPixelLocation, mPixel)
    }

    override fun setValue(@FloatRange(from = 1.0, to = 100.0) value: Float) {
        mPixel = value
    }

    companion object {

        const val UNIFORM_IMAGE_WIDTH_FACTOR = "imageWidthFactor"

        const val UNIFORM_IMAGE_HEIGHT_FACTOR = "imageHeightFactor"

        const val UNIFORM_PIXEL = "pixel"

        const val PIXELATION_FRAGMENT_SHADER = "" +
                "precision highp float;\n" +
                " varying vec2 " + VARYING_TEXTURE_COORD + ";\n" +
                "uniform float " + UNIFORM_IMAGE_WIDTH_FACTOR + ";\n" +
                "uniform float " + UNIFORM_IMAGE_HEIGHT_FACTOR + ";\n" +
                " uniform float " + ALPHA_UNIFORM + ";\n" +
                "uniform sampler2D " + TEXTURE_SAMPLER_UNIFORM + ";\n" +
                "uniform float " + UNIFORM_PIXEL + ";\n" +
                "void main() {\n" +
                "" +
                "  vec2 uv  = " + VARYING_TEXTURE_COORD + ".xy;\n" +
                "  float dx = " + UNIFORM_PIXEL + " * " + UNIFORM_IMAGE_WIDTH_FACTOR + ";\n" +
                "  float dy = " + UNIFORM_PIXEL + " * " + UNIFORM_IMAGE_HEIGHT_FACTOR + ";\n" +
                "  vec2 coord = vec2(dx * floor(uv.x / dx), dy * floor(uv.y / dy));\n" +
                "  vec4 tc = texture2D(" + TEXTURE_SAMPLER_UNIFORM + ", coord);\n" +
                "  gl_FragColor = vec4(tc);\n" +
                "    gl_FragColor *= " + ALPHA_UNIFORM + ";\n" +
                "}"
    }

}