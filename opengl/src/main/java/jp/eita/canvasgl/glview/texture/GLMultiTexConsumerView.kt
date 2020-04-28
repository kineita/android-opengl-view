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
import android.util.AttributeSet
import jp.eita.canvasgl.ICanvasGL
import jp.eita.canvasgl.glview.texture.gles.EglContextWrapper
import java.util.*

/**
 * This class is used to accept eglContext and textures from outside. Then it can use them to draw.
 * The [.setSharedEglContext] must be called as the precondition to consume outside texture.
 */
abstract class GLMultiTexConsumerView : BaseGLCanvasTextureView {

    protected var consumedTextures: MutableList<GLTexture> = ArrayList()

    protected var mSharedEglContext: EglContextWrapper? = null

    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    /**
     * @param sharedEglContext The openGL context from other or [EglContextWrapper.EGL_NO_CONTEXT_WRAPPER]
     */
    fun setSharedEglContext(sharedEglContext: EglContextWrapper?) {
        mSharedEglContext = sharedEglContext
        glThreadBuilder!!.setSharedEglContext(sharedEglContext!!)
        createGLThread()
    }

    override fun createGLThread() {
        if (mSharedEglContext != null) {
            super.createGLThread()
        }
    }

    /**
     * This must be called for a GLMultiTexConsumerView.
     *
     * @param glTexture texture from outSide.
     */
    fun addConsumeGLTexture(glTexture: GLTexture) {
        consumedTextures.add(glTexture)
    }

    /**
     * Will not call until @param surfaceTexture not null
     */
    protected abstract fun onGLDraw(canvas: ICanvasGL?, consumedTextures: List<GLTexture>?)
    override fun onGLDraw(canvas: ICanvasGL?) {
        val iterator = consumedTextures.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            if (next.rawTexture.isRecycled) {
                iterator.remove()
            }
        }
        onGLDraw(canvas, consumedTextures)
    }

    override fun surfaceDestroyed() {
        super.surfaceDestroyed()
        consumedTextures.clear()
    }
}