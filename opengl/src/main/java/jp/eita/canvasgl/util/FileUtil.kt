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

import android.text.TextUtils
import java.io.File
import java.io.FileWriter

object FileUtil {

    @JvmStatic
    @Synchronized
    fun createFile(path: String): File? {
        if (TextUtils.isEmpty(path)) {
            return null
        }
        val file = File(path)
        if (file.isFile) {
            return file
        }
        val parentFile = file.parentFile
        if (parentFile != null && (parentFile.isDirectory || parentFile.mkdirs())) {
            try {
                if (file.createNewFile()) {
                    return file
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return null
    }

    @JvmStatic
    @Synchronized
    fun delete(path: File?): Boolean {
        if (null == path) {
            return true
        }
        if (path.isDirectory) {
            val files = path.listFiles()
            if (null != files) {
                for (file in files) {
                    if (!delete(file)) {
                        return false
                    }
                }
            }
        }

        return !path.exists() || path.delete()
    }

    @JvmStatic
    fun writeToFile(content: String, filePath: String) {
        var fileWriter: FileWriter? = null
        try {
            fileWriter = FileWriter(filePath, true)
            fileWriter.write(content)
            fileWriter.flush()
        } catch (t: Throwable) {
            t.printStackTrace()
        } finally {
            if (fileWriter != null) {
                try {
                    fileWriter.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}