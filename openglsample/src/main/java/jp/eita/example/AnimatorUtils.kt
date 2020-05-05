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

package jp.eita.example

import android.os.Handler
import jp.eita.example.model.MovableCollisionObject
import jp.eita.example.structure.AlphaList
import jp.eita.example.structure.ScaleRatioList

object AnimatorUtils {

    private val listMovableCollisionObject: MutableList<MovableCollisionObject> = ArrayList()

    private val scaleRatioList: ScaleRatioList = ScaleRatioList(0)

    private val alphaList: AlphaList = AlphaList(0)

    private val runnableScaleSizeRatio = Runnable {

    }

    fun animateScaleSize(movableCollisionObject: MovableCollisionObject, scaleSizeRatio: Float, timeDelayToAnimate: Long) {
        listMovableCollisionObject.add(movableCollisionObject)
        scaleRatioList.add(scaleSizeRatio)
        movableCollisionObject.scaleSizeRatio = scaleSizeRatio
        Handler().postDelayed(runnableScaleSizeRatio, timeDelayToAnimate)
    }
}