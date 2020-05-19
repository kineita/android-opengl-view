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
import jp.eita.canvasgl.shapeFilter.DrawShapeFilter
import jp.eita.canvasgl.textureFilter.TextureFilter
import java.nio.ByteBuffer
import java.nio.FloatBuffer

//
// GLCanvas gives a convenient interface to draw using OpenGL.
//
// When a rectangle is specified in this interface, it means the region
// [x, x+width) * [y, y+height)
//
interface GLCanvas {

    val gLId: GLId?

    // Tells GLCanvas the size of the underlying GL surface. This should be
    // called before first drawing and when the size of GL surface is changed.
    // This is called by GLRoot and should not be called by the clients
    // who only want to draw on the GLCanvas. Both width and height must be
    // nonnegative.
    fun setSize(width: Int, height: Int)

    // Clear the drawing buffers.
    fun clearBuffer()
    fun clearBuffer(argb: FloatArray?)

    // Sets and gets the current alpha, alpha must be in [0, 1].
    var alpha: Float

    // (current alpha) = (current alpha) * alpha
    fun multiplyAlpha(alpha: Float)

    // Change the current transform matrix.
    fun translate(x: Float, y: Float, z: Float)

    fun translate(x: Float, y: Float)

    fun scale(sx: Float, sy: Float, sz: Float)

    fun rotate(angle: Float, x: Float, y: Float, z: Float)

    fun multiplyMatrix(matrix: FloatArray?, offset: Int)

    // Pushes the configuration state (matrix, and alpha) onto
    // a private stack.
    fun save()

    // Same as save(), but only save those specified in saveFlags.
    fun save(saveFlags: Int)

    // Pops from the top of the stack as current configuration state (matrix,
    // alpha, and clip). This call balances a previous call to save(), and is
    // used to remove all modifications to the configuration state since the
    // last save call.
    fun restore()

    fun drawCircle(x: Float, y: Float, radius: Float, paint: GLPaint?, drawShapeFilter: DrawShapeFilter?)

