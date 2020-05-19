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
package jp.eita.example.main.bubble

import android.graphics.Bitmap
import android.graphics.PointF
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import jp.eita.canvasgl.textureFilter.*
import jp.eita.canvasgl.util.BitmapUtils
import jp.eita.canvasgl.util.OpenGLUtil
import jp.eita.example.R
import jp.eita.example.model.Bubble
import kotlinx.android.synthetic.main.activity_opengl_bubble.*
import java.util.*
import kotlin.collections.ArrayList

class BubbleActivity : AppCompatActivity() {

    private val upFilterList: MutableList<TextureFilter> = ArrayList()

    private lateinit var bitmap: Bitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_opengl_bubble)
        title = getString(R.string.bubble_title)
        setUpBitmap()
        initFilterList(upFilterList)
        setUpButtonLike()
    }

    private fun setUpBitmap() {
        bitmap = BitmapUtils.convertToBitmapFrom(applicationContext, R.drawable.ic_fast_food)!!
//        BitmapUtils.adjustOpacity(bitmap = bitmap, opacity = 120)
//        bitmap = BitmapUtils.getScaledDownBitmap(bitmap, 64, false)
//        anim_gl_view.alpha = 0.6f
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
            gl_view_bubble.onPause()
            gl_view_bubble.movableCollisionObjectList.add(createBubble(upFilterList))
            gl_view_bubble.onResume()
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

        return Bubble(
                PointF(x, y - 700),
                vx,
                vy,
                vRotate,
                100f,
                bitmap,
                textureFilter,
                scaleSizeRatio = scaleRatio,
                alpha = alpha
        )
    }

    override fun onResume() {
        super.onResume()
        gl_view_bubble.onResume()
    }

    override fun onPause() {
        super.onPause()
        gl_view_bubble.onPause()
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