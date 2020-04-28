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
package jp.eita.canvasgl.util

import android.util.Log

object Loggers {
    var DEBUG = true

    fun d(tag: String?, msg: String) {
        if (DEBUG) {
            Log.d(tag, msg)
        }
    }

    fun v(tag: String?, msg: String) {
        if (DEBUG) {
            Log.v(tag, msg)
        }
    }

    fun i(tag: String?, msg: String) {
        if (DEBUG) {
            Log.i(tag, msg)
        }
    }

    fun w(tag: String?, msg: String) {
        if (DEBUG) {
            Log.w(tag, msg)
        }
    }

    fun e(tag: String?, msg: String) {
        if (DEBUG) {
            Log.e(tag, msg)
        }
    }
}