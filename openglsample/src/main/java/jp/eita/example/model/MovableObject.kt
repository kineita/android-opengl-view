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

import android.graphics.Canvas
import android.graphics.PointF
import jp.eita.canvasgl.ICanvasGL
import jp.eita.canvasgl.glview.GLObject

open class MovableObject(

        var point: PointF,

        var vx: Float,

        var vy: Float,

        var vRotate: Float,

        var rotateDegree: Float = DEFAULT_ROTATE_DEGREE,

        var scaleSizeRatio: Float = DEFAULT_SCALE_VALUE,

        var alpha: Int = DEFAULT_ALPHA_VALUE

) : GLObject() {

    fun reset(point: PointF, vx: Float, vy: Float, vRotate: Float) {
        this.point = point
        this.vx = vx
        this.vy = vy
        this.vRotate = vRotate
        this.rotateDegree = DEFAULT_ROTATE_DEGREE
        this.scaleSizeRatio = DEFAULT_SCALE_VALUE
        this.alpha = DEFAULT_ALPHA_VALUE
    }

    open fun updatePosition(timeMs: Int) {
        point.x += vx * timeMs
        point.y += vy * timeMs
        rotateDegree += vRotate * timeMs
    }

    override fun toString(): String {
        return "GLObject(point=$point, vx=$vx, vy=$vy, vRotate=$vRotate, rotateDegree=$rotateDegree, scaleSizeRatio=$scaleSizeRatio, alpha=$alpha)"
    }

    override fun glDraw(iCanvasGL: ICanvasGL) {}

    override fun normalDraw(canvas: Canvas) {}

    override fun onDestroy() {}

    companion object {

        const val DEFAULT_ROTATE_DEGREE = 0F

        const val DEFAULT_SCALE_VALUE = 1.0f

        const val DEFAULT_ALPHA_VALUE = 255
    }
}