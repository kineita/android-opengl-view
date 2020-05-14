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

import android.graphics.PointF
import jp.eita.canvasgl.pathManager.PathManager
import jp.eita.canvasgl.pathManager.PathManagerConfig
import jp.eita.canvasgl.pathManager.PathManagerConfig.ScaleSizeConfig
import jp.eita.canvasgl.util.PositionUtil

open class BezierPathManager : PathManager<BezierPathManagerConfig>() {

    init {
        config = BezierPathManagerConfig()
    }

    override fun generateListPoint(): List<PointF> {
        config.point?.let { pointConfig ->
            val path = pointConfig.generatePath()
            return PositionUtil.convertPathToListPoint(path, pointConfig.size)
        }

        throw IllegalArgumentException("Please config ${BezierPathManager::class.simpleName} point configuration first.")
    }

    override fun generateListScaleSize(): List<Float> {
        config.scaleSize?.let { scaleSizeConfig ->
            if (scaleSizeConfig.listLevel.isEmpty()) {
                throw IllegalArgumentException("Please set up list level in config before generating list scale size.")
            }

            val listLevelSorted = scaleSizeConfig.listLevel.sortedWith(compareBy { it.position })
            checkValidSortedScaleSizeListLevel(listLevelSorted)
            val result = ArrayList<Float>(listLevelSorted.size)
            val size = listLevelSorted.size
            for (i in 0 until size - 1) {
                val currentLevel = listLevelSorted[i]
                val nextLevel = listLevelSorted[i + 1]
                val distancePosition = nextLevel.position - currentLevel.position + 1 // + 1 for including last item.
                val distanceScaleSize = nextLevel.scaleSizeRatio - currentLevel.scaleSizeRatio
                val lambda = distanceScaleSize / distancePosition

                var delta = 0f
                for (k in 0 until distancePosition) {
                    result.add(currentLevel.scaleSizeRatio + delta)
                    delta += lambda
                }
            }

            return result
        }

        throw IllegalArgumentException("Please config ${BezierPathManager::class.simpleName} scale size configuration first.")
    }

    private fun checkValidSortedScaleSizeListLevel(listLevelSorted: List<ScaleSizeConfig.Level>) {
        val firstLevel = listLevelSorted[0]
        if (firstLevel.position != 0) {
            throw  IllegalArgumentException("Please add level at position 0.")
        }
        val lastLevel = listLevelSorted[listLevelSorted.size - 1]
        if (lastLevel.position != config.scaleSize!!.size) {
            throw  IllegalArgumentException("Please add level at position ${config.scaleSize!!.size - 1}.")
        }
    }

    override fun generateListAlpha(): List<Int> {
        config.alpha?.let { alphaConfig ->
            if (alphaConfig.listLevel.isEmpty()) {
                throw IllegalArgumentException("Please set up list level in config before generating list scale size.")
            }

            val listLevelSorted = alphaConfig.listLevel.sortedWith(compareBy { it.position })
            checkValidSortedAlphaListLevel(listLevelSorted)
            val result = ArrayList<Int>(listLevelSorted.size)
            val size = listLevelSorted.size
            for (i in 0 until size - 1) {
                val currentLevel = listLevelSorted[i]
                val nextLevel = listLevelSorted[i + 1]
                val distancePosition = nextLevel.position - currentLevel.position + 1 // + 1 for including last item.
                val distanceAlpha = nextLevel.alpha - currentLevel.alpha
                val lambda = distanceAlpha / distancePosition

                var delta = 0
                for (k in 0 until distancePosition) {
                    result.add(currentLevel.alpha + delta)
                    delta += lambda
                }
            }

            return result
        }

        throw IllegalArgumentException("Please config ${BezierPathManager::class.simpleName} alpha configuration first.")
    }

    private fun checkValidSortedAlphaListLevel(listLevelSorted: List<PathManagerConfig.AlphaConfig.Level>) {
        val firstLevel = listLevelSorted[0]
        if (firstLevel.position != 0) {
            throw  IllegalArgumentException("Please add level at position 0.")
        }
        val lastLevel = listLevelSorted[listLevelSorted.size - 1]
        if (lastLevel.position != config.scaleSize!!.size) {
            throw  IllegalArgumentException("Please add level at position ${config.scaleSize!!.size - 1}.")
        }
    }
}