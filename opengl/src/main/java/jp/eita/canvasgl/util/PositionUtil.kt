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

package jp.eita.canvasgl.util

import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.PointF

object PositionUtil {

    fun convertPathToListPoint(path: Path, acceleration: Int): List<PointF> {
        val result = ArrayList<PointF>()
        val pathMeasure = PathMeasure(path, false)
        val length = pathMeasure.length
        var distance = 0f
        val delta = length / acceleration
        var counter = 0
        val aCoordinates = FloatArray(2)

        while (distance < length && counter < acceleration) {
            // get point from the path
            pathMeasure.getPosTan(distance, aCoordinates, null)
            val pointF = PointF(aCoordinates[0], aCoordinates[1])
            result.add(pointF)
            counter++
            distance += delta
        }

        return result
    }

}