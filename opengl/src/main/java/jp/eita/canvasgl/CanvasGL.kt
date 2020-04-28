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
package jp.eita.canvasgl

import android.graphics.*
import android.opengl.GLES20
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import jp.eita.canvasgl.glcanvas.*
import jp.eita.canvasgl.glcanvas.GLCanvas.ICustomMVPMatrix
import jp.eita.canvasgl.matrix.IBitmapMatrix
import jp.eita.canvasgl.shapeFilter.BasicDrawShapeFilter
import jp.eita.canvasgl.shapeFilter.DrawCircleFilter
import jp.eita.canvasgl.shapeFilter.DrawShapeFilter
import jp.eita.canvasgl.textureFilter.BasicTextureFilter
import jp.eita.canvasgl.textureFilter.FilterGroup
import jp.eita.canvasgl.textureFilter.TextureFilter
import java.util.*
import kotlin.math.roundToInt

/**
 * All the depth of textures are the same. So the texture drawn after will cover the texture drawn before.
 */
class CanvasGL constructor(override val glCanvas: GLCanvas = GLES20Canvas()) : ICanvasGL {

    private val bitmapTextureMap: MutableMap<Bitmap, BasicTexture> = WeakHashMap()

    private val defaultTextureFilter: BasicTextureFilter

    private val canvasBackgroundColor: FloatArray

    private val surfaceTextureMatrix = FloatArray(16)

    override var width = 0
        private set

    override var height = 0
        private set

    private val defaultDrawShapeFilter: BasicDrawShapeFilter

    private val drawCircleFilter = DrawCircleFilter()

    private var currentTextureFilter: TextureFilter? = null

    init {
        glCanvas.setOnPreDrawShapeListener(object : GLCanvas.OnPreDrawShapeListener {
            override fun onPreDraw(program: Int, drawShapeFilter: DrawShapeFilter?) {
                drawShapeFilter?.onPreDraw(program, this@CanvasGL)
            }

        })
        glCanvas.setOnPreDrawTextureListener(object : GLCanvas.OnPreDrawTextureListener {
            override fun onPreDraw(textureProgram: Int, texture: BasicTexture?, textureFilter: TextureFilter?) {
                textureFilter!!.onPreDraw(textureProgram, texture!!, this@CanvasGL)
            }

        })
        defaultTextureFilter = BasicTextureFilter()
        defaultDrawShapeFilter = BasicDrawShapeFilter()
        canvasBackgroundColor = FloatArray(4)
    }

    override fun bindBitmapToTexture(whichTexture: Int, bitmap: Bitmap): BitmapTexture? {
        GLES20.glActiveTexture(whichTexture)
        GLES20Canvas.checkError()
        val texture = getTexture(bitmap, null) as BitmapTexture?
        texture!!.onBind(glCanvas)
        GLES20.glBindTexture(texture.target, texture.id)
        GLES20Canvas.checkError()

        return texture
    }

    override fun bindRawTexture(whichTexture: Int, texture: RawTexture) {
        GLES20.glActiveTexture(whichTexture)
        GLES20Canvas.checkError()
        if (!texture.isLoaded) {
            texture.prepare(glCanvas)
        }
        GLES20.glBindTexture(texture.target, texture.id)
        GLES20Canvas.checkError()
    }

    override fun beginRenderTarget(texture: RawTexture?) {
        glCanvas.beginRenderTarget(texture)
    }

    override fun endRenderTarget() {
        glCanvas.endRenderTarget()
    }

    override fun drawSurfaceTexture(texture: BasicTexture?, surfaceTexture: SurfaceTexture?, left: Int, top: Int, right: Int, bottom: Int) {
        drawSurfaceTexture(texture, surfaceTexture, left, top, right, bottom, defaultTextureFilter)
    }

    override fun drawSurfaceTexture(texture: BasicTexture?, surfaceTexture: SurfaceTexture?, left: Int, top: Int, right: Int, bottom: Int, textureFilter: TextureFilter?) {
        drawSurfaceTexture(texture, surfaceTexture, left, top, right, bottom, null, textureFilter)
    }

    override fun drawSurfaceTexture(texture: BasicTexture?, surfaceTexture: SurfaceTexture?, matrix: IBitmapMatrix?) {
        drawSurfaceTexture(texture!!, surfaceTexture, matrix, defaultTextureFilter)
    }

    override fun drawSurfaceTexture(texture: BasicTexture, surfaceTexture: SurfaceTexture?, matrix: IBitmapMatrix?, textureFilter: TextureFilter) {
        drawSurfaceTexture(texture, surfaceTexture, 0, 0, texture.width, texture.height, matrix, textureFilter)
    }

