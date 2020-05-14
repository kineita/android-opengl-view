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

import android.graphics.PointF

open class PathManager<T : PathManagerConfig<*, *, *>> {

    lateinit var config: T

    /**
     * @return path which is List[PointF] after calculating.
     */
    open fun generateListPoint(): List<PointF> = emptyList()

    /**
     * @return list alpha.
     */
    open fun generateListAlpha(): List<Int> = emptyList()

    /**
     * @return list scale size.
     */
    open fun generateListScaleSize(): List<Float> = emptyList()
}