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
        val listOneValuePosition = ArrayList<PointF>()

        // Find position of all one value from pattern.
        var size = pattern.size
        for (i in size - 1 downTo 0) {
            val col: Int = i % LENGTH_MATRIX
            val row: Int = i / LENGTH_MATRIX
            if (pattern[i] == 1) {
                val x = moveX * col
                val y = moveY * row
                val point = PointF(x, y)
                listOneValuePosition.add(point)
            }
        }

        // Generate list point (path) by pattern.
        size = listOneValuePosition.size
        for (i in 1 until size) {
            val currentPoint = listOneValuePosition[i]
            val lastPoint = listOneValuePosition[i - 1]
            val isDifferentX = lastPoint.x != currentPoint.x
            val isDifferentY = lastPoint.y != currentPoint.y
            when {
                isDifferentX && isDifferentY -> {
                    val listCurve = MathUtils.generateCurve(lastPoint, currentPoint, 320f, speed = 10)
                    result.addAll(listCurve)
                }
                else -> {
                    val listLine = MathUtils.generateLine(lastPoint, currentPoint, speed = 10)
                    result.addAll(listLine)
                }
            }

            if (i == size - 1) {
                val listLine = MathUtils.generateLine(currentPoint, PointF(currentPoint.x, currentPoint.y - 400), speed = 10)
                result.addAll(listLine)
            }
        }

        return result
    }

    companion object {

        const val LENGTH_MATRIX: Int = 9

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
                0, 0, 0, 0, 1, 0, 0, 0, 0,
                0, 0, 0, 0, 1, 0, 0, 0, 0,
                0, 0, 0, 0, 1, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 1, 0, 0, 0,
                0, 0, 0, 0, 0, 1, 0, 0, 0,
                0, 0, 0, 0, 0, 1, 0, 0, 0,
                0, 0, 0, 0, 1, 0, 0, 0, 0,
                0, 0, 0, 0, 1, 0, 0, 0, 0,
                0, 0, 0, 0, 1, 0, 0, 0, 0
        )
    }
}