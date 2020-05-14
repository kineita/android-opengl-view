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

package jp.eita.canvasgl.pathManager

import android.graphics.Path

open class PathManagerConfig<POINT : PathManagerConfig.PointConfig
        , ALPHA : PathManagerConfig.AlphaConfig
        , SCALE : PathManagerConfig.ScaleSizeConfig> {

    open var point: POINT? = null

    open var alpha: ALPHA? = null

    open var scaleSize: SCALE? = null

    open class BaseConfig(
            // This is number of points is created on screen.
            open var size: Int = 60
    )

    open class AlphaConfig : BaseConfig() {

        open val listLevel: MutableList<Level> = ArrayList()

        open fun addLevel(level: Level): AlphaConfig {
            if (listLevel.isNotEmpty()) {
                val previousLevel = listLevel[listLevel.size - 1]
                when {
                    level.position <= previousLevel.position -> {
                        throw IllegalArgumentException("$level is added which has position from <= $previousLevel, please set its position larger than.")
                    }
                    level.position > size -> {
                        throw IllegalArgumentException("$level is added which has position >= $size, please set it smaller than.")
                    }
                }
            }

            listLevel.add(level)
            return this
        }

        open class Level(val position: Int, val alpha: Int) {

            override fun toString(): String {
                return "AlphaConfig.Level(position=$position, alpha=$alpha)"
            }
        }
    }

    open class ScaleSizeConfig : BaseConfig() {

        open val listLevel: MutableList<Level> = ArrayList()

        open fun addLevel(level: Level): ScaleSizeConfig {
            if (listLevel.isNotEmpty()) {
                val previousLevel = listLevel[listLevel.size - 1]
                when {
                    level.position <= previousLevel.position -> {
                        throw IllegalArgumentException("$level is added which has position from <= $previousLevel, please set its position larger than.")
                    }
                    level.position > size -> {
                        throw IllegalArgumentException("$level is added which has position >= $size, please set it smaller than.")
                    }
                }
            }

            listLevel.add(level)
            return this
        }

        open class Level(val position: Int, val scaleSizeRatio: Float) {

            override fun toString(): String {
                return "ScaleSizeConfig.Level(position=$position, scaleSizeRatio=$scaleSizeRatio)"
            }
        }
    }

    /**
     * [PointConfig] will generate for your a [android.graphics.Path]
     */
    open class PointConfig : BaseConfig {

        protected lateinit var path: Path

        constructor() : super()

        constructor(path: Path) : super() {
            this.path = path
        }

        /**
         * Applies resolved control points to the specified Path.
         */
        open fun generatePath(): Path {
            return this.path
        }
    }
}