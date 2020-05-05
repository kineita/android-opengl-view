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
package jp.eita.example.model

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import androidx.annotation.FloatRange
import jp.eita.canvasgl.ICanvasGL
import jp.eita.canvasgl.textureFilter.BasicTextureFilter
import jp.eita.canvasgl.textureFilter.TextureFilter
import jp.eita.canvasgl.util.Loggers

class Bubble : MovableCollisionObject {

    private val bitmap: Bitmap

    private var textureFilter: TextureFilter? = null

    private val paint: Paint

    constructor(
            point: PointF,
            vx: Float,
            vy: Float,
            vRotate: Float,
            collisionRadius: Float,
            bitmap: Bitmap,
            textureFilter: TextureFilter? = null,
            @FloatRange(from = 0.1, to = 2.0) scaleSizeRatio: Float = DEFAULT_SCALE_VALUE,
            alpha: Int = DEFAULT_ALPHA_VALUE
    ) : super(point, vx, vy, vRotate, rotateDegree = bitmap.width / 2f, collisionRadius = collisionRadius) {
        this.bitmap = bitmap
        this.paint = Paint()
        if (textureFilter == null) {
            this.textureFilter = BasicTextureFilter()
        } else {
            this.textureFilter = textureFilter
        }
        this.alpha = alpha
        this.scaleSizeRatio = scaleSizeRatio
    }

    override fun glDraw(iCanvasGL: ICanvasGL) {
        iCanvasGL.save()
        val left = (point.x - bitmap.width / 2f).toInt()
        val top = (point.y - bitmap.height / 2f).toInt()
        iCanvasGL.rotate(rotateDegree, point.x, point.y)
        iCanvasGL.setAlpha(alpha)
        iCanvasGL.drawBitmap(bitmap, scaleSizeRatio, left, top, textureFilter!!)
        iCanvasGL.restore()
    }

    override fun normalDraw(canvas: Canvas) {
        canvas.save()
        val left = (point.x - bitmap.width / 2.toFloat()).toInt()
        val top = (point.y - bitmap.height / 2.toFloat()).toInt()
        canvas.rotate(rotateDegree, point.x, point.y)
        canvas.drawBitmap(bitmap, left.toFloat(), top.toFloat(), paint)
        canvas.restore()
    }

    override fun onCollision(direction: Int) {
        Loggers.d(TAG, "onCollision = $direction")
        super.onCollision(direction = direction)
        if (direction == CollisionListener.DIRECTION_HORIZONTAL) {
            vx = -vx
        } else if (direction == CollisionListener.DIRECTION_VERTICAL) {
            vy = -vy
        }
    }

    companion object {

        val TAG: String = this::class.simpleName.toString()
    }
}