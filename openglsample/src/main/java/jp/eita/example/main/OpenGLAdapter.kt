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
package jp.eita.example.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import jp.eita.example.R

class OpenGLAdapter(
        private val listParameters: List<OpenGLParameters>,
        private val onClickListener: View.OnClickListener
) : RecyclerView.Adapter<OpenGLViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OpenGLViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.research_item_view, parent, false)
        view.setOnClickListener(onClickListener)

        return OpenGLViewHolder(view)
    }

    override fun getItemCount(): Int {
        return listParameters.size
    }

    override fun onBindViewHolder(holder: OpenGLViewHolder, position: Int) {
        holder.openGLParameters = listParameters[position]
    }
}