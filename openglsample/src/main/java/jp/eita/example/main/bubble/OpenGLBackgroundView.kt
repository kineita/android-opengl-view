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
package jp.eita.example.main.bubble

import android.content.Context
import android.util.AttributeSet
import jp.eita.canvasgl.ICanvasGL
import jp.eita.canvasgl.glview.GLContinuousView
import jp.eita.example.model.Bubble
import jp.eita.example.model.MovableCollisionObject
import jp.eita.example.model.Wall
import jp.eita.example.model.Wall.WallX
import jp.eita.example.model.Wall.WallY
import java.util.*

class OpenGLBackgroundView : GLContinuousView {

    val movableCollisionObjectList: MutableList<MovableCollisionObject> = ArrayList()

    private val wallTop: Wall = WallY(0F)

    private val wallLeft: Wall = WallX(0F)

    private var wallBottom: Wall? = null

    private var wallRight: Wall? = null

    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    private val onCollisionListener: MovableCollisionObject.CollisionListener = object : MovableCollisionObject.CollisionListener {

        override fun onCollision(direction: Int) {
//                movableCollisionObjectList.remove(this)
        }
    }

    private fun addBubble(bubble: Bubble) {
        movableCollisionObjectList.add(bubble)
        bubble.onCollisionListener = onCollisionListener
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        wallBottom = WallY(h.toFloat())
        wallRight = WallX(w.toFloat())
    }

    override fun onGLDraw(canvas: ICanvasGL) {
        val iterator: MutableIterator<MovableCollisionObject> = movableCollisionObjectList.iterator()
        while (iterator.hasNext()) {
            val bubble = iterator.next()
            bubble.glDraw(canvas)
            if (wallTop.isTouch(bubble.point, bubble.collisionRadius) || wallBottom!!.isTouch(bubble.point, bubble.collisionRadius)) {
                bubble.onCollision(MovableCollisionObject.CollisionListener.DIRECTION_VERTICAL)
            } else if (wallLeft.isTouch(bubble.point, bubble.collisionRadius) || wallRight!!.isTouch(bubble.point, bubble.collisionRadius)) {
                bubble.onCollision(MovableCollisionObject.CollisionListener.DIRECTION_HORIZONTAL)
            }

            bubble.updatePosition(INTERNAL_TIME_MS)
        }
    }

    companion object {

        private const val INTERNAL_TIME_MS = 16
    }
}