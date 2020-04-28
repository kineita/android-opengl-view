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

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.SurfaceTexture
import androidx.annotation.IntRange
import jp.eita.canvasgl.glcanvas.*
import jp.eita.canvasgl.matrix.IBitmapMatrix
import jp.eita.canvasgl.textureFilter.TextureFilter

interface ICanvasGL {

    fun bindBitmapToTexture(whichTexture: Int, bitmap: Bitmap): BitmapTexture?

    fun bindRawTexture(whichTexture: Int, texture: RawTexture)

    fun beginRenderTarget(texture: RawTexture?)

    fun endRenderTarget()

    val glCanvas: GLCanvas

    fun drawSurfaceTexture(texture: BasicTexture?, surfaceTexture: SurfaceTexture?, left: Int, top: Int, right: Int, bottom: Int)

    fun drawSurfaceTexture(texture: BasicTexture?, surfaceTexture: SurfaceTexture?, left: Int, top: Int, right: Int, bottom: Int, textureFilter: TextureFilter?)

    fun drawSurfaceTexture(texture: BasicTexture?, surfaceTexture: SurfaceTexture?, matrix: IBitmapMatrix?)

    fun drawSurfaceTexture(texture: BasicTexture, surfaceTexture: SurfaceTexture?, matrix: IBitmapMatrix?, textureFilter: TextureFilter)

    fun drawSurfaceTexture(texture: BasicTexture?, surfaceTexture: SurfaceTexture?, left: Int, top: Int, right: Int, bottom: Int, matrix: IBitmapMatrix?, textureFilter: TextureFilter?)

    fun drawBitmap(bitmap: Bitmap, matrix: IBitmapMatrix)

    fun drawBitmap(bitmap: Bitmap, matrix: IBitmapMatrix, textureFilter: TextureFilter)

    fun drawBitmap(bitmap: Bitmap, src: Rect?, dst: RectF?)

    fun drawBitmap(bitmap: Bitmap, left: Int, top: Int)

    fun drawBitmap(bitmap: Bitmap, left: Int, top: Int, textureFilter: TextureFilter)

    fun drawBitmap(bitmap: Bitmap, src: Rect?, dst: Rect?)

    fun drawBitmap(bitmap: Bitmap, src: RectF?, dst: RectF?, textureFilter: TextureFilter)

    fun drawBitmap(bitmap: Bitmap, left: Int, top: Int, width: Int, height: Int)

    fun drawBitmap(bitmap: Bitmap, left: Int, top: Int, width: Int, height: Int, textureFilter: TextureFilter)

    fun invalidateTextureContent(bitmap: Bitmap)

    fun drawCircle(x: Float, y: Float, radius: Float, paint: GLPaint)

    fun drawLine(starttX: Float, startY: Float, stopX: Float, stopY: Float, paint: GLPaint?)

    fun drawRect(rect: RectF, paint: GLPaint)

    fun drawRect(r: Rect, paint: GLPaint)

    fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: GLPaint)

    fun save()

    fun save(saveFlags: Int)

    fun restore()

    fun rotate(degrees: Float)

    fun rotate(degrees: Float, px: Float, py: Float)

    fun scale(sx: Float, sy: Float)

    fun scale(sx: Float, sy: Float, px: Float, py: Float)

    fun translate(dx: Float, dy: Float)

    fun clearBuffer()

    fun clearBuffer(color: Int)

    fun setSize(width: Int, height: Int)

    val width: Int

    val height: Int

    fun resume()

    fun pause()

    /**
     * If used in a texture view, make sure the setOpaque(false) is called.
     *
     * @param alpha alpha value
     */
    fun setAlpha(@IntRange(from = 0, to = 255) alpha: Int)
}