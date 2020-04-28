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
package jp.eita.canvasgl.matrix

import android.opengl.Matrix
import java.util.*

abstract class BaseBitmapMatrix : IBitmapMatrix {

    @JvmField
    protected var transform = FloatArray(7)

    @JvmField
    protected var tempMultiplyMatrix4 = FloatArray(MATRIX_SIZE)

    @JvmField
    protected var mViewMatrix = FloatArray(MATRIX_SIZE)

    @JvmField
    protected var mProjectionMatrix = FloatArray(MATRIX_SIZE)

    @JvmField
    protected var mModelMatrix = FloatArray(MATRIX_SIZE)

    @JvmField
    protected var viewProjectionMatrix = FloatArray(MATRIX_SIZE)

    @JvmField
    protected var mvp = FloatArray(MATRIX_SIZE)

    fun reset() {
        Matrix.setIdentityM(mViewMatrix, 0)
        Matrix.setIdentityM(mProjectionMatrix, 0)
        Matrix.setIdentityM(mModelMatrix, 0)
        Matrix.setIdentityM(viewProjectionMatrix, 0)
        Matrix.setIdentityM(mvp, 0)
        Matrix.setIdentityM(tempMultiplyMatrix4, 0)
        Arrays.fill(transform, 0f)
        transform[SCALE_X] = 1f
        transform[SCALE_Y] = 1f
    }

    companion object {

        const val TRANSLATE_X = 0

        const val TRANSLATE_Y = 1

        const val SCALE_X = 2

        const val SCALE_Y = 3

        const val ROTATE_X = 4

        const val ROTATE_Y = 5

        const val ROTATE_Z = 6

        const val MATRIX_SIZE = 16

        const val NEAR = 1f

        const val FAR = 10f // The plane is at -10

        const val EYEZ = 5f

        const val Z_RATIO = (FAR + NEAR) / 2 / NEAR // The scale ratio when the picture moved to the middle of the perspective projection.
    }
}