    override fun drawSurfaceTexture(texture: BasicTexture?, surfaceTexture: SurfaceTexture?, left: Int, top: Int, right: Int, bottom: Int, matrix: IBitmapMatrix?, textureFilter: TextureFilter?) {
        currentTextureFilter = textureFilter
        var filteredTexture = texture
        if (textureFilter is FilterGroup) {
            filteredTexture = getFilterGroupTexture(texture, surfaceTexture, textureFilter)
        }
        val customMVPMatrix = if (matrix == null) null else object : ICustomMVPMatrix {
            override fun getMVPMatrix(viewportW: Int, viewportH: Int, x: Float, y: Float, drawW: Float, drawH: Float): FloatArray? {
                return matrix.obtainResultMatrix(viewportW, viewportH, x, y, drawW, drawH)!!
            }
        }
        if (surfaceTexture == null) {
            glCanvas.drawTexture(filteredTexture, left, top, right - left, bottom - top, textureFilter, customMVPMatrix)
        } else {
            surfaceTexture.getTransformMatrix(surfaceTextureMatrix)
            glCanvas.drawTexture(filteredTexture, surfaceTextureMatrix, left, top, right - left, bottom - top, textureFilter, customMVPMatrix)
        }
    }

    private fun getFilterGroupTexture(texture: BasicTexture?, surfaceTexture: SurfaceTexture?, basicTextureFilter: FilterGroup): BasicTexture? {
        var textureEdited = texture
        textureEdited = basicTextureFilter.draw(textureEdited!!, glCanvas, object : FilterGroup.OnDrawListener {
            override fun onDraw(drawTexture: BasicTexture?, textureFilter: TextureFilter?, isFirst: Boolean) {
                if (isFirst) {
                    surfaceTexture!!.getTransformMatrix(surfaceTextureMatrix)
                    glCanvas.drawTexture(drawTexture, surfaceTextureMatrix, 0, 0, drawTexture!!.width, drawTexture.height, textureFilter, null)
                } else {
                    glCanvas.drawTexture(drawTexture, 0, 0, drawTexture!!.width, drawTexture.height, textureFilter, null)
                }
            }

        })

        return textureEdited
    }

    override fun drawBitmap(bitmap: Bitmap, matrix: IBitmapMatrix) {
        drawBitmap(bitmap, matrix, defaultTextureFilter)
    }

    override fun drawBitmap(bitmap: Bitmap, matrix: IBitmapMatrix, textureFilter: TextureFilter) {
        val basicTexture = getTexture(bitmap, textureFilter)
        save()
        glCanvas.drawTexture(basicTexture, 0, 0, bitmap.width, bitmap.height, textureFilter, object : ICustomMVPMatrix {
            override fun getMVPMatrix(viewportW: Int, viewportH: Int, x: Float, y: Float, drawW: Float, drawH: Float): FloatArray? {
                return matrix.obtainResultMatrix(viewportW, viewportH, x, y, drawW, drawH)
            }
        })
        restore()
    }

    override fun drawBitmap(bitmap: Bitmap, src: Rect?, dst: RectF?) {
        drawBitmap(bitmap, RectF(src), dst, defaultTextureFilter)
    }

    override fun drawBitmap(bitmap: Bitmap, left: Int, top: Int) {
        drawBitmap(bitmap, left, top, defaultTextureFilter)
    }

    override fun drawBitmap(bitmap: Bitmap, left: Int, top: Int, textureFilter: TextureFilter) {
        val basicTexture = getTexture(bitmap, textureFilter)
        glCanvas.drawTexture(basicTexture, left, top, bitmap.width, bitmap.height, textureFilter, null)
    }

    override fun drawBitmap(bitmap: Bitmap, @FloatRange(from = 0.1, to = 2.0) scaleRatioBitmap: Float, left: Int, top: Int, textureFilter: TextureFilter) {
        val basicTexture = getTexture(bitmap, textureFilter)
        val newBitmapWidth: Int = (bitmap.width * scaleRatioBitmap).roundToInt()
        val newBitmapHeight: Int = (bitmap.height * scaleRatioBitmap).roundToInt()
        glCanvas.drawTexture(basicTexture, left, top, newBitmapWidth, newBitmapHeight, textureFilter, null)
    }

    override fun drawBitmap(bitmap: Bitmap, src: Rect?, dst: Rect?) {
        drawBitmap(bitmap, src, RectF(dst))
    }

    override fun drawBitmap(bitmap: Bitmap, src: RectF?, dst: RectF?, textureFilter: TextureFilter) {
        if (dst == null) {
            throw NullPointerException()
        }
        val basicTexture = getTexture(bitmap, textureFilter)
        glCanvas.drawTexture(basicTexture, src, dst, textureFilter, null)
    }

    override fun drawBitmap(bitmap: Bitmap, left: Int, top: Int, width: Int, height: Int) {
        drawBitmap(bitmap, left, top, width, height, defaultTextureFilter)
    }

    override fun drawBitmap(bitmap: Bitmap, left: Int, top: Int, width: Int, height: Int, textureFilter: TextureFilter) {
        val basicTexture = getTexture(bitmap, textureFilter)
        glCanvas.drawTexture(basicTexture, left, top, width, height, textureFilter, null)
    }

