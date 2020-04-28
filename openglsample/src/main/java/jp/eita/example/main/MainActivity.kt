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

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import jp.eita.example.main.OpenGLName.OPENGL
import jp.eita.example.R
import jp.eita.example.bubble.OpenGLActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var openGLAdapter: OpenGLAdapter

    private val onClickListener: View.OnClickListener = View.OnClickListener {
        val intent: Intent
        when (it.tag) {
            OPENGL -> {
                intent = Intent(this, OpenGLActivity::class.java)
            }
            else -> {
                return@OnClickListener
            }
        }
        startActivity(intent)
    }

    private val listParameters: List<OpenGLParameters> = listOf(
            OpenGLParameters(OPENGL)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        openGLAdapter = OpenGLAdapter(listParameters, onClickListener)
        recycler_view.adapter = openGLAdapter
        recycler_view.layoutManager = LinearLayoutManager(this)
    }
}