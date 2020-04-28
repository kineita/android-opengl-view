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
package jp.eita.canvasgl.shapeFilter

import jp.eita.canvasgl.ICanvasGL
import jp.eita.canvasgl.glcanvas.GLES20Canvas

open class BasicDrawShapeFilter : DrawShapeFilter {

    override val vertexShader = DRAW_VERTEX_SHADER

    override val fragmentShader = DRAW_FRAGMENT_SHADER

    override fun onPreDraw(program: Int, canvas: ICanvasGL?) {}

    override fun destroy() {}

    companion object {

        const val MATRIX_UNIFORM = GLES20Canvas.MATRIX_UNIFORM

        const val POSITION_ATTRIBUTE = GLES20Canvas.POSITION_ATTRIBUTE

        const val COLOR_UNIFORM = GLES20Canvas.COLOR_UNIFORM

        const val DRAW_FRAGMENT_SHADER = (""
                + "precision mediump float;\n"
                + "uniform vec4 " + COLOR_UNIFORM + ";\n"
                + "void main() {\n"
                + "  gl_FragColor = " + COLOR_UNIFORM + ";\n"
                + "}\n")

        const val DRAW_VERTEX_SHADER = (""
                + "uniform mat4 " + MATRIX_UNIFORM + ";\n"
                + "attribute vec2 " + POSITION_ATTRIBUTE + ";\n"
                + "void main() {\n"
                + "  vec4 pos = vec4(" + POSITION_ATTRIBUTE + ", 0.0, 1.0);\n"
                + "  gl_Position = " + MATRIX_UNIFORM + " * pos;\n"
                + "}\n")
    }
}