    private fun getTexture(bitmap: Bitmap, textureFilter: TextureFilter?): BasicTexture? {
        currentTextureFilter = textureFilter
        throwIfCannotDraw(bitmap)
        var resultTexture = getTextureFromMap(bitmap)
        if (textureFilter is FilterGroup) {
            resultTexture = textureFilter.draw(resultTexture!!, glCanvas, object : FilterGroup.OnDrawListener {
                override fun onDraw(drawTexture: BasicTexture?, textureFilter: TextureFilter?, isFirst: Boolean) {
                    glCanvas.drawTexture(drawTexture, 0, 0, drawTexture!!.width, drawTexture.height, textureFilter, null)
                }
            })
        }

        return resultTexture
    }

    /***
     * Use this to the bitmap to texture. Called when your bitmap content pixels changed
     * @param bitmap the bitmap whose content pixels changed
     */
    override fun invalidateTextureContent(bitmap: Bitmap) {
        val resultTexture = getTextureFromMap(bitmap)
        if (resultTexture is UploadedTexture) {
            resultTexture.invalidateContent()
        }
    }

    private fun getTextureFromMap(bitmap: Bitmap): BasicTexture? {
        val resultTexture: BasicTexture?
        if (bitmapTextureMap.containsKey(bitmap)) {
            resultTexture = bitmapTextureMap[bitmap]
        } else {
            resultTexture = BitmapTexture(bitmap)
            bitmapTextureMap[bitmap] = resultTexture
        }

        return resultTexture
    }

    override fun drawCircle(x: Float, y: Float, radius: Float, paint: GLPaint) {
        if (paint.style == Paint.Style.FILL) {
            drawCircleFilter.lineWidth = 0.5f
        } else {
            drawCircleFilter.lineWidth = paint.lineWidth / (2 * radius)
        }
        glCanvas.drawCircle(x - radius, y - radius, radius, paint, drawCircleFilter)
    }

    override fun drawLine(starttX: Float, startY: Float, stopX: Float, stopY: Float, paint: GLPaint?) {
        glCanvas.drawLine(starttX, startY, stopX, stopY, paint, defaultDrawShapeFilter)
    }

    override fun drawRect(rect: RectF, paint: GLPaint) {
        drawRect(rect.left, rect.top, rect.right, rect.bottom, paint)
    }

    override fun drawRect(r: Rect, paint: GLPaint) {
        drawRect(r.left.toFloat(), r.top.toFloat(), r.right.toFloat(), r.bottom.toFloat(), paint)
    }

    override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: GLPaint) {
        if (paint.style == Paint.Style.STROKE) {
            glCanvas.drawRect(left, top, right - left, bottom - top, paint, defaultDrawShapeFilter)
        } else {
            glCanvas.fillRect(left, top, right - left, bottom - top, paint.color, defaultDrawShapeFilter)
        }
    }

    override fun save() {
        glCanvas.save()
    }

    override fun save(saveFlags: Int) {
        glCanvas.save(saveFlags)
    }

    override fun restore() {
        glCanvas.restore()
    }

    override fun rotate(degrees: Float) {
        glCanvas.rotate(degrees, 0f, 0f, 1f)
    }

    override fun rotate(degrees: Float, px: Float, py: Float) {
        glCanvas.translate(px, py)
        rotate(degrees)
        glCanvas.translate(-px, -py)
    }

    override fun scale(sx: Float, sy: Float) {
        glCanvas.scale(sx, sy, 1f)
    }

    override fun scale(sx: Float, sy: Float, px: Float, py: Float) {
        glCanvas.translate(px, py)
        scale(sx, sy)
        glCanvas.translate(-px, -py)
    }

    override fun translate(dx: Float, dy: Float) {
        glCanvas.translate(dx, dy)
    }

    override fun clearBuffer() {
        glCanvas.clearBuffer()
    }

    override fun clearBuffer(color: Int) {
        canvasBackgroundColor[1] = Color.red(color).toFloat() / 255
        canvasBackgroundColor[2] = Color.green(color).toFloat() / 255
        canvasBackgroundColor[3] = Color.blue(color).toFloat() / 255
        canvasBackgroundColor[0] = Color.alpha(color).toFloat() / 255
        glCanvas.clearBuffer(canvasBackgroundColor)
    }

    override fun setSize(width: Int, height: Int) {
        this.width = width
        this.height = height
        glCanvas.setSize(width, height)
    }

    override fun resume() { }

    override fun pause() {
        currentTextureFilter?.destroy()
    }

    override fun setAlpha(@IntRange(from = 0, to = 255) alpha: Int) {
        glCanvas.alpha = alpha / 255.toFloat()
    }

    @Throws(Throwable::class)
    protected fun finalize() {
        for (bitmapTexture in bitmapTextureMap.values) {
            bitmapTexture.recycle()
        }
    }

    private fun throwIfCannotDraw(bitmap: Bitmap) {
        if (bitmap.isRecycled) {
            throw RuntimeException("Canvas: trying to use a recycled bitmap $bitmap")
        }
        if (!bitmap.isPremultiplied && bitmap.config == Bitmap.Config.ARGB_8888 &&
                bitmap.hasAlpha()) {
            throw RuntimeException("Canvas: trying to use a non-premultiplied bitmap $bitmap")
        }
    }
}