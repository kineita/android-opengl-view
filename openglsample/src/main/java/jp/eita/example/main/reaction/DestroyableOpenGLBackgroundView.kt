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

package jp.eita.example.main.reaction

import android.content.Context
import android.util.AttributeSet
import jp.eita.canvasgl.ICanvasGL
import jp.eita.canvasgl.glview.GLContinuousView
import jp.eita.canvasgl.util.Loggers
import jp.eita.example.model.*
import jp.eita.example.model.Wall.WallX
import jp.eita.example.model.Wall.WallY
import java.util.*

class DestroyableOpenGLBackgroundView : GLContinuousView {

    val reactionList: MutableList<Reaction> = ArrayList()

    private val wallTop: Wall = WallY(0F)

    private val wallLeft: Wall = WallX(0F)

    private var wallBottom: Wall? = null

    private var wallRight: Wall? = null

    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    private val onCollisionListener: MovableCollisionObject.CollisionListener = object : MovableCollisionObject.CollisionListener {

        override fun onCollision(direction: Int) {

        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        wallBottom = WallY(h.toFloat())
        wallRight = WallX(w.toFloat())
    }

    override fun onGLDraw(canvas: ICanvasGL) {
        val iterator: MutableIterator<Reaction> = reactionList.iterator()
        while (iterator.hasNext()) {
            val reaction = iterator.next()
            if (isOutOfRange(reaction)) {
                iterator.remove()
                continue
            }

            reaction.glDraw(canvas)
            if (wallTop.isTouch(reaction.point, reaction.collisionRadius)
                    || wallBottom!!.isTouch(reaction.point, reaction.collisionRadius)) {
                reaction.onCollision(MovableCollisionObject.CollisionListener.DIRECTION_VERTICAL)
            } else if (wallLeft.isTouch(reaction.point, reaction.collisionRadius)
                    || wallRight!!.isTouch(reaction.point, reaction.collisionRadius)) {
                reaction.onCollision(MovableCollisionObject.CollisionListener.DIRECTION_HORIZONTAL)
            }

            reaction.updatePosition(INTERNAL_TIME_MS)
        }
    }

    private fun isOutOfRange(reaction: Reaction): Boolean {
        if (reaction.point.y < -reaction.bitmap.height) {
            return true
        }

        return false
    }

    companion object {

        private const val INTERNAL_TIME_MS = 40
    }
}