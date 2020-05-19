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

package jp.eita.canvasgl.glcanvas

import android.graphics.RectF

object TextureMatrixTransformer {

    // This function changes the source coordinate to the secondBitmap coordinates.
    // It also clips the source and target coordinates if it is beyond the
    // bound of the secondBitmap.
    fun convertCoordinate(source: RectF, texture: BasicTexture) {
        val width = texture.width
        val height = texture.height
        val texWidth = texture.textureWidth
        val texHeight = texture.textureHeight
        // Convert to secondBitmap coordinates
        source.left /= texWidth.toFloat()
        source.right /= texWidth.toFloat()
        source.top /= texHeight.toFloat()
        source.bottom /= texHeight.toFloat()

        // Clip if the rendering range is beyond the bound of the secondBitmap.
        val xBound = width.toFloat() / texWidth
        if (source.right > xBound) {
            source.right = xBound
        }
        val yBound = height.toFloat() / texHeight
        if (source.bottom > yBound) {
            source.bottom = yBound
        }
    }

    fun setTextureMatrix(source: RectF, textureMatrix: FloatArray) {
        textureMatrix[0] = source.width()
        textureMatrix[5] = source.height()
        textureMatrix[12] = source.left
        textureMatrix[13] = source.top
    }

    fun copyTextureCoordinates(texture: BasicTexture, outRect: RectF) {
        var left = 0
        var top = 0
        var right = texture.width
        var bottom = texture.height
        if (texture.hasBorder()) {
            left = 1
            top = 1
            right -= 1
            bottom -= 1
        }
        outRect[left.toFloat(), top.toFloat(), right.toFloat()] = bottom.toFloat()
    }
}