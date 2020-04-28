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
package jp.eita.example.structure

class AlphaList(initialCapacity: Int) : ArrayList<Int>(initialCapacity) {

    val listDetails: MutableList<Detail> = ArrayList()

    override fun add(element: Int): Boolean {
        listDetails.add(Detail(element))
        return super.add(element)
    }

    class Detail(value: Int) {

        private var rangeAlpha = intArrayOf(
                value,
                (value * 0.8).toInt(),
                (value * 0.7).toInt(),
                (value * 0.4).toInt(),
                (value * 0.2).toInt(),
                (value * 0.4).toInt(),
                (value * 0.6).toInt(),
                (value * 0.7).toInt(),
                (value * 0.8).toInt(),
                value
        )

        private var crawlerAlpha: Int = 0

        val scaleRatioValue: Int
            get() {
                when {
                    crawlerAlpha >= rangeAlpha.size - 1 -> {
                        crawlerAlpha = 0
                    }
                    else -> {
                        crawlerAlpha += 1
                    }
                }

                return rangeAlpha[crawlerAlpha]
            }
    }
}