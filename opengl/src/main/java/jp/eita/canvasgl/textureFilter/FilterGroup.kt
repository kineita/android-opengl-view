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
package jp.eita.canvasgl.textureFilter

import jp.eita.canvasgl.glcanvas.BasicTexture
import jp.eita.canvasgl.glcanvas.GLCanvas
import jp.eita.canvasgl.glcanvas.RawTexture
import jp.eita.canvasgl.util.Loggers
import java.util.*

open class FilterGroup(private var filters: List<TextureFilter>?) : BasicTextureFilter() {

    private val rawTextureList: MutableList<RawTexture> = ArrayList()

    protected var mergedFilters: MutableList<TextureFilter>? = null

    private var outputTexture: BasicTexture? = null

    private var initialTexture: BasicTexture? = null

    init {
        updateMergedFilters()
    }

    private fun createTextures(initialTexture: BasicTexture) {
        recycleTextures()
        for (i in mergedFilters!!.indices) {
            rawTextureList.add(RawTexture(initialTexture.width, initialTexture.height, false))
        }
    }

    private fun recycleTextures() {
        for (rawTexture in rawTextureList) {
            rawTexture.recycle()
        }
        rawTextureList.clear()
    }

    fun draw(initialTexture: BasicTexture, glCanvas: GLCanvas, onDrawListener: OnDrawListener): BasicTexture? {
        if (initialTexture is RawTexture) {
            if (!initialTexture.isNeedInvalidate) {
                return outputTexture
            }
        } else if (this.initialTexture === initialTexture && outputTexture != null) {
            return outputTexture
        }
        if (rawTextureList.size != mergedFilters!!.size || this.initialTexture !== initialTexture) {
            createTextures(initialTexture)
        }
        this.initialTexture = initialTexture
        var drawTexture: BasicTexture? = initialTexture
        var i = 0
        val size = rawTextureList.size
        while (i < size) {
            val rawTexture = rawTextureList[i]
            val textureFilter = mergedFilters!![i]
            glCanvas.beginRenderTarget(rawTexture)
            onDrawListener.onDraw(drawTexture, textureFilter, i == 0)
            glCanvas.endRenderTarget()
            drawTexture = rawTexture
            i++
        }
        outputTexture = drawTexture
        return drawTexture
    }

    override fun destroy() {
        super.destroy()
        Loggers.d(TAG, "destroy")
        recycleTextures()
    }

    fun updateMergedFilters() {
        if (filters == null) {
            return
        }
        if (mergedFilters == null) {
            mergedFilters = ArrayList()
        } else {
            mergedFilters!!.clear()
        }
        var filters: List<TextureFilter>?
        for (filter in this.filters!!) {
            if (filter is FilterGroup) {
                filter.updateMergedFilters()
                filters = filter.mergedFilters
                if (filters == null || filters.isEmpty()) continue
                mergedFilters!!.addAll(filters)
                continue
            }
            mergedFilters!!.add(filter)
        }
    }

    interface OnDrawListener {

        fun onDraw(drawTexture: BasicTexture?, textureFilter: TextureFilter?, isFirst: Boolean)
    }

    companion object {
        private const val TAG = "FilterGroup"
    }
}