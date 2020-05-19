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
import jp.eita.canvasgl.pathManager.PathManagerConfig.AlphaConfig
import jp.eita.canvasgl.pathManager.PathManagerConfig.ScaleSizeConfig
import jp.eita.canvasgl.pathManager.bezier.BezierPathManager
import jp.eita.canvasgl.pathManager.bezier.BezierPathManagerConfig
import jp.eita.canvasgl.textureFilter.BasicTextureFilter
import jp.eita.canvasgl.textureFilter.TextureFilter
import jp.eita.canvasgl.util.BitmapUtils
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

    private val pathManager = BezierPathManager().apply {
        config.apply {
            point = BezierPathManagerConfig.PointConfig(3)
                    .also { it.size = 60 }
            scaleSize = BezierPathManagerConfig.ScaleSizeConfig()
                    .apply {
                        addLevel(ScaleSizeConfig.Level(0, 1f))
                        addLevel(ScaleSizeConfig.Level(20, 1.5f))
                        addLevel(ScaleSizeConfig.Level(40, 1.8f))
                        addLevel(ScaleSizeConfig.Level(60, 0.8f))
                    }
                    .also { it.size = 60 }
            alpha = BezierPathManagerConfig.AlphaConfig()
                    .apply {
                        addLevel(AlphaConfig.Level(0, 60))
                        addLevel(AlphaConfig.Level(60, 255))
                    }
                    .also { it.size = 60 }
        }
    }

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
    }

    private fun setUpEditTextScaleRatio() {
        editTextScaleRatio.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable?) {
//                try {
//                    val value: Double = if (s == null || s.toString() == "") {
//                        return
//                    } else if (s.toString() == "0.") {
//                        0.1
//                    } else {
//                        s.toString().toDouble()
//                    }
//                    gl_view_reaction.onPause()
////                    anim_gl_view.updateScaleRatioForAllBubbles(value.toFloat())
//                    gl_view_reaction.onResume()
//                } catch (ex: Exception) {
//
//                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

        })
    }

    private fun initFilterList(filterList: MutableList<TextureFilter>) {
        filterList.add(BasicTextureFilter())
    }

    private fun setUpButtonLike() {
        buttonLike.setOnClickListener {
            gl_view_reaction.onPause()
            val reaction = createReaction(upFilterList)
//            AnimatorUtils.animateScaleSize(reaction, if (editTextScaleRatio.text.toString().isEmpty()) {
//                1.6f
//            } else {
//                editTextScaleRatio.text.toString().toFloat()
//            }, DEFAULT_TIME_DELAY_ANIMATE)
//            AnimatorUtils.animateAlpha(reaction, 255, DEFAULT_TIME_DELAY_ANIMATE)
            gl_view_reaction.addReaction(reaction)
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
            1.0f
        } else {
            editTextScaleRatio.text.toString().toFloat()
        }
        val alpha = 255
        val point = PointF(x, y)
        if (random.nextInt() % 2 == 0) {
            pathManager.config.point!![0] = point
            pathManager.config.point!![1] = PointF(point.x - 200, 500f)
            pathManager.config.point!![2] = PointF(point.x, -bitmap.height - 50f)
        } else {
            pathManager.config.point!![0] = point
            pathManager.config.point!![1] = PointF(point.x + 200, 500f)
            pathManager.config.point!![2] = PointF(point.x, -bitmap.height - 50f)
        }

        return Reaction(
                point,
                vx,
                vy,
                vRotate,
                -1f,
                bitmap = bitmap,
                textureFilter = textureFilter,
                scaleSizeRatio = scaleRatio,
                alpha = alpha,
                pathManager = pathManager
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