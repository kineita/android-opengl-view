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

import android.graphics.Bitmap
import android.graphics.RectF
import android.opengl.GLES20
import android.opengl.Matrix
import jp.eita.canvasgl.ICanvasGL
import jp.eita.canvasgl.glcanvas.BasicTexture
import jp.eita.canvasgl.glcanvas.GLES20Canvas.Companion.checkError
import jp.eita.canvasgl.glcanvas.GLES20Canvas.Companion.printMatrix
import jp.eita.canvasgl.glcanvas.TextureMatrixTransformer.convertCoordinate
import jp.eita.canvasgl.glcanvas.TextureMatrixTransformer.copyTextureCoordinates
import jp.eita.canvasgl.glcanvas.TextureMatrixTransformer.setTextureMatrix
import jp.eita.canvasgl.glview.texture.GLTexture
import java.util.*

/**
 * A filter that supports mix two textures.
 * This accept the second texture as a RawTexture or a Bitmap
 */
abstract class TwoTextureFilter : BasicTextureFilter {

    protected val mTempTextureMatrix = FloatArray(16)

    protected var secondBitmap: Bitmap? = null

    /**
     * Set the second texture. If secondBitmap exists, this will be ignored.
     *
     * @param secondRawTexture The second texture. If secondBitmap exists, this will be ignored
     */
    protected var secondRawTexture: GLTexture? = null

    private val mTempSrcRectF = RectF()

    override val vertexShader: String
        get() = VERTEX_SHADER

    /**
     * Set the second bitmap. If this exists, the secondRawTexture will be ignored
     *
     * @param secondBitmap The second bitmap that will be mixed
     */
    constructor(secondBitmap: Bitmap) {
        this.secondBitmap = secondBitmap
    }

    constructor()

    fun setBitmap(secondBitmap: Bitmap) {
        this.secondBitmap = secondBitmap
    }

    private fun resetMatrix() {
        Arrays.fill(mTempTextureMatrix, 0f)
    }

    override fun onPreDraw(program: Int, texture: BasicTexture, canvas: ICanvasGL) {
        super.onPreDraw(program, texture, canvas)
        if (useSecondBitmap()) {
            handleSecondBitmapTexture(program, canvas)
            return
        }
        if (secondRawTexture != null) {
            handleSecondRawTexture(program, canvas)
        }
    }

    private fun handleSecondBitmapTexture(program: Int, canvas: ICanvasGL) {
        val secondTexture: BasicTexture? = canvas.bindBitmapToTexture(GLES20.GL_TEXTURE3, secondBitmap!!)
        resetMatrix()
        Matrix.setIdentityM(mTempTextureMatrix, 0)
        copyTextureCoordinates(secondTexture!!, mTempSrcRectF)
        convertCoordinate(mTempSrcRectF, secondTexture)
        setTextureMatrix(mTempSrcRectF, mTempTextureMatrix)
        printMatrix("two tex matrix", mTempTextureMatrix, 0)
        val textureMatrixPosition = GLES20.glGetUniformLocation(program, TEXTURE_MATRIX_UNIFORM2)
        GLES20.glUniformMatrix4fv(textureMatrixPosition, 1, false, mTempTextureMatrix, 0)
        val sampler2 = GLES20.glGetUniformLocation(program, UNIFORM_TEXTURE_SAMPLER2)
        checkError()
        GLES20.glUniform1i(sampler2, 3) // match GL_TEXTURE3
        checkError()
    }

    private fun handleSecondRawTexture(program: Int, canvas: ICanvasGL) {
        val rawTexture = secondRawTexture!!.rawTexture
        canvas.bindRawTexture(GLES20.GL_TEXTURE3, rawTexture)
        resetMatrix()
        secondRawTexture!!.surfaceTexture.getTransformMatrix(mTempTextureMatrix)
        val textureMatrixPosition = GLES20.glGetUniformLocation(program, TEXTURE_MATRIX_UNIFORM2)
        GLES20.glUniformMatrix4fv(textureMatrixPosition, 1, false, mTempTextureMatrix, 0)
        val sampler2 = GLES20.glGetUniformLocation(program, UNIFORM_TEXTURE_SAMPLER2)
        checkError()
        GLES20.glUniform1i(sampler2, 3)
        checkError()
    }

    override val oesFragmentProgram: String
        get() {
            return if (useSecondBitmap()) {
                """
             #extension GL_OES_EGL_image_external : require
             ${fragmentShader.replaceFirst(SAMPLER_2D.toRegex(), SAMPLER_EXTERNAL_OES)}
             """.trimIndent()
            } else """
             #extension GL_OES_EGL_image_external : require
             ${fragmentShader.replace(SAMPLER_2D.toRegex(), SAMPLER_EXTERNAL_OES)}
             """.trimIndent()
        }

    private fun useSecondBitmap(): Boolean {
        return secondBitmap != null
    }

    companion object {

        const val VARYING_TEXTURE_COORD2 = "vTextureCoord2"

        const val UNIFORM_TEXTURE_SAMPLER2 = "uTextureSampler2"

        private const val TEXTURE_MATRIX_UNIFORM2 = "uTextureMatrix2"

        private const val VERTEX_SHADER = " \n" +
                "attribute vec2 " + POSITION_ATTRIBUTE + ";\n" +
                "varying vec2 " + VARYING_TEXTURE_COORD + ";\n" +
                "varying vec2 " + VARYING_TEXTURE_COORD2 + ";\n" +
                "uniform mat4 " + MATRIX_UNIFORM + ";\n" +
                "uniform mat4 " + TEXTURE_MATRIX_UNIFORM + ";\n" +
                "uniform mat4 " + TEXTURE_MATRIX_UNIFORM2 + ";\n" +
                " \n" +
                "void main() {\n" +
                "  vec4 pos = vec4(" + POSITION_ATTRIBUTE + ", 0.0, 1.0);\n" +
                "    gl_Position = " + MATRIX_UNIFORM + " * pos;\n" +
                "    " + VARYING_TEXTURE_COORD + " = (" + TEXTURE_MATRIX_UNIFORM + " * pos).xy;\n" +
                "    " + VARYING_TEXTURE_COORD2 + " = (" + TEXTURE_MATRIX_UNIFORM2 + " * pos).xy;\n" +
                "}"
    }
}