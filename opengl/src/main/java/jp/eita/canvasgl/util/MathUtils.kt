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

@file:Suppress("unused")

package jp.eita.canvasgl.util

import android.graphics.PointF
import kotlin.math.*

object MathUtils {

    private const val DEFAULT_LAMBDA = 10f

    private const val DEFAULT_SPEED: Int = 15

    fun generateCurve(pointFrom: PointF, pointTo: PointF, radius: Float, speed: Int = DEFAULT_SPEED): ArrayList<PointF> {
        val result: ArrayList<PointF> = ArrayList()
        val dist: Float = distanceOf(pointFrom, pointTo)
        val h = sqrt(radius * radius - dist * dist / 4.0)
        val angleStep: Float = speed / radius
        if (2.0f * radius <= dist) {
            throw Error("Radius is too small")
        }

        // Find center point
        val x1: Float = pointFrom.x
        val x2: Float = pointFrom.y
        val y1: Float = pointTo.x
        val y2: Float = pointTo.y
        val m1: Float = (x1 + y1) / 2
        val m2: Float = (x2 + y2) / 2
        val u1: Float = -(y2 - x2) / dist
        val u2: Float = (y1 - x1) / dist
        val o1: Float = (m1 + h * u1).toFloat()
        val o2: Float = (m2 + h * u2).toFloat()
        val pointCenter = PointF(o1, o2)
        val startAngle: Float = getAngle(pointFrom, pointCenter, radius)
        var endAngle: Float = getAngle(pointTo, pointCenter, radius)
        if (endAngle < startAngle) {
            endAngle += 2.0f * Math.PI.toFloat()
        }
        var ladderStartAngle: Float = startAngle
        while (ladderStartAngle < endAngle) {
            val point = PointF(o1 + radius * cos(ladderStartAngle), o2 + radius * sin(ladderStartAngle))
            result.add(point)
            ladderStartAngle += angleStep
        }
        result.add(pointTo)

        return result
    }

    private fun getAngle(pointX: PointF, pointY: PointF, radius: Float): Float {
        val cosA: Float = (pointX.x - pointY.x) / radius
        val sinA: Float = (pointX.y - pointY.y) / radius
        val angle: Float = acos(cosA)

        return (if (sin(angle) * sinA >= 0f) {
            angle
        } else {
            2.0f * Math.PI.toFloat() - angle
        })
    }

    private fun distanceOf(pointX: PointF, pointY: PointF): Float {
        return hypot(pointX.x - pointY.x, pointX.y - pointY.y)
    }

    fun generateLine(pointFrom: PointF, pointTo: PointF, speed: Int = DEFAULT_SPEED): List<PointF> {
        val result = ArrayList<PointF>()
        val distance = distanceOf(pointFrom, pointTo)
        val distanceFromO = distanceOf(pointFrom, PointF(0f, 0f))
        val distanceToO = distanceOf(pointTo, PointF(0f, 0f))

        var deltaX = 0f
        var deltaY = 0f
        var flag = 0f
        while (flag < distance) {
            val lambda = DEFAULT_LAMBDA + speed
            flag += lambda
            if (distanceFromO > distanceToO) {
                deltaX -= if (pointFrom.x == pointTo.x) 0f else lambda
                deltaY -= if (pointFrom.y == pointTo.y) 0f else lambda
            } else {
                deltaX += if (pointFrom.x == pointTo.x) 0f else lambda
                deltaY += if (pointFrom.y == pointTo.y) 0f else lambda
            }
            val x = if (pointTo.x >= 0) pointFrom.x + deltaX else pointFrom.x - deltaX
            val y = if (pointTo.y >= 0) pointFrom.y + deltaY else pointFrom.y - deltaY
            val pointF = PointF(x, y)
            result.add(pointF)
        }

        // Relocation last item
        if (result.isNotEmpty()) {
            val lastLocation = result.size - 1
            when {
                result[lastLocation].x != pointTo.x -> {
                    result[lastLocation].x = pointTo.x
                }
                result[lastLocation].y != pointTo.y -> {
                    result[lastLocation].y = pointTo.y
                }
            }
        }

        return result
    }

    fun findMidpoint(pointX: PointF, pointY: PointF): PointF {
        val x: Float = (pointX.x + pointY.x) / 2
        val y: Float = (pointX.y + pointY.y) / 2

        return PointF(x, y)
    }
}