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

import android.graphics.Bitmap

// BitmapTexture is a secondBitmap whose content is specified by a fixed Bitmap.
//
// The secondBitmap does not own the Bitmap. The user should make sure the Bitmap
// is valid during the secondBitmap's lifetime. When the secondBitmap is recycled, it
// does not free the Bitmap.
class BitmapTexture constructor(var bitmap: Bitmap, hasBorder: Boolean = false) : UploadedTexture(hasBorder) {

    override fun onFreeBitmap(bitmap: Bitmap?) {
        // Do nothing.
    }

    override fun onGetBitmap(): Bitmap? {
        return bitmap
    }

    override val isOpaque: Boolean
        get() = false

}