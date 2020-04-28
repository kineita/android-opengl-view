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

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix
import android.util.Log
import jp.eita.canvasgl.glcanvas.GLCanvas.*
import jp.eita.canvasgl.glcanvas.TextureMatrixTransformer.convertCoordinate
import jp.eita.canvasgl.glcanvas.TextureMatrixTransformer.copyTextureCoordinates
import jp.eita.canvasgl.glcanvas.TextureMatrixTransformer.setTextureMatrix
import jp.eita.canvasgl.shapeFilter.BasicDrawShapeFilter
import jp.eita.canvasgl.shapeFilter.DrawShapeFilter
import jp.eita.canvasgl.textureFilter.BasicTextureFilter
import jp.eita.canvasgl.textureFilter.TextureFilter
import jp.eita.canvasgl.util.Loggers
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * drawRect, drawLine, drawCircle --> prepareDraw --> [GLES20Canvas.draw]
 * drawTexture --> setupTextureFilter --> drawTextureRect --> prepareTexture
 * --> setPosition  --> draw --> setMatrix
 */
class GLES20Canvas : GLCanvas {

    private val mUnboundTextures = IntArrayCustom()

    private val mDeleteBuffers = IntArrayCustom()

    // Temporary variables used within calculations
    private val mTempMatrix = FloatArray(32)

    private val mTempColor = FloatArray(4)

    private val mTempSourceRect = RectF()

    private val mTempTargetRect = RectF()

    private val mTempTextureMatrix = FloatArray(MATRIX_SIZE)

    private val mTempIntArray = IntArray(1)

    private var mDrawParameters = arrayOf(
            AttributeShaderParameter(POSITION_ATTRIBUTE),  // INDEX_POSITION
            UniformShaderParameter(MATRIX_UNIFORM),  // INDEX_MATRIX
            UniformShaderParameter(COLOR_UNIFORM))

    private var mTextureParameters = arrayOf(
            AttributeShaderParameter(POSITION_ATTRIBUTE),  // INDEX_POSITION
            UniformShaderParameter(MATRIX_UNIFORM),  // INDEX_MATRIX
            UniformShaderParameter(TEXTURE_MATRIX_UNIFORM),  // INDEX_TEXTURE_MATRIX
            UniformShaderParameter(TEXTURE_SAMPLER_UNIFORM),  // INDEX_TEXTURE_SAMPLER
            UniformShaderParameter(ALPHA_UNIFORM))

    private var mOesTextureParameters = arrayOf(
            AttributeShaderParameter(POSITION_ATTRIBUTE),  // INDEX_POSITION
            UniformShaderParameter(MATRIX_UNIFORM),  // INDEX_MATRIX
            UniformShaderParameter(TEXTURE_MATRIX_UNIFORM),  // INDEX_TEXTURE_MATRIX
            UniformShaderParameter(TEXTURE_SAMPLER_UNIFORM),  // INDEX_TEXTURE_SAMPLER
            UniformShaderParameter(ALPHA_UNIFORM))

    private var mMeshParameters = arrayOf(
            AttributeShaderParameter(POSITION_ATTRIBUTE),  // INDEX_POSITION
            UniformShaderParameter(MATRIX_UNIFORM),  // INDEX_MATRIX
            AttributeShaderParameter(TEXTURE_COORD_ATTRIBUTE),  // INDEX_TEXTURE_COORD
            UniformShaderParameter(TEXTURE_SAMPLER_UNIFORM),  // INDEX_TEXTURE_SAMPLER
            UniformShaderParameter(ALPHA_UNIFORM))

    private val mDrawShapeFilterMapProgramId: MutableMap<DrawShapeFilter, Int> = HashMap()

    private val mTextureFilterMapProgramId: MutableMap<TextureFilter, Int> = HashMap()

    private val mOESTextureFilterMapProgramId: MutableMap<TextureFilter, Int> = HashMap()

    // Keep track of restore state
    private var mMatrices = FloatArray(INITIAL_RESTORE_STATE_SIZE * MATRIX_SIZE)

    private var mAlphas = FloatArray(INITIAL_RESTORE_STATE_SIZE)

    private val mSaveFlags = IntArrayCustom()

    private var mCurrentAlphaIndex = 0

    private var mCurrentMatrixIndex = 0

    // Viewport size
    private var mWidth = 0

    private var mHeight = 0

    // Projection matrix
    private val mProjectionMatrix = FloatArray(MATRIX_SIZE)

    // Screen size for when we aren't bound to a secondBitmap
    private var mScreenWidth = 0

    private var mScreenHeight = 0

    // GL programs
    private var mDrawProgram: Int

    private var mTextureProgram = 0

    private var mOesTextureProgram = 0

    private var mMeshProgram = 0

    // GL buffer containing BOX_COORDINATES
    private val mBoxCoordinates: Int

    private var mTextureFilter: TextureFilter? = null

    private var mDrawShapeFilter: DrawShapeFilter? = null

    private var onPreDrawTextureListener: OnPreDrawTextureListener? = null

    private var onPreDrawShapeListener: OnPreDrawShapeListener? = null

    // Keep track of statistics for debugging
    private var mCountDrawMesh = 0

    private var countTextureRect = 0

    private var mCountFillRect = 0

