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
import jp.eita.example.model.Reaction
import java.util.*

class DestroyableOpenGLBackgroundView : GLContinuousView {

    val reactionList: MutableList<Reaction> = ArrayList()

    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    override fun onGLDraw(canvas: ICanvasGL) {
        val iterator: MutableIterator<Reaction> = reactionList.iterator()
        while (iterator.hasNext()) {
            Loggers.d("Testing", "reactionList size = ${reactionList.size}")
            val reaction = iterator.next()
            if (isOutOfRange(reaction)) {
                // Clear old reaction to release it.
                canvas.removeBitmapTexture(bitmap = reaction.bitmap)
                reaction.onDestroy()
                iterator.remove()
                continue
            }

            reaction.glDraw(canvas)
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