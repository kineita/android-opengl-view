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

package jp.eita.canvasgl.glcanvas

import android.graphics.PointF
import jp.eita.canvasgl.MathUtils
import kotlin.math.abs

class GLPath(

        private val width: Int, // Width of GlView

        private val height: Int, // Height of GlView

        private val ratioScreen: Float // Such as 16:9, 21:9, 16:10,...
) {

    var pattern: IntArray = STRAIGHT
        set(value) {
            checkValidPattern(value)
            this.pointArray = convertPatternToPointList(value)
            field = value
        }

    lateinit var pointArray: List<PointF>

    init {
        pattern = STRAIGHT_LEFT_STRAIGHT
    }

    private fun checkValidPattern(pattern: IntArray) {
        if (pattern.size != LENGTH_MATRIX * LENGTH_MATRIX) {
            throw IllegalArgumentException("Pattern size must be 25, please check it again.")
        }

        val hashSet: HashSet<Int> = HashSet()
        val size = pattern.size
        for (i in 0 until size) {
            if (pattern[i] == 1) {
                if (hashSet.contains(1)) {
                    throw IllegalArgumentException("Number 1 can only appear one time each 5 element.")
                }
                hashSet.add(pattern[i])
            }

            if (i % LENGTH_MATRIX == 0) {
                hashSet.clear()
            }
        }
    }

    private fun convertPatternToPointList(pattern: IntArray): List<PointF> {
        val rationOfBox = ratioScreen / LENGTH_MATRIX
        val moveX = rationOfBox * width
        val moveY = rationOfBox * height
        val result: MutableList<PointF> = ArrayList()
        val size = pattern.size
        for (i in size - 1 downTo 0) {
            val col: Int = i % LENGTH_MATRIX
            val row: Int = i / LENGTH_MATRIX
            var isFirstTime = false
            if (pattern[i] == 1) {
                val x = moveX * col
                val y = moveY * row
                if (result.isEmpty()) {
                    isFirstTime = true
                }
                val pointF = PointF(x, y)
                result.add(pointF)
                val lastPointF: PointF? = if (isFirstTime) {
                    null
                } else {
                    result[result.size - 1]
                }
                val currentPointF: PointF? = if (result.size < 2) null else result[result.size - 2]
                createSubMatrix(
                        currentPointF = currentPointF,
                        lastPointF = lastPointF,
                        listPoint = result
                )

            }
        }
        result.add(PointF(-400f, -1000f))

        return result
    }

    private fun createSubMatrix(
            currentPointF: PointF?,
            lastPointF: PointF?,
            listPoint: MutableList<PointF>
    ) {
        if (currentPointF == null || lastPointF == null) {
            return
        }
        listPoint.remove(lastPointF)

        val deltaX: Float = if (lastPointF.x != currentPointF.x) {
            abs(abs(lastPointF.x) - abs(currentPointF.x)) / LENGTH_TINY_SUB_MATRIX
        } else {
            0.0f
        }
        val deltaY: Float = if (lastPointF.y != currentPointF.y) {
            val distanceY = abs(abs(lastPointF.y) - abs(currentPointF.y))
            distanceY / LENGTH_TINY_SUB_MATRIX
        } else {
            0.0f
        }

        // Begin creating curve line
        if (deltaX != 0f && deltaY != 0f) {
            val listCurvePoint = MathUtils.generateCurve(
                    pointFrom = currentPointF,
                    pointTo = lastPointF,
                    radius = 300f,
                    distance = 15f
            )
            listPoint.addAll(listCurvePoint)
        }

        var ladderDeltaX: Float = deltaX
        var ladderDeltaY: Float = deltaY
        for (k in 0 until LENGTH_TINY_SUB_MATRIX) {
            val x = currentPointF.x - ladderDeltaX
            ladderDeltaX += deltaX
            val y = currentPointF.y - ladderDeltaY
            ladderDeltaY += deltaY
            val pointF = PointF(x, y)
            listPoint.add(pointF)
        }
    }

    companion object {

        const val LENGTH_MATRIX: Int = 9

        const val LENGTH_TINY_SUB_MATRIX: Int = 4

        val EMPTY: IntArray = intArrayOf(
                0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0
        )

        val STRAIGHT: IntArray = intArrayOf(
                0, 0, 0, 0, 1, 0, 0, 0, 0,
                0, 0, 0, 0, 1, 0, 0, 0, 0,
                0, 0, 0, 0, 1, 0, 0, 0, 0,
                0, 0, 0, 0, 1, 0, 0, 0, 0,
                0, 0, 0, 0, 1, 0, 0, 0, 0,
                0, 0, 0, 0, 1, 0, 0, 0, 0,
                0, 0, 0, 0, 1, 0, 0, 0, 0,
                0, 0, 0, 0, 1, 0, 0, 0, 0,
                0, 0, 0, 0, 1, 0, 0, 0, 0
        )

        val STRAIGHT_LEFT_STRAIGHT: IntArray = intArrayOf(
                0, 0, 0, 0, 1, 0, 0, 0, 0,
                0, 0, 0, 0, 1, 0, 0, 0, 0,
                0, 0, 0, 0, 1, 0, 0, 0, 0,
                0, 0, 0, 1, 0, 0, 0, 0, 0,
                0, 0, 0, 1, 0, 0, 0, 0, 0,
                0, 0, 0, 1, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 1, 0, 0, 0, 0,
                0, 0, 0, 0, 1, 0, 0, 0, 0,
                0, 0, 0, 0, 1, 0, 0, 0, 0
        )

        val STRAIGHT_RIGHT_STRAIGHT: IntArray = intArrayOf(
                0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0
        )
    }
}