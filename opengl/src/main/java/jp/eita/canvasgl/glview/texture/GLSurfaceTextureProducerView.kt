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
package jp.eita.canvasgl.glview.texture

import android.content.Context
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import jp.eita.canvasgl.ICanvasGL
import jp.eita.canvasgl.glcanvas.BasicTexture
import jp.eita.canvasgl.glcanvas.RawTexture
import jp.eita.canvasgl.glview.texture.gles.EglContextWrapper

/**
 * This will generate a texture which is in the eglContext of the CanvasGL. And the texture can be used outside.
 * The [.setSharedEglContext] will be called automatically when [.onSurfaceTextureAvailable]
 * For example, the generated texture can be used in camera preview texture or [GLMultiTexConsumerView].
 *
 * From pause to run: onResume --> createSurface --> onSurfaceChanged
 */
abstract class GLSurfaceTextureProducerView : GLMultiTexProducerView {

    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun getInitialTexCount(): Int {
        return 1
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        super.onSurfaceTextureAvailable(surface, width, height)
        if (mSharedEglContext == null) {
            setSharedEglContext(EglContextWrapper.EGL_NO_CONTEXT_WRAPPER)
        }
    }

    fun setOnSurfaceTextureSet(onSurfaceTextureSet: OnSurfaceTextureSet) {
        setSurfaceTextureCreatedListener(object : SurfaceTextureCreatedListener {
            override fun onCreated(producedTextureList: List<GLTexture>) {
                val glTexture = producedTextureList[0]
                onSurfaceTextureSet.onSet(glTexture.surfaceTexture, glTexture.rawTexture)
            }

        })
    }

    override fun onGLDraw(canvas: ICanvasGL?, producedTextures: List<GLTexture>?, consumedTextures: List<GLTexture>?) {
        val glTexture = producedTextures!![0]
        if (consumedTextures!!.isNotEmpty()) {
            val consumeTexture = consumedTextures[0]
            onGLDraw(canvas, glTexture.surfaceTexture, glTexture.rawTexture, consumeTexture.surfaceTexture, consumeTexture.rawTexture)
            onGLDraw(canvas, glTexture, consumeTexture)
        } else {
            onGLDraw(canvas, glTexture.surfaceTexture, glTexture.rawTexture, null, null)
            onGLDraw(canvas, glTexture, null)
        }
    }

    @Deprecated("")
    protected fun onGLDraw(canvas: ICanvasGL?, producedSurfaceTexture: SurfaceTexture?, producedRawTexture: RawTexture?, outsideSurfaceTexture: SurfaceTexture?, outsideTexture: BasicTexture?) {
    }

    protected fun onGLDraw(canvas: ICanvasGL?, producedGLTexture: GLTexture?, outsideGLTexture: GLTexture?) {}
    interface OnSurfaceTextureSet {
        fun onSet(surfaceTexture: SurfaceTexture?, surfaceTextureRelatedTexture: RawTexture?)
    }
}