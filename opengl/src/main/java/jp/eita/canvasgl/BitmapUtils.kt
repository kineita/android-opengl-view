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
package jp.eita.canvasgl

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.VectorDrawable
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.IntRange
import androidx.core.content.ContextCompat

object BitmapUtils {

    /**
     * This function will convert drawable ID to bitmap. It will detect if [drawableId] is [BitmapDrawable] or [VectorDrawable].
     * @param context
     * @param drawableId
     *
     * @return [Bitmap]
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun convertToBitmapFrom(context: Context, @DrawableRes drawableId: Int): Bitmap? {
        return when (val drawable = ContextCompat.getDrawable(context, drawableId)) {
            is BitmapDrawable -> return drawable.bitmap
            is VectorDrawable -> getBitmapFrom(drawable)
            else -> throw IllegalArgumentException("Unsupported drawable type")
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun getBitmapFrom(vectorDrawable: VectorDrawable): Bitmap? {
        val bitmap = Bitmap.createBitmap(vectorDrawable.intrinsicWidth,
                vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
        vectorDrawable.draw(canvas)

        return bitmap
    }

    /**
     * @param bitmap The source bitmap.
     * @param opacity a value between 0 (completely transparent) and 255 (completely
     * opaque).
     * @return The opacity-adjusted bitmap.  If the source bitmap is mutable it will be
     * adjusted and returned, otherwise a new bitmap is created.
     */
    fun adjustOpacity(bitmap: Bitmap, @IntRange(from = 0, to = 255) opacity: Int): Bitmap? {
        val mutableBitmap = if (bitmap.isMutable) bitmap else bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val colour = opacity and 0xFF shl 24
        canvas.drawColor(colour, PorterDuff.Mode.DST_IN)

        return mutableBitmap
    }

    /**
     * @param bitmap The source bitmap.
     * @param value to make transparent, > 0 will make bold or < 0 will make fuzzy.
     */
    fun makeTransparent(src: Bitmap, @IntRange(from = 1, to = 255) value: Int): Bitmap {
        val mutableBitmap: Bitmap = src.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val colour = value and 0xFF shl 24
        canvas.drawColor(colour, PorterDuff.Mode.DST_IN)

        return mutableBitmap
    }

    /**
     * @param bitmap the Bitmap to be scaled
     * @param threshold the maxium dimension (either width or height) of the scaled bitmap
     * @param isNecessaryToKeepOrig is it necessary to keep the original bitmap? If not recycle the original bitmap to prevent memory leak.
     */
    fun getScaledDownBitmap(bitmap: Bitmap, threshold: Int, isNecessaryToKeepOrig: Boolean): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        var newWidth = width
        var newHeight = height
        if (width > height && width > threshold) {
            newWidth = threshold
            newHeight = (height * newWidth.toFloat() / width).toInt()
        }
        if (width in (height + 1)..threshold) {
            //the bitmap is already smaller than our required dimension, no need to resize it
            return bitmap
        }
        if (width < height && height > threshold) {
            newHeight = threshold
            newWidth = (width * newHeight.toFloat() / height).toInt()
        }
        if (height in (width + 1)..threshold) {
            //the bitmap is already smaller than our required dimension, no need to resize it
            return bitmap
        }
        if (width == height && width > threshold) {
            newWidth = threshold
            newHeight = newWidth
        }
        return if (width == height && width <= threshold) {
            //the bitmap is already smaller than our required dimension, no need to resize it
            bitmap
        } else {
            getResizedBitmap(bitmap, newWidth, newHeight, isNecessaryToKeepOrig)
        }
    }

    private fun getResizedBitmap(bm: Bitmap, newWidth: Int, newHeight: Int, isNecessaryToKeepOrig: Boolean): Bitmap {
        val width = bm.width
        val height = bm.height
        val scaleWidth = newWidth.toFloat() / width
        val scaleHeight = newHeight.toFloat() / height
        // CREATE A MATRIX FOR THE MANIPULATION
        val matrix = Matrix()
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight)

        // "RECREATE" THE NEW BITMAP
        val resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false)
        if (!isNecessaryToKeepOrig) {
            bm.recycle()
        }

        return resizedBitmap
    }
}