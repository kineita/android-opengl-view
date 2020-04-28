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
package jp.eita.example.bubble.model

import android.graphics.PointF
import androidx.annotation.FloatRange
import androidx.annotation.IntRange

open class MovableObject {

    var point: PointF

    var vx: Float

    var vy: Float

    var vRotate: Float

    var collisionRadius: Float

    var rotateDegree = 0f

    var scaleSizeRatio: Float = DEFAULT_SCALE_VALUE
        set(@FloatRange(from = 0.1, to = 2.0) value) {
            field = value
        }

    var alpha: Int = DEFAULT_ALPHA_VALUE
        set(@IntRange(from = 0, to = 255) value) {
            field = value
        }

    constructor(
            point: PointF,
            vx: Float,
            vy: Float,
            vRotate: Float,
            collisionRadius: Float
    ) {
        this.point = point
        this.vx = vx
        this.vy = vy
        this.vRotate = vRotate
        this.collisionRadius = collisionRadius
    }

    fun reset(point: PointF, vx: Float, vy: Float, vRotate: Float, collisionRadius: Float, scaleRatio: Float) {
        this.point = point
        this.vx = vx
        this.vy = vy
        this.vRotate = vRotate
        this.collisionRadius = collisionRadius
        this.scaleSizeRatio = scaleRatio
        this.alpha = DEFAULT_ALPHA_VALUE
    }

    open fun updatePosition(timeMs: Int) {
        point.x += vx * timeMs
        point.y += vy * timeMs
        rotateDegree += vRotate * timeMs
    }

    override fun toString(): String {
        return "${this::class.simpleName}{" +
                "point=" + point +
                ", vx=" + vx +
                ", vy=" + vy +
                ", collisionRadius=" + collisionRadius +
                ", vRotate=" + vRotate +
                ", rotateDegree= $rotateDegree" +
                ", alpha= $alpha" +
                ", scaleSizeRatio= $scaleSizeRatio" +
                '}'
    }

    companion object {

        const val DEFAULT_SCALE_VALUE = 1.0f

        const val DEFAULT_ALPHA_VALUE = 255
    }
}