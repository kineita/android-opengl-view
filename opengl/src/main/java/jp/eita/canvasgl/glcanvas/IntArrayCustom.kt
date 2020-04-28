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

class IntArrayCustom {

    var internalArray: IntArray = IntArray(INIT_CAPACITY)
        private set

    private var mSize = 0

    fun add(value: Int) {
        if (internalArray.size == mSize) {
            val temp = IntArray(mSize + mSize)
            System.arraycopy(internalArray, 0, temp, 0, mSize)
            internalArray = temp
        }
        internalArray[mSize++] = value
    }

    fun removeLast(): Int {
        mSize--
        return internalArray[mSize]
    }

    fun size(): Int {
        return mSize
    }

    // For testing only
    fun toArray(array: IntArray?): IntArray {
        var result = array
        if (result == null || result.size < mSize) {
            result = IntArray(mSize)
        }
        System.arraycopy(internalArray, 0, result, 0, mSize)

        return result
    }

    fun clear() {
        mSize = 0
        if (internalArray.size != INIT_CAPACITY) internalArray = IntArray(INIT_CAPACITY)
    }

    companion object {
        private const val INIT_CAPACITY = 8
    }
}