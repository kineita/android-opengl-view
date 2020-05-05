/*
 *
 *  * Copyright [2020 - Present] [Lê Trần Ngọc Thành - 瑛太 (eita)] [kineita (Github)]
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package jp.eita.example.model

import android.graphics.Bitmap
import android.graphics.PointF
import jp.eita.canvasgl.ICanvasGL
import jp.eita.canvasgl.textureFilter.BasicTextureFilter
import jp.eita.canvasgl.textureFilter.TextureFilter

class Reaction : MovableCollisionObject {

    val bitmap: Bitmap

    private var textureFilter: TextureFilter

    constructor(
            point: PointF,
            vx: Float,
            vy: Float,
            vRotate: Float,
            rotateDegree: Float = DEFAULT_ROTATE_DEGREE,
            scaleSizeRatio: Float = DEFAULT_SCALE_VALUE,
            alpha: Int = DEFAULT_ALPHA_VALUE,
            collisionRadius: Float = -1f, // Set it to -1 to ignore collision.
            textureFilter: TextureFilter = BasicTextureFilter(),
            bitmap: Bitmap
    ) : super(point, vx, vy, vRotate, rotateDegree, scaleSizeRatio, alpha, collisionRadius) {
        this.textureFilter = textureFilter
        this.bitmap = bitmap
    }

    override fun glDraw(iCanvasGL: ICanvasGL) {
        iCanvasGL.save()
        val left = (point.x - bitmap.width / 2f).toInt()
        val top = (point.y - bitmap.height / 2f).toInt()
        iCanvasGL.setAlpha(alpha)
        iCanvasGL.drawBitmap(bitmap, scaleSizeRatio, left, top, textureFilter)
        iCanvasGL.restore()
    }

    override fun updatePosition(timeMs: Int) {
        point.y += vy * timeMs
    }

    override fun onDestroy() {

    }
}