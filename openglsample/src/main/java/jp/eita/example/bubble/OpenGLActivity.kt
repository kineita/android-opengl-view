/*
 * Copyright [2020 - Present] [Lê Trần Ngọc Thành - 瑛太 (eita)] [kineita (Github)]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jp.eita.example.bubble

import android.graphics.Bitmap
import android.graphics.PointF
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import jp.eita.canvasgl.BitmapUtils
import jp.eita.canvasgl.OpenGLUtil
import jp.eita.canvasgl.textureFilter.*
import jp.eita.example.R
import jp.eita.example.bubble.model.Bubble
import jp.eita.example.structure.AlphaList
import jp.eita.example.structure.ScaleRatioList
import kotlinx.android.synthetic.main.activity_opengl.*
import java.util.*
import kotlin.collections.ArrayList

class OpenGLActivity : AppCompatActivity() {

    private val upFilterList: MutableList<TextureFilter> = ArrayList()

    private lateinit var bitmap: Bitmap

    private val scaleRatioList: ScaleRatioList = ScaleRatioList(0)

    private val alphaList: AlphaList = AlphaList(0)

    private val runnable = Runnable {
        loopChangingPropertiesBubbles()
    }

    init {
//        Handler().postDelayed(runnable, 150)
    }

    private fun loopChangingPropertiesBubbles() {
        changingPropertiesBubbles()
        Handler().postDelayed(runnable, 150)
    }

    private fun changingPropertiesBubbles() {
        for (i in 0 until anim_gl_view.bubbles.size) {
            anim_gl_view.bubbles[i].scaleSizeRatio = scaleRatioList.listDetails[i].scaleRatioValue
            anim_gl_view.bubbles[i].alpha = alphaList.listDetails[i].scaleRatioValue
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_opengl)
        title = getString(R.string.bubble_title)
        setUpBitmap()
        setUpEditTextScaleRatio()
        initFilterList(upFilterList)
        setUpButtonLike()
    }

    private fun setUpBitmap() {
        bitmap = BitmapUtils.convertToBitmapFrom(applicationContext, R.drawable.ic_fast_food)!!
//        BitmapUtils.adjustOpacity(bitmap = bitmap, opacity = 120)
//        bitmap = BitmapUtils.getScaledDownBitmap(bitmap, 64, false)
//        anim_gl_view.alpha = 0.6f
    }

    private fun setUpEditTextScaleRatio() {
        editTextScaleRatio.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable?) {
                try {
                    val value: Double = if (s == null || s.toString() == "") {
                        return
                    } else if (s.toString() == "0.") {
                        0.1
                    } else {
                        s.toString().toDouble()
                    }
                    anim_gl_view.onPause()
//                    anim_gl_view.updateScaleRatioForAllBubbles(value.toFloat())
                    anim_gl_view.onResume()
                } catch (ex: Exception) {

                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

        })
    }

    private fun initFilterList(filterList: MutableList<TextureFilter>) {
        filterList.add(PixelationFilter(1f))
//        filterList.add(ContrastFilter(1.6f))
//        filterList.add(SaturationFilter(1.6f))
//        filterList.add(PixelationFilter(12F))
//        filterList.add(HueFilter(100F))
//        filterList.add(CropFilter(120.0f, 120.0f, 120.0f, 120.0f))
    }

    private fun setUpButtonLike() {
        buttonLike.setOnClickListener {
            anim_gl_view.onPause()
            anim_gl_view.bubbles.add(createBubble(upFilterList))
            anim_gl_view.onResume()
        }
    }

    private fun createBubble(filterList: List<TextureFilter>): Bubble {
        val random = Random()
        val textureFilter = filterList[random.nextInt(filterList.size)]
        val vy = -(MIN_VY + random.nextInt(MAX_VY)) * VY_MULTIPLIER
        var vx = (MIN_VX + random.nextInt(MAX_VX)) * VX_MULTIPLIER
        vx = if (random.nextBoolean()) vx else -vx
        val vRotate = 0.05f

        val point = OpenGLUtil.getPointOfView(buttonLike)
        val x = point.x.toFloat()
        val y = point.y.toFloat()

        val scaleRatio = if (editTextScaleRatio.text.toString().isEmpty()) {
            0.1f
        } else {
            editTextScaleRatio.text.toString().toFloat()
        }
        val alpha = 255
        scaleRatioList.add(scaleRatio)
        alphaList.add(alpha)
        val bubble = Bubble(
                PointF(x, y - 700),
                vx,
                vy,
                vRotate,
                bitmap,
                textureFilter,
                scaleSizeRatio = scaleRatio,
                alpha = alpha
        )

        return bubble
    }

    override fun onResume() {
        super.onResume()
        anim_gl_view.onResume()
    }

    override fun onPause() {
        super.onPause()
        anim_gl_view.onPause()
    }

    companion object {

        const val VY_MULTIPLIER = 0.01f // px/ms

        const val VX_MULTIPLIER = 0.01f

        const val MIN_VY = 10

        const val MAX_VY = 30

        const val MIN_VX = 10

        const val MAX_VX = 30
    }
}