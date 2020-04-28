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

import android.opengl.GLES20
import jp.eita.canvasgl.ICanvasGL
import jp.eita.canvasgl.OpenGLUtil.setFloat

class DrawCircleFilter : BasicDrawShapeFilter() {

    var lineWidth = 0f

    override fun onPreDraw(program: Int, canvas: ICanvasGL?) {
        super.onPreDraw(program, canvas)
        val lineWidthLocation = GLES20.glGetUniformLocation(program, UNIFORM_LINE_WIDTH)
        setFloat(lineWidthLocation, lineWidth)
    }

    companion object {
        const val VARYING_DRAW_REGION_COORD = "vDrawRegionCoord"

        const val VERTEXT_SHADER = (""
                + "uniform mat4 " + MATRIX_UNIFORM + ";\n"
                + "attribute vec2 " + POSITION_ATTRIBUTE + ";\n"
                + "varying vec2 " + VARYING_DRAW_REGION_COORD + ";\n"
                + "void main() {\n"
                + "  vec4 pos = vec4(" + POSITION_ATTRIBUTE + ", 0.0, 1.0);\n"
                + "  gl_Position = " + MATRIX_UNIFORM + " * pos;\n"
                + "  " + VARYING_DRAW_REGION_COORD + " = pos.xy;\n"
                + "}\n")

        const val UNIFORM_LINE_WIDTH = "lineWidth"

        const val FRAGMENT_SHADER = (""
                + "precision mediump float;\n"
                + "varying vec2 " + VARYING_DRAW_REGION_COORD + ";\n"
                + "uniform vec4 " + COLOR_UNIFORM + ";\n"
                + "uniform float " + UNIFORM_LINE_WIDTH + ";\n"
                + "void main() {\n"
                + "  float dx = " + VARYING_DRAW_REGION_COORD + ".x - 0.5;\n"
                + "  float dy = " + VARYING_DRAW_REGION_COORD + ".y - 0.5;\n"
                + "  float powVal = dx*dx + dy*dy; \n"
                + "  float subRadius = 0.5 - " + UNIFORM_LINE_WIDTH + "; \n"
                + "  if(powVal >= subRadius * subRadius && powVal <= 0.5 * 0.5) {\n"
                + "    gl_FragColor = " + COLOR_UNIFORM + ";\n"
                + "  } else {\n"
                + "    gl_FragColor = vec4(0, 0, 0, 0);\n"
                + "  }\n"
                + " \n"
                + "}\n")
    }
}