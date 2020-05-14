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

    open class Config(
            // This is number of points is created on screen.
            open var size: Int = 40
    )

    open class AlphaConfig : Config()

    open class ScaleSizeConfig : Config()

    /**
     * [PointConfig] will generate for your a [android.graphics.Path]
     */
    open class PointConfig : Config {

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