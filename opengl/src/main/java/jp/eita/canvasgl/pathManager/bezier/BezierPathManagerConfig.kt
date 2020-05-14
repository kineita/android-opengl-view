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

package jp.eita.canvasgl.pathManager.bezier

import android.graphics.Path
import android.graphics.PointF
import jp.eita.canvasgl.pathManager.PathManagerConfig

class BezierPathManagerConfig : PathManagerConfig<BezierPathManagerConfig.PointConfig, PathManagerConfig.AlphaConfig, PathManagerConfig.ScaleSizeConfig>() {

    open class PointConfig : PathManagerConfig.PointConfig {

        private var knots: Int? = null

        private lateinit var mX: FloatArray

        private lateinit var mY: FloatArray

        private lateinit var pX1: FloatArray

        private lateinit var pY1: FloatArray

        private lateinit var pX2: FloatArray

        private lateinit var pY2: FloatArray

        private var resolved = false

        private var resolver: ControlPointsResolver? = null

        constructor(path: Path) : super(path)

        constructor(knots: Int) {
            this.knots = knots
            require(knots > 1) { "At least two knot points required" }
            mX = FloatArray(knots)
            mY = FloatArray(knots)
            val segments = knots - 1
            pX1 = FloatArray(segments)
            pY1 = FloatArray(segments)
            pX2 = FloatArray(segments)
            pY2 = FloatArray(segments)
        }


        /**
         * Gets segments count.
         */
        fun segments(): Int? {
            knots?.let {
                return it - 1
            }

            return null
        }

        /**
         * Sets coordinates of knot.
         */
        operator fun set(knot: Int, point: PointF) {
            mX[knot] = point.x
            mY[knot] = point.y
            resolved = false
        }

        /**
         * Sets x coordinate of knot.
         */
        fun x(knot: Int, x: Float) {
            mX[knot] = x
            resolved = false
        }

        /**
         * Sets y coordinate of knot.
         */
        fun y(knot: Int, y: Float) {
            mY[knot] = y
            resolved = false
        }

        /**
         * Gets x coordinate of knot.
         */
        fun x(knot: Int): Float {
            return mX[knot]
        }

        /**
         * Gets y coordinate of knot.
         */
        fun y(knot: Int): Float {
            return mY[knot]
        }

        /**
         * Gets resolved x coordinate of first control point.
         */
        fun px1(segment: Int): Float {
            ensureResolved()

            return pX1[segment]
        }

        /**
         * Gets resolved y coordinate of first control point.
         */
        fun py1(segment: Int): Float {
            ensureResolved()

            return pY1[segment]
        }

        /**
         * Gets resolved x coordinate of second control point.
         */
        fun px2(segment: Int): Float {
            ensureResolved()

            return pX2[segment]
        }

        /**
         * Gets resolved y coordinate of second control point.
         */
        fun py2(segment: Int): Float {
            ensureResolved()

            return pY2[segment]
        }

        override fun generatePath(): Path {
            knots?.let {
                ensureResolved()
                val pathGenerated = Path()
                pathGenerated.reset()
                pathGenerated.moveTo(mX[0], mY[0])
                val segments = it - 1
                if (segments == 1) {
                    pathGenerated.lineTo(mX[1], mY[1])
                } else {
                    for (segment in 0 until segments) {
                        val knot = segment + 1
                        pathGenerated.cubicTo(
                                pX1[segment],
                                pY1[segment],
                                pX2[segment],
                                pY2[segment],
                                mX[knot],
                                mY[knot])
                    }
                }

                return pathGenerated
            }

            return super.generatePath()
        }

        private fun ensureResolved() {
            if (!resolved) {
                val segments = knots!! - 1
                if (segments == 1) {
                    pX1[0] = mX[0]
                    pY1[0] = mY[0]
                    pX2[0] = mX[1]
                    pY2[0] = mY[1]
                } else {
                    if (resolver == null) {
                        resolver = ControlPointsResolver(segments)
                    }
                    resolver!!.resolve(mX, pX1, pX2)
                    resolver!!.resolve(mY, pY1, pY2)
                }
                resolved = true
            }
        }

        private class ControlPointsResolver internal constructor(private val segments: Int) {

            private val mA: FloatArray = FloatArray(segments)

            private val mB: FloatArray = FloatArray(segments)

            private val mC: FloatArray = FloatArray(segments)

            private val mR: FloatArray = FloatArray(segments)

            fun resolve(k: FloatArray, p1: FloatArray, p2: FloatArray) {
                val segments = segments
                val last = segments - 1
                val a = mA
                val b = mB
                val c = mC
                val r = mR

                // prepare left most segment.
                a[0] = 0f
                b[0] = 2f
                c[0] = 1f
                r[0] = k[0] + 2f * k[1]

                // prepare internal segments.
                for (i in 1 until last) {
                    a[i] = 1f
                    b[i] = 4f
                    c[i] = 1f
                    r[i] = 4f * k[i] + 2f * k[i + 1]
                }

                // prepare right most segment.
                a[last] = 2f
                b[last] = 7f
                c[last] = 0f
                r[last] = 8f * k[last] + k[segments]

                // solves Ax=b with the Thomas algorithm (from Wikipedia).
                for (i in 1 until segments) {
                    val m = a[i] / b[i - 1]
                    b[i] = b[i] - m * c[i - 1]
                    r[i] = r[i] - m * r[i - 1]
                }
                p1[last] = r[last] / b[last]
                for (i in segments - 2 downTo 0) {
                    p1[i] = (r[i] - c[i] * p1[i + 1]) / b[i]
                }

                // we have p1, now compute p2.
                for (i in 0 until last) {
                    p2[i] = 2f * k[i + 1] - p1[i + 1]
                }
                p2[last] = (k[segments] + p1[segments - 1]) / 2f
            }
        }
    }

    open class ScaleSizeConfig : PathManagerConfig.AlphaConfig() {


    }
}