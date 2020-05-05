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

import android.graphics.PointF

open class MovableCollisionObject(
        point: PointF,

        vx: Float,

        vy: Float,

        vRotate: Float,

        rotateDegree: Float = DEFAULT_ROTATE_DEGREE,

        scaleSizeRatio: Float = DEFAULT_SCALE_VALUE,

        alpha: Int = DEFAULT_ALPHA_VALUE,

        var collisionRadius: Float

) : MovableObject(point = point, vx = vx, vy = vy, vRotate = vRotate, rotateDegree = rotateDegree, scaleSizeRatio = scaleSizeRatio, alpha = alpha) {

    var onCollisionListener: CollisionListener? = null

    open fun onCollision(direction: Int) {
        onCollisionListener?.onCollision(direction = direction)
    }

    override fun toString(): String {
        return super.toString() + ", collisionRadius=$collisionRadius"
    }

    interface CollisionListener {

        fun onCollision(direction: Int)

        companion object {

            const val DIRECTION_HORIZONTAL = 0

            const val DIRECTION_VERTICAL = 1
        }
    }
}