    private var mCountDrawLine = 0

    // Buffer for framebuffer IDs -- we keep track so we can switch the attached
    // secondBitmap.
    private val mFrameBuffer = IntArray(1)

    // Bound textures.
    private val mTargetTextures = ArrayList<RawTexture?>()

    override val gLId: GLId? = GLES20IdImpl()

    init {
        Matrix.setIdentityM(mTempTextureMatrix, 0)
        Matrix.setIdentityM(mMatrices, mCurrentMatrixIndex)
        mAlphas[mCurrentAlphaIndex] = 1f
        mTargetTextures.add(null)
        val boxBuffer = createBuffer(BOX_COORDINATES)
        mBoxCoordinates = uploadBuffer(boxBuffer)
        mDrawProgram = assembleProgram(loadShader(GLES20.GL_VERTEX_SHADER, BasicDrawShapeFilter.DRAW_VERTEX_SHADER), loadShader(GLES20.GL_FRAGMENT_SHADER, BasicDrawShapeFilter.DRAW_FRAGMENT_SHADER), mDrawParameters, mTempIntArray)
        val textureFragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, BasicTextureFilter.TEXTURE_FRAGMENT_SHADER)
        val meshVertexShader = loadShader(GLES20.GL_VERTEX_SHADER, MESH_VERTEX_SHADER)
        setupMeshProgram(meshVertexShader, textureFragmentShader)
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        checkError()
    }

    private fun setupMeshProgram(meshVertexShader: Int, textureFragmentShader: Int) {
        mMeshProgram = assembleProgram(meshVertexShader, textureFragmentShader, mMeshParameters, mTempIntArray)
    }

    override fun setSize(width: Int, height: Int) {
        mWidth = width
        mHeight = height
        checkError()
        Matrix.setIdentityM(mMatrices, mCurrentMatrixIndex)
        Matrix.orthoM(mProjectionMatrix, 0, 0f, width.toFloat(), 0f, height.toFloat(), -1f, 1f)
        if (targetTexture == null) {
            mScreenWidth = width
            mScreenHeight = height
            Matrix.translateM(mMatrices, mCurrentMatrixIndex, 0f, height.toFloat(), 0f)
            Matrix.scaleM(mMatrices, mCurrentMatrixIndex, 1f, -1f, 1f)
        }
    }

    override fun clearBuffer() {
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        checkError()
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        checkError()
    }

    override fun clearBuffer(argb: FloatArray?) {
        GLES20.glClearColor(argb!![1], argb[2], argb[3], argb[0])
        checkError()
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        checkError()
    }

    override var alpha: Float
        get() = mAlphas[mCurrentAlphaIndex]
        set(alpha) {
            mAlphas[mCurrentAlphaIndex] = alpha
        }

    override fun multiplyAlpha(alpha: Float) {
        this.alpha = alpha * alpha
    }

    override fun translate(x: Float, y: Float, z: Float) {
        Matrix.translateM(mMatrices, mCurrentMatrixIndex, x, y, z)
    }

    // This is a faster version of translate(x, y, z) because
    // (1) we knows z = 0, (2) we inline the Matrix.translateM call,
    // (3) we unroll the loop
    override fun translate(x: Float, y: Float) {
        val index = mCurrentMatrixIndex
        val m = mMatrices
        m[index + 12] += m[index] * x + m[index + 4] * y
        m[index + 13] += m[index + 1] * x + m[index + 5] * y
        m[index + 14] += m[index + 2] * x + m[index + 6] * y
        m[index + 15] += m[index + 3] * x + m[index + 7] * y
    }

    override fun scale(sx: Float, sy: Float, sz: Float) {
        Matrix.scaleM(mMatrices, mCurrentMatrixIndex, sx, sy, sz)
    }

    override fun rotate(angle: Float, x: Float, y: Float, z: Float) {
        if (angle == 0.0f) {
            return
        }

        val temp = mTempMatrix
        Matrix.setRotateM(temp, 0, angle, x, y, z)
        val matrix = mMatrices
        val index = mCurrentMatrixIndex
        Matrix.multiplyMM(temp, MATRIX_SIZE, matrix, index, temp, 0)
        System.arraycopy(temp, MATRIX_SIZE, matrix, index, MATRIX_SIZE)
    }

    override fun multiplyMatrix(matrix: FloatArray?, offset: Int) {
        val temp = mTempMatrix
        val currentMatrix = mMatrices
        val index = mCurrentMatrixIndex
        Matrix.multiplyMM(temp, 0, currentMatrix, index, matrix, offset)
        System.arraycopy(temp, 0, currentMatrix, index, 16)
    }

    override fun save() {
        save(GLCanvas.SAVE_FLAG_ALL)
    }

    override fun save(saveFlags: Int) {
        val saveAlpha = saveFlags and GLCanvas.SAVE_FLAG_ALPHA == GLCanvas.SAVE_FLAG_ALPHA
        if (saveAlpha) {
            val currentAlpha = alpha
            mCurrentAlphaIndex++
            if (mAlphas.size <= mCurrentAlphaIndex) {
                mAlphas = mAlphas.copyOf(mAlphas.size * 2)
            }
            mAlphas[mCurrentAlphaIndex] = currentAlpha
        }
        val saveMatrix = saveFlags and GLCanvas.SAVE_FLAG_MATRIX == GLCanvas.SAVE_FLAG_MATRIX
        if (saveMatrix) {
            val currentIndex = mCurrentMatrixIndex
            mCurrentMatrixIndex += MATRIX_SIZE
            if (mMatrices.size <= mCurrentMatrixIndex) {
                mMatrices = mMatrices.copyOf(mMatrices.size * 2)
            }
            System.arraycopy(mMatrices, currentIndex, mMatrices, mCurrentMatrixIndex, MATRIX_SIZE)
        }
        mSaveFlags.add(saveFlags)
    }

    override fun restore() {
        val restoreFlags = mSaveFlags.removeLast()
        val restoreAlpha = restoreFlags and GLCanvas.SAVE_FLAG_ALPHA == GLCanvas.SAVE_FLAG_ALPHA
        if (restoreAlpha) {
            mCurrentAlphaIndex--
        }
        val restoreMatrix = restoreFlags and GLCanvas.SAVE_FLAG_MATRIX == GLCanvas.SAVE_FLAG_MATRIX
        if (restoreMatrix) {
            mCurrentMatrixIndex -= MATRIX_SIZE
        }
    }

    override fun drawCircle(x: Float, y: Float, radius: Float, paint: GLPaint?, drawShapeFilter: DrawShapeFilter?) {
        setupDrawShapeFilter(drawShapeFilter)
        draw(GLES20.GL_TRIANGLE_STRIP, OFFSET_FILL_RECT, COUNT_FILL_VERTEX, x, y, 2 * radius, 2 * radius, paint!!.color, 0f)
    }

    override fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float, paint: GLPaint?, drawShapeFilter: DrawShapeFilter?) {
        setupDrawShapeFilter(drawShapeFilter)
        draw(GLES20.GL_LINE_STRIP, OFFSET_DRAW_LINE, COUNT_LINE_VERTEX, x1, y1, x2 - x1, y2 - y1,
                paint)
        mCountDrawLine++
    }

    override fun drawRect(x: Float, y: Float, width: Float, height: Float, paint: GLPaint?, drawShapeFilter: DrawShapeFilter?) {
        setupDrawShapeFilter(drawShapeFilter)
        draw(GLES20.GL_LINE_LOOP, OFFSET_DRAW_RECT, COUNT_RECT_VERTEX, x, y, width, height, paint)
        mCountDrawLine++
    }

    private fun draw(type: Int, offset: Int, count: Int, x: Float, y: Float, width: Float, height: Float,
                     paint: GLPaint?) {
        draw(type, offset, count, x, y, width, height, paint!!.color, paint.lineWidth)
    }

    private fun draw(type: Int, offset: Int, count: Int, x: Float, y: Float, width: Float, height: Float,
                     color: Int, lineWidth: Float) {
        prepareDraw(offset, color, lineWidth)
        if (onPreDrawShapeListener != null) {
            onPreDrawShapeListener!!.onPreDraw(mDrawProgram, mDrawShapeFilter)
        }
        draw(mDrawParameters, type, count, x, y, width, height, null)
    }

    private fun prepareDraw(offset: Int, color: Int, lineWidth: Float) {
        GLES20.glUseProgram(mDrawProgram)
        checkError()
        if (lineWidth > 0) {
            GLES20.glLineWidth(lineWidth)
            checkError()
        }
        val colorArray = getColor(color)
        enableBlending(true)
        GLES20.glBlendColor(colorArray[0], colorArray[1], colorArray[2], colorArray[3])
        checkError()
        GLES20.glUniform4fv(mDrawParameters[INDEX_COLOR].handle, 1, colorArray, 0)
        setPosition(mDrawParameters, offset)
        checkError()
    }

    private fun getColor(color: Int): FloatArray {
        val alpha = (color ushr 24 and 0xFF) / 255f * alpha
        val red = (color ushr 16 and 0xFF) / 255f * alpha
        val green = (color ushr 8 and 0xFF) / 255f * alpha
        val blue = (color and 0xFF) / 255f * alpha
        mTempColor[0] = red
        mTempColor[1] = green
        mTempColor[2] = blue
        mTempColor[3] = alpha
        return mTempColor
    }

    private fun setPosition(params: Array<ShaderParameter>, offset: Int) {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mBoxCoordinates)
        checkError()
        GLES20.glVertexAttribPointer(params[INDEX_POSITION].handle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false, VERTEX_STRIDE, offset * VERTEX_STRIDE)
        checkError()
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        checkError()
    }

    private fun draw(params: Array<ShaderParameter>, type: Int, count: Int, x: Float, y: Float, width: Float,
                     height: Float, customMVPMatrix: ICustomMVPMatrix?) {
        setMatrix(params, x, y, width, height, customMVPMatrix)
        val positionHandle = params[INDEX_POSITION].handle
        GLES20.glEnableVertexAttribArray(positionHandle)
        checkError()
        GLES20.glDrawArrays(type, 0, count)
        checkError()
        GLES20.glDisableVertexAttribArray(positionHandle)
        checkError()
    }

    private fun setMatrix(params: Array<ShaderParameter>, x: Float, y: Float, width: Float, height: Float, customMVPMatrix: ICustomMVPMatrix?) {
        if (customMVPMatrix != null) {
            GLES20.glUniformMatrix4fv(params[INDEX_MATRIX].handle, 1, false, customMVPMatrix.getMVPMatrix(mScreenWidth, mScreenHeight, x, y, width, height), 0)
            checkError()
            return
        }
        GLES20.glViewport(0, 0, mScreenWidth, mScreenHeight)
        Matrix.translateM(mTempMatrix, 0, mMatrices, mCurrentMatrixIndex, x, y, 0f)
        Matrix.scaleM(mTempMatrix, 0, width, height, 1f)
        Matrix.multiplyMM(mTempMatrix, MATRIX_SIZE, mProjectionMatrix, 0, mTempMatrix, 0)
        printMatrix("translate matrix:", mTempMatrix, MATRIX_SIZE)
        GLES20.glUniformMatrix4fv(params[INDEX_MATRIX].handle, 1, false, mTempMatrix, MATRIX_SIZE)
        checkError()
    }

    override fun fillRect(x: Float, y: Float, width: Float, height: Float, color: Int, drawShapeFilter: DrawShapeFilter?) {
        setupDrawShapeFilter(drawShapeFilter)
        draw(GLES20.GL_TRIANGLE_STRIP, OFFSET_FILL_RECT, COUNT_FILL_VERTEX, x, y, width, height,
                color, 0f)
        mCountFillRect++
    }

    override fun drawTexture(texture: BasicTexture?, x: Int, y: Int, width: Int, height: Int, textureFilter: TextureFilter?, customMVPMatrix: ICustomMVPMatrix?) {
        if (width <= 0 || height <= 0) {
            return
        }
        setupTextureFilter(texture!!.target, textureFilter)
        copyTextureCoordinates(texture, mTempSourceRect)
        mTempTargetRect[x.toFloat(), y.toFloat(), x + width.toFloat()] = y + height.toFloat()
        convertCoordinate(mTempSourceRect, texture)
        changeTargetIfNeeded(mTempSourceRect, mTempTargetRect, texture)
        drawTextureRect(texture, mTempSourceRect, mTempTargetRect, customMVPMatrix)
    }

    override fun drawTexture(texture: BasicTexture?, source: RectF?, target: RectF?, textureFilter: TextureFilter?, customMVPMatrix: ICustomMVPMatrix?) {
        if (target!!.width() <= 0 || target.height() <= 0) {
            return
        }
        setupTextureFilter(texture!!.target, textureFilter)
        mTempSourceRect.set(source!!)
        mTempTargetRect.set(target)
        convertCoordinate(mTempSourceRect, texture)
        changeTargetIfNeeded(mTempSourceRect, mTempTargetRect, texture)
        drawTextureRect(texture, mTempSourceRect, mTempTargetRect, customMVPMatrix)
    }

    override fun drawTexture(texture: BasicTexture?, textureTransform: FloatArray?, x: Int, y: Int, w: Int,
                             h: Int, textureFilter: TextureFilter?, customMVPMatrix: ICustomMVPMatrix?) {
        if (w <= 0 || h <= 0) {
            return
        }
        setupTextureFilter(texture!!.target, textureFilter)
        mTempTargetRect[x.toFloat(), y.toFloat(), x + w.toFloat()] = y + h.toFloat()
        drawTextureRect(texture, textureTransform, mTempTargetRect, customMVPMatrix)
    }

    private fun drawTextureRect(texture: BasicTexture?, source: RectF, target: RectF, customMVPMatrix: ICustomMVPMatrix?) {
        setTextureMatrix(source, mTempTextureMatrix)
        drawTextureRect(texture, mTempTextureMatrix, target, customMVPMatrix)
    }

    private fun drawTextureRect(texture: BasicTexture?, textureMatrix: FloatArray?, target: RectF, customMVPMatrix: ICustomMVPMatrix?) {
        val params = prepareTexture(texture)
        setPosition(params, OFFSET_FILL_RECT)
        //        printMatrix("texture matrix", textureMatrix, 0);
        GLES20.glUniformMatrix4fv(params[INDEX_TEXTURE_MATRIX].handle, 1, false, textureMatrix, 0)
        if (onPreDrawTextureListener != null) {
            onPreDrawTextureListener!!.onPreDraw(if (texture!!.target == GLES20.GL_TEXTURE_2D) mTextureProgram else mOesTextureProgram, texture, mTextureFilter)
        }
        checkError()
        if (texture!!.isFlippedVertically) {
            save(GLCanvas.SAVE_FLAG_MATRIX)
            translate(0f, target.centerY())
            scale(1f, -1f, 1f)
            translate(0f, -target.centerY())
        }
        draw(params, GLES20.GL_TRIANGLE_STRIP, COUNT_FILL_VERTEX, target.left, target.top,
                target.width(), target.height(), customMVPMatrix)
        if (texture.isFlippedVertically) {
            restore()
        }
        countTextureRect++
    }

    private fun prepareTexture(texture: BasicTexture?): Array<ShaderParameter> {
        val params: Array<ShaderParameter>
        val program: Int
        if (texture!!.target == GLES20.GL_TEXTURE_2D) {
            params = mTextureParameters
            program = mTextureProgram
        } else {
            params = mOesTextureParameters
            program = mOesTextureProgram
        }
        prepareTexture(texture, program, params)
        return params
    }

    private fun prepareTexture(texture: BasicTexture?, program: Int, params: Array<ShaderParameter>) {
        GLES20.glUseProgram(program)
        checkError()
        enableBlending(!texture!!.isOpaque || alpha < OPAQUE_ALPHA)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        checkError()
        texture.onBind(this)
        GLES20.glBindTexture(texture.target, texture.id)
        checkError()
        GLES20.glUniform1i(params[INDEX_TEXTURE_SAMPLER].handle, 0)
        checkError()
        GLES20.glUniform1f(params[INDEX_ALPHA].handle, alpha)
        checkError()
    }

    override fun drawMesh(texture: BasicTexture?, x: Int, y: Int, xyBuffer: Int, uvBuffer: Int,
                          indexBuffer: Int, indexCount: Int, mode: Int) {
        prepareTexture(texture, mMeshProgram, mMeshParameters)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexBuffer)
        checkError()
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, xyBuffer)
        checkError()
        val positionHandle = mMeshParameters[INDEX_POSITION].handle
        GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false,
                VERTEX_STRIDE, 0)
        checkError()
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, uvBuffer)
        checkError()
        val texCoordHandle = mMeshParameters[INDEX_TEXTURE_COORD].handle
        GLES20.glVertexAttribPointer(texCoordHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT,
                false, VERTEX_STRIDE, 0)
        checkError()
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        checkError()
        GLES20.glEnableVertexAttribArray(positionHandle)
        checkError()
        GLES20.glEnableVertexAttribArray(texCoordHandle)
        checkError()
        setMatrix(mMeshParameters, x.toFloat(), y.toFloat(), 1f, 1f, null)
        GLES20.glDrawElements(mode, indexCount, GLES20.GL_UNSIGNED_BYTE, 0)
        checkError()
        GLES20.glDisableVertexAttribArray(positionHandle)
        checkError()
        GLES20.glDisableVertexAttribArray(texCoordHandle)
        checkError()
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
        checkError()
        mCountDrawMesh++
    }

    override fun drawMixed(texture: BasicTexture?, toColor: Int, ratio: Float, x: Int, y: Int, w: Int, h: Int, drawShapeFilter: DrawShapeFilter?) {
        copyTextureCoordinates(texture!!, mTempSourceRect)
        mTempTargetRect[x.toFloat(), y.toFloat(), x + w.toFloat()] = y + h.toFloat()
        drawMixed(texture, toColor, ratio, mTempSourceRect, mTempTargetRect, drawShapeFilter)
    }

    override fun drawMixed(texture: BasicTexture?, toColor: Int, ratio: Float, source: RectF?, target: RectF?, drawShapeFilter: DrawShapeFilter?) {
        if (target!!.width() <= 0 || target.height() <= 0) {
            return
        }
        save(GLCanvas.SAVE_FLAG_ALPHA)
        val currentAlpha = alpha
        val cappedRatio = min(1f, max(0f, ratio))
        val textureAlpha = (1f - cappedRatio) * currentAlpha
        alpha = textureAlpha
        drawTexture(texture, source, target, BasicTextureFilter(), null)
        val colorAlpha = cappedRatio * currentAlpha
        alpha = colorAlpha
        fillRect(target.left, target.top, target.width(), target.height(), toColor, drawShapeFilter)
        restore()
    }

    override fun unloadTexture(texture: BasicTexture?): Boolean {
        val unload = texture!!.isLoaded
        if (unload) {
            synchronized(mUnboundTextures) { mUnboundTextures.add(texture.id) }
        }
        return unload
    }

    override fun deleteBuffer(bufferId: Int) {
        synchronized(mUnboundTextures) { mDeleteBuffers.add(bufferId) }
    }

    override fun deleteRecycledResources() {
        synchronized(mUnboundTextures) {
            var ids = mUnboundTextures
            if (mUnboundTextures.size() > 0) {
                gLId!!.glDeleteTextures(ids.size(), ids.internalArray, 0)
                ids.clear()
            }
            ids = mDeleteBuffers
            if (ids.size() > 0) {
                gLId!!.glDeleteBuffers(ids.size(), ids.internalArray, 0)
                ids.clear()
            }
        }
    }

    override fun dumpStatisticsAndClear() {
        val line = String.format("MESH:%d, TEX_RECT:%d, FILL_RECT:%d, LINE:%d", mCountDrawMesh,
                countTextureRect, mCountFillRect, mCountDrawLine)
        mCountDrawMesh = 0
        countTextureRect = 0
        mCountFillRect = 0
        mCountDrawLine = 0
        Log.d(TAG, line)
    }

    override fun endRenderTarget() {
        val oldTexture = mTargetTextures.removeAt(mTargetTextures.size - 1)
        val texture = targetTexture
        setRenderTarget(oldTexture, texture)
        restore() // restore matrix and alpha
    }

    override fun beginRenderTarget(texture: RawTexture?) {
        save() // save matrix and alpha and blending
        val oldTexture = targetTexture
        mTargetTextures.add(texture)
        setRenderTarget(oldTexture, texture)
    }

    private val targetTexture: RawTexture?
        get() = mTargetTextures[mTargetTextures.size - 1]

    private fun setRenderTarget(oldTexture: BasicTexture?, texture: RawTexture?) {
        if (oldTexture == null && texture != null) {
            if (texture.target == GLES20.GL_TEXTURE_2D) {
                GLES20.glGenFramebuffers(1, mFrameBuffer, 0)
                checkError()
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffer[0])
                checkError()
            } else {
                GLES11Ext.glGenFramebuffersOES(1, mFrameBuffer, 0)
                checkError()
                GLES11Ext.glBindFramebufferOES(GLES11Ext.GL_FRAMEBUFFER_OES, mFrameBuffer[0])
                checkError()
            }
        } else if (oldTexture != null && texture == null) {
            if (oldTexture.target == GLES20.GL_TEXTURE_2D) {
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
                checkError()
                GLES20.glDeleteFramebuffers(1, mFrameBuffer, 0)
                checkError()
            } else {
                GLES11Ext.glBindFramebufferOES(GLES11Ext.GL_FRAMEBUFFER_OES, 0)
                checkError()
                GLES11Ext.glDeleteFramebuffersOES(1, mFrameBuffer, 0)
                checkError()
            }
        }
        if (texture == null) {
            setSize(mScreenWidth, mScreenHeight)
        } else {
            setSize(texture.width, texture.height)
            if (!texture.isLoaded) {
                texture.prepare(this)
            }
            if (texture.target == GLES20.GL_TEXTURE_2D) {
                GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                        texture.target, texture.id, 0)
                checkError()
                checkFramebufferStatus()
            } else {
                GLES11Ext.glFramebufferTexture2DOES(GLES11Ext.GL_FRAMEBUFFER_OES, GLES11Ext.GL_COLOR_ATTACHMENT0_OES,
                        texture.target, texture.id, 0)
                checkError()
                checkFramebufferStatusOes()
            }
        }
    }

    override fun setTextureParameters(texture: BasicTexture?) {
        val target = texture!!.target
        GLES20.glBindTexture(target, texture.id)
        checkError()
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameterf(target, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameterf(target, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
    }

    override fun initializeTextureSize(texture: BasicTexture?, format: Int, type: Int) {
        val target = texture!!.target
        GLES20.glBindTexture(target, texture.id)
        checkError()
        val width = texture.textureWidth
        val height = texture.textureHeight
        GLES20.glTexImage2D(target, 0, format, width, height, 0, format, type, null)
    }

    override fun initializeTexture(texture: BasicTexture?, bitmap: Bitmap?) {
        val target = texture!!.target
        GLES20.glBindTexture(target, texture.id)
        checkError()
        GLUtils.texImage2D(target, 0, bitmap, 0)
    }

    override fun texSubImage2D(texture: BasicTexture?, xOffset: Int, yOffset: Int, bitmap: Bitmap?,
                               format: Int, type: Int) {
        val target = texture!!.target
        GLES20.glBindTexture(target, texture.id)
        checkError()
        GLUtils.texSubImage2D(target, 0, xOffset, yOffset, bitmap, format, type)
    }

    override fun uploadBuffer(buffer: FloatBuffer?): Int {
        return uploadBuffer(buffer, FLOAT_SIZE)
    }

    override fun uploadBuffer(buffer: ByteBuffer?): Int {
        return uploadBuffer(buffer, 1)
    }

    private fun uploadBuffer(buffer: Buffer?, elementSize: Int): Int {
        gLId!!.glGenBuffers(1, mTempIntArray, 0)
        checkError()
        val bufferId = mTempIntArray[0]
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferId)
        checkError()
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, buffer!!.capacity() * elementSize, buffer,
                GLES20.GL_STATIC_DRAW)
        checkError()
        return bufferId
    }

    override fun recoverFromLightCycle() {
//        GLES20.glViewport(0, 0, mWidth, mHeight);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        checkError()
    }

    override fun getBounds(bounds: Rect?, x: Int, y: Int, width: Int, height: Int) {
        Matrix.translateM(mTempMatrix, 0, mMatrices, mCurrentMatrixIndex, x.toFloat(), y.toFloat(), 0f)
        Matrix.scaleM(mTempMatrix, 0, width.toFloat(), height.toFloat(), 1f)
        Matrix.multiplyMV(mTempMatrix, MATRIX_SIZE, mTempMatrix, 0, BOUNDS_COORDINATES, 0)
        Matrix.multiplyMV(mTempMatrix, MATRIX_SIZE + 4, mTempMatrix, 0, BOUNDS_COORDINATES, 4)
        bounds!!.left = mTempMatrix[MATRIX_SIZE].roundToInt()
        bounds.right = mTempMatrix[MATRIX_SIZE + 4].roundToInt()
        bounds.top = mTempMatrix[MATRIX_SIZE + 1].roundToInt()
        bounds.bottom = mTempMatrix[MATRIX_SIZE + 5].roundToInt()
        bounds.sort()
    }

    private fun setupDrawShapeFilter(drawShapeFilter: DrawShapeFilter?) {
        if (drawShapeFilter == null) {
            throw NullPointerException("draw shape filter is null.")
        }
        mDrawShapeFilter = drawShapeFilter
        if (mDrawShapeFilterMapProgramId.containsKey(drawShapeFilter)) {
            mDrawProgram = mDrawShapeFilterMapProgramId[drawShapeFilter]!!
            loadHandles(mDrawParameters, mDrawProgram)
            return
        }
        mDrawProgram = loadAndAssemble(mDrawParameters, drawShapeFilter.vertexShader, drawShapeFilter.fragmentShader)
        mDrawShapeFilterMapProgramId[drawShapeFilter] = mDrawProgram
    }

    private fun setupTextureFilter(target: Int, textureFilter: TextureFilter?) {
        if (textureFilter == null) {
            throw NullPointerException("Texture filter is null.")
        }

        mTextureFilter = textureFilter
        if (target == GLES20.GL_TEXTURE_2D) {
            if (mTextureFilterMapProgramId.containsKey(textureFilter)) {
                mTextureProgram = mTextureFilterMapProgramId[textureFilter]!!
                loadHandles(mTextureParameters, mTextureProgram)
                return
            }
            mTextureProgram = loadAndAssemble(mTextureParameters, textureFilter.vertexShader, textureFilter.fragmentShader)
            mTextureFilterMapProgramId[textureFilter] = mTextureProgram
        } else {
            if (mOESTextureFilterMapProgramId.containsKey(textureFilter)) {
                mOesTextureProgram = mOESTextureFilterMapProgramId[textureFilter]!!
                loadHandles(mOesTextureParameters, mOesTextureProgram)
                return
            }
            mOesTextureProgram = loadAndAssemble(mOesTextureParameters, textureFilter.vertexShader, textureFilter.oesFragmentProgram)
            mOESTextureFilterMapProgramId[textureFilter] = mOesTextureProgram
        }
    }

    private fun loadAndAssemble(shaderParameters: Array<ShaderParameter>, vertexProgram: String?, fragmentProgram: String?): Int {
        val vertexShaderHandle = loadShader(GLES20.GL_VERTEX_SHADER, vertexProgram)
        val fragmentShaderHandle = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentProgram)
        return assembleProgram(vertexShaderHandle, fragmentShaderHandle, shaderParameters, mTempIntArray)
    }

    override fun setOnPreDrawTextureListener(l: OnPreDrawTextureListener?) {
        onPreDrawTextureListener = l
    }

    override fun setOnPreDrawShapeListener(l: OnPreDrawShapeListener?) {
        onPreDrawShapeListener = l
    }

    abstract class ShaderParameter(protected val mName: String) {

        var handle = 0

        abstract fun loadHandle(program: Int)
    }

    private class UniformShaderParameter(name: String) : ShaderParameter(name) {
        override fun loadHandle(program: Int) {
            handle = GLES20.glGetUniformLocation(program, mName)
            checkError()
        }
    }

    private class AttributeShaderParameter(name: String) : ShaderParameter(name) {
        override fun loadHandle(program: Int) {
            handle = GLES20.glGetAttribLocation(program, mName)
            checkError()
        }
    }

    companion object {
        const val POSITION_ATTRIBUTE = "aPosition"
        const val COLOR_UNIFORM = "uColor"
        const val MATRIX_UNIFORM = "uMatrix"
        const val TEXTURE_MATRIX_UNIFORM = "uTextureMatrix"
        const val TEXTURE_SAMPLER_UNIFORM = "uTextureSampler"
        const val ALPHA_UNIFORM = "uAlpha"
        const val TEXTURE_COORD_ATTRIBUTE = "aTextureCoordinate"
        const val MESH_VERTEX_SHADER = (""
                + "uniform mat4 " + MATRIX_UNIFORM + ";\n"
                + "attribute vec2 " + POSITION_ATTRIBUTE + ";\n"
                + "attribute vec2 " + TEXTURE_COORD_ATTRIBUTE + ";\n"
                + "varying vec2 vTextureCoord;\n"
                + "void main() {\n"
                + "  vec4 pos = vec4(" + POSITION_ATTRIBUTE + ", 0.0, 1.0);\n"
                + "  gl_Position = " + MATRIX_UNIFORM + " * pos;\n"
                + "  vTextureCoord = " + TEXTURE_COORD_ATTRIBUTE + ";\n"
                + "}\n")

        const val INITIAL_RESTORE_STATE_SIZE = 8

        // ************** Constants **********************
        private val TAG = GLES20Canvas::class.java.simpleName

        private const val FLOAT_SIZE = java.lang.Float.SIZE / java.lang.Byte.SIZE

        private const val OPAQUE_ALPHA = 0.95f

        private const val COORDS_PER_VERTEX = 2

        private const val VERTEX_STRIDE = COORDS_PER_VERTEX * FLOAT_SIZE

        private const val COUNT_FILL_VERTEX = 4

        private const val COUNT_LINE_VERTEX = 2

        private const val COUNT_RECT_VERTEX = 4

        private const val OFFSET_FILL_RECT = 0

        private const val OFFSET_DRAW_LINE = OFFSET_FILL_RECT + COUNT_FILL_VERTEX

        private const val OFFSET_DRAW_RECT = OFFSET_DRAW_LINE + COUNT_LINE_VERTEX

        private val BOX_COORDINATES = floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f, 1f, 1f, 0f, 0f, 1f, 1f, 0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f)

        private val BOUNDS_COORDINATES = floatArrayOf(0f, 0f, 0f, 1f, 1f, 1f, 0f, 1f)

        private const val MATRIX_SIZE = 16

        // Handle indices -- common
        private const val INDEX_POSITION = 0

        private const val INDEX_MATRIX = 1

        // Handle indices -- draw
        private const val INDEX_COLOR = 2

        // Handle indices -- secondBitmap
        private const val INDEX_TEXTURE_MATRIX = 2

        private const val INDEX_TEXTURE_SAMPLER = 3

        private const val INDEX_ALPHA = 4

        // Handle indices -- mesh
        private const val INDEX_TEXTURE_COORD = 2

        val gLId: GLId = GLES20IdImpl()

        private fun createBuffer(values: FloatArray): FloatBuffer {
            // First create an nio buffer, then create a VBO from it.
            val size = values.size * FLOAT_SIZE
            val buffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder())
                    .asFloatBuffer()
            buffer.put(values, 0, values.size).position(0)

            return buffer
        }

        private fun assembleProgram(vertexShader: Int, fragmentShader: Int, params: Array<ShaderParameter>, linkStatus: IntArray): Int {
            var program = GLES20.glCreateProgram()
            checkError()
            if (program == 0) {
                throw RuntimeException("Cannot create GL program: " + GLES20.glGetError())
            }
            GLES20.glAttachShader(program, vertexShader)
            checkError()
            GLES20.glAttachShader(program, fragmentShader)
            checkError()
            GLES20.glLinkProgram(program)
            checkError()
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] != GLES20.GL_TRUE) {
                Log.e(TAG, "Could not link program: ")
                Log.e(TAG, GLES20.glGetProgramInfoLog(program))
                GLES20.glDeleteProgram(program)
                program = 0
            }
            loadHandles(params, program)

            return program
        }

        private fun loadHandles(params: Array<ShaderParameter>, program: Int) {
            for (param in params) {
                param.loadHandle(program)
            }
        }

        private fun loadShader(type: Int, shaderCode: String?): Int {
            // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
            // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
            val shader = GLES20.glCreateShader(type)

            // add the source code to the shader and compile it
            GLES20.glShaderSource(shader, shaderCode)
            checkError()
            GLES20.glCompileShader(shader)
            checkError()

            return shader
        }

        private fun enableBlending(enableBlending: Boolean) {
            if (enableBlending) {
                GLES20.glEnable(GLES20.GL_BLEND)
                checkError()
            } else {
                GLES20.glDisable(GLES20.GL_BLEND)
                checkError()
            }
        }

        private fun changeTargetIfNeeded(source: RectF, target: RectF, texture: BasicTexture?) {
            val yBound = texture!!.height.toFloat() / texture.textureHeight
            val xBound = texture.width.toFloat() / texture.textureWidth
            if (source.right > xBound) {
                target.right = target.left + target.width() * (xBound - source.left) / source.width()
            }
            if (source.bottom > yBound) {
                target.bottom = target.top + target.height() * (yBound - source.top) / source.height()
            }
        }

        private fun checkFramebufferStatus() {
            val status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
            if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
                val msg = when (status) {
                    GLES20.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT -> "GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT"
                    GLES20.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS -> "GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS"
                    GLES20.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT -> "GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT"
                    GLES20.GL_FRAMEBUFFER_UNSUPPORTED -> "GL_FRAMEBUFFER_UNSUPPORTED"
                    else -> ""
                }

                throw RuntimeException(msg + ":" + Integer.toHexString(status))
            }
        }

        private fun checkFramebufferStatusOes() {
            val status = GLES11Ext.glCheckFramebufferStatusOES(GLES11Ext.GL_FRAMEBUFFER_OES)
            if (status != GLES11Ext.GL_FRAMEBUFFER_COMPLETE_OES) {
                val msg = when (status) {
                    GLES11Ext.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT_OES -> "GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT"
                    GLES11Ext.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS_OES -> "GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS"
                    GLES11Ext.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT_OES -> "GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT"
                    GLES11Ext.GL_FRAMEBUFFER_UNSUPPORTED_OES -> "GL_FRAMEBUFFER_UNSUPPORTED"
                    else -> ""
                }

                throw RuntimeException(msg + ":" + Integer.toHexString(status))
            }
        }

        fun checkError() {
            val error = GLES20.glGetError()
            if (error != 0) {
                val t = Throwable()
                Log.e(TAG, "GL error: $error", t)
            }
        }

        fun printMatrix(message: String?, m: FloatArray, offset: Int) {
            if (!Loggers.DEBUG) {
                return
            }
            val b = StringBuilder(message!!)
            b.append('\n')
            val size = 4
            for (i in 0 until size) {
                val format = "%.6f"
                b.append(String.format(format, m[offset + i]))
                b.append(", ")
                b.append(String.format(format, m[offset + 4 + i]))
                b.append(", ")
                b.append(String.format(format, m[offset + 8 + i]))
                b.append(", ")
                b.append(String.format(format, m[offset + 12 + i]))
                if (i < size - 1) {
                    b.append(", ")
                }
                b.append('\n')
            }
            Loggers.v(TAG, b.toString())
        }
    }
}