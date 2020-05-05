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
package jp.eita.example.main.reaction

import android.graphics.Bitmap
import android.graphics.PointF
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import jp.eita.canvasgl.BitmapUtils
import jp.eita.canvasgl.textureFilter.*
import jp.eita.example.AnimatorUtils
import jp.eita.example.R
import jp.eita.example.model.Reaction
import kotlinx.android.synthetic.main.activity_opengl_bubble.buttonLike
import kotlinx.android.synthetic.main.activity_opengl_bubble.editTextScaleRatio
import kotlinx.android.synthetic.main.activity_opengl_reaction.*
import java.util.*
import kotlin.collections.ArrayList

class ReactionActivity : AppCompatActivity() {

    private val upFilterList: MutableList<TextureFilter> = ArrayList()

    private lateinit var bitmap: Bitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_opengl_reaction)
        title = getString(R.string.reaction_title)
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
                    gl_view_reaction.onPause()
//                    anim_gl_view.updateScaleRatioForAllBubbles(value.toFloat())
                    gl_view_reaction.onResume()
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
        filterList.add(ContrastFilter(1.6f))
        filterList.add(SaturationFilter(1.6f))
        filterList.add(PixelationFilter(12F))
        filterList.add(HueFilter(100F))
    }

    private fun setUpButtonLike() {
        buttonLike.setOnClickListener {
            gl_view_reaction.onPause()
            val reaction = createReaction(upFilterList)
            AnimatorUtils.animateScaleSize(reaction, if (editTextScaleRatio.text.toString().isEmpty()) {
                0.1f
            } else {
                editTextScaleRatio.text.toString().toFloat()
            }, DEFAULT_TIME_DELAY_ANIMATE)
            AnimatorUtils.animateAlpha(reaction, 255, DEFAULT_TIME_DELAY_ANIMATE)
            gl_view_reaction.reactionList.add(reaction)
            gl_view_reaction.onResume()
        }
    }

    private fun createReaction(filterList: List<TextureFilter>): Reaction {
        val random = Random()
        val textureFilter = filterList[random.nextInt(filterList.size)]
        val vy = -(MIN_VY + random.nextInt(MAX_VY)) * VY_MULTIPLIER
        var vx = (MIN_VX + random.nextInt(MAX_VX)) * VX_MULTIPLIER
        vx = if (random.nextBoolean()) vx else -vx
        val vRotate = 0.05f

        val x: Float = (gl_view_reaction.width / 2).toFloat()
        val y: Float = (gl_view_reaction.height - 100).toFloat()

        val scaleRatio = if (editTextScaleRatio.text.toString().isEmpty()) {
            0.1f
        } else {
            editTextScaleRatio.text.toString().toFloat()
        }
        val alpha = 255

        return Reaction(
                PointF(x, y),
                vx,
                vy,
                vRotate,
                -1f,
                bitmap = bitmap,
                textureFilter = textureFilter,
                scaleSizeRatio = scaleRatio,
                alpha = alpha
        )
    }

    override fun onResume() {
        super.onResume()
        gl_view_reaction.onResume()
    }

    override fun onPause() {
        super.onPause()
        gl_view_reaction.onPause()
    }

    companion object {

        const val VY_MULTIPLIER = 0.01f // px/ms

        const val VX_MULTIPLIER = 0.01f

        const val MIN_VY = 10

        const val MAX_VY = 30

        const val MIN_VX = 10

        const val MAX_VX = 30

        const val DEFAULT_TIME_DELAY_ANIMATE: Long = 150
    }
}