    // Draws a line using the specified paint from (x1, y1) to (x2, y2).
    // (Both end points are included).
    fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float, paint: GLPaint?, drawShapeFilter: DrawShapeFilter?)

    // Draws a rectangle using the specified paint from (x1, y1) to (x2, y2).
    // (Both end points are included).
    fun drawRect(x1: Float, y1: Float, x2: Float, y2: Float, paint: GLPaint?, drawShapeFilter: DrawShapeFilter?)

    // Fills the specified rectangle with the specified color.
    fun fillRect(x: Float, y: Float, width: Float, height: Float, color: Int, drawShapeFilter: DrawShapeFilter?)

    // Draws a secondBitmap to the specified rectangle.
    fun drawTexture(
            texture: BasicTexture?, x: Int, y: Int, width: Int, height: Int, textureFilter: TextureFilter?, customMVPMatrix: ICustomMVPMatrix?)

    fun drawMesh(tex: BasicTexture?, x: Int, y: Int, xyBuffer: Int,
                 uvBuffer: Int, indexBuffer: Int, indexCount: Int, mode: Int)

    // Draws the source rectangle part of the secondBitmap to the target rectangle.
    fun drawTexture(texture: BasicTexture?, source: RectF?, target: RectF?, textureFilter: TextureFilter?, customMVPMatrix: ICustomMVPMatrix?)

    // Draw a secondBitmap with a specified secondBitmap transform.
    fun drawTexture(texture: BasicTexture?, textureTransform: FloatArray?,
                    x: Int, y: Int, w: Int, h: Int, textureFilter: TextureFilter?, customMVPMatrix: ICustomMVPMatrix?)

    // Draw two textures to the specified rectangle. The actual secondBitmap used is
    // from * (1 - ratio) + to * ratio
    // The two textures must have the same size.
    fun drawMixed(from: BasicTexture?, toColor: Int,
                  ratio: Float, x: Int, y: Int, w: Int, h: Int, drawShapeFilter: DrawShapeFilter?)

    // Draw a region of a secondBitmap and a specified color to the specified
    // rectangle. The actual color used is from * (1 - ratio) + to * ratio.
    // The region of the secondBitmap is defined by parameter "src". The target
    // rectangle is specified by parameter "target".
    fun drawMixed(from: BasicTexture?, toColor: Int,
                  ratio: Float, src: RectF?, target: RectF?, drawShapeFilter: DrawShapeFilter?)

    // Unloads the specified secondBitmap from the canvas. The resource allocated
    // to draw the secondBitmap will be released. The specified secondBitmap will return
    // to the unloaded state. This function should be called only from
    // BasicTexture or its descendant
    fun unloadTexture(texture: BasicTexture?): Boolean

    // Delete the specified buffer object, similar to unloadTexture.
    fun deleteBuffer(bufferId: Int)

    // Delete the textures and buffers in GL side. This function should only be
    // called in the GL thread.
    fun deleteRecycledResources()

    // Dump statistics information and clear the counters. For debug only.
    fun dumpStatisticsAndClear()

    fun beginRenderTarget(texture: RawTexture?)

    fun endRenderTarget()

    /**
     * Sets secondBitmap parameters to use GL_CLAMP_TO_EDGE for both
     * GL_TEXTURE_WRAP_S and GL_TEXTURE_WRAP_T. Sets secondBitmap parameters to be
     * GL_LINEAR for GL_TEXTURE_MIN_FILTER and GL_TEXTURE_MAG_FILTER.
     * bindTexture() must be called prior to this.
     *
     * @param texture The secondBitmap to set parameters on.
     */
    fun setTextureParameters(texture: BasicTexture?)

    /**
     * Initializes the secondBitmap to a size by calling texImage2D on it.
     *
     * @param texture The secondBitmap to initialize the size.
     * @param format  The secondBitmap format (e.g. GL_RGBA)
     * @param type    The secondBitmap type (e.g. GL_UNSIGNED_BYTE)
     */
    fun initializeTextureSize(texture: BasicTexture?, format: Int, type: Int)

    /**
     * Initializes the secondBitmap to a size by calling texImage2D on it.
     *
     * @param texture The secondBitmap to initialize the size.
     * @param bitmap  The bitmap to initialize the bitmap with.
     */
    fun initializeTexture(texture: BasicTexture?, bitmap: Bitmap?)

    /**
     * Calls glTexSubImage2D to upload a bitmap to the secondBitmap.
     *
     * @param texture The target secondBitmap to write to.
     * @param xOffset Specifies a texel offset in the x direction within the
     * secondBitmap array.
     * @param yOffset Specifies a texel offset in the y direction within the
     * secondBitmap array.
     * @param format  The secondBitmap format (e.g. GL_RGBA)
     * @param type    The secondBitmap type (e.g. GL_UNSIGNED_BYTE)
     */
    fun texSubImage2D(texture: BasicTexture?, xOffset: Int, yOffset: Int,
                      bitmap: Bitmap?,
                      format: Int, type: Int)

    /**
     * Generates buffers and uploads the buffer data.
     *
     * @param buffer The buffer to upload
     * @return The buffer ID that was generated.
     */
    fun uploadBuffer(buffer: FloatBuffer?): Int

    /**
     * Generates buffers and uploads the element array buffer data.
     *
     * @param buffer The buffer to upload
     * @return The buffer ID that was generated.
     */
    fun uploadBuffer(buffer: ByteBuffer?): Int

    /**
     * After LightCycle makes GL calls, this method is called to restore the GL
     * configuration to the one expected by GLCanvas.
     */
    fun recoverFromLightCycle()

    /**
     * Gets the bounds given by x, y, width, and height as well as the internal
     * matrix state. There is no special handling for non-90-degree rotations.
     * It only considers the lower-left and upper-right corners as the bounds.
     *
     * @param bounds The output bounds to write to.
     * @param x      The left side of the input rectangle.
     * @param y      The bottom of the input rectangle.
     * @param width  The width of the input rectangle.
     * @param height The height of the input rectangle.
     */
    fun getBounds(bounds: Rect?, x: Int, y: Int, width: Int, height: Int)

    interface OnPreDrawTextureListener {

        fun onPreDraw(textureProgram: Int, texture: BasicTexture?, textureFilter: TextureFilter?)
    }

    interface OnPreDrawShapeListener {

        fun onPreDraw(program: Int, drawShapeFilter: DrawShapeFilter?)
    }

    interface ICustomMVPMatrix {

        fun getMVPMatrix(viewportW: Int, viewportH: Int, x: Float, y: Float, drawW: Float, drawH: Float): FloatArray?
    }

    companion object {

        const val SAVE_FLAG_ALL = -0x1

        const val SAVE_FLAG_ALPHA = 0x01

        const val SAVE_FLAG_MATRIX = 0x02
    }
}