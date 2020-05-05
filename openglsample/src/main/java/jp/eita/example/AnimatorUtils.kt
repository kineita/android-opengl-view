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
import jp.eita.canvasgl.glview.GLObject
import jp.eita.canvasgl.util.Loggers
import jp.eita.example.model.MovableObject
import jp.eita.example.structure.AlphaGenerator
import jp.eita.example.structure.ScaleSizeRatioGenerator

object AnimatorUtils {

    private val TAG: String = AnimatorUtils::class.simpleName.toString()

    fun animateScaleSize(movableObject: MovableObject, scaleSizeRatio: Float, timeDelayToAnimate: Long) {
        val scaleSizeRatioGenerator = ScaleSizeRatioGenerator(scaleSizeRatio)
        val runnable: Runnable = object : Runnable {
            override fun run() {
                if (movableObject.status == GLObject.Status.DESTROY) {
                    Loggers.d(TAG, "End of scale size runnable")
                    return
                }

                movableObject.scaleSizeRatio = scaleSizeRatioGenerator.scaleRatioValue
                Handler().postDelayed(this, timeDelayToAnimate)
            }
        }
        Handler().postDelayed(runnable, timeDelayToAnimate)
    }

    fun animateAlpha(movableObject: MovableObject, alpha: Int, timeDelayToAnimate: Long) {
        val alphaGenerator = AlphaGenerator(alpha)
        val runnable: Runnable = object : Runnable {
            override fun run() {
                if (movableObject.status == GLObject.Status.DESTROY) {
                    Loggers.d(TAG, "End of alpha runnable")
                    return
                }

                movableObject.alpha = alphaGenerator.alphaValue
                Handler().postDelayed(this, timeDelayToAnimate)
            }
        }
        Handler().postDelayed(runnable, timeDelayToAnimate)
    }
}