package com.kia.blagogramapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kia.blagogramapp.adapters.SearchUsersAdapter
import com.kia.blagogramapp.api.ApiRepository
import com.kia.blagogramapp.models.SearchUserItem
import com.kia.blagogramapp.network.RetrofitClient
import kotlinx.coroutines.*

class SearchUserActivity : AppCompatActivity() {

    private val repo = ApiRepository(RetrofitClient.api)
    private var searchJob: Job? = null

    private lateinit var input: EditText
    private lateinit var recycler: RecyclerView
    private lateinit var progress: ProgressBar
    private lateinit var backBtn: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_user)

        input = findViewById(R.id.searchInput)
        recycler = findViewById(R.id.recyclerSearch)
        progress = findViewById(R.id.searchProgress)
        backBtn = findViewById(R.id.backBtn)

        recycler.layoutManager = LinearLayoutManager(this)
        backBtn.setOnClickListener { finish() }

        input.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(t: CharSequence?, start: Int, before: Int, count: Int) {
                val q = t.toString().trim()
                progress.visibility = View.VISIBLE

                if (q.isNotEmpty()) search(q)
                else recycler.adapter = SearchUsersAdapter(emptyList()) {}
            }
        })
    }

    private fun search(query: String) {
        val token = getSharedPreferences("app", MODE_PRIVATE)
            .getString("token", "") ?: ""

        searchJob?.cancel()

        searchJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val users: List<SearchUserItem> = repo.searchUsers(token, query)

                withContext(Dispatchers.Main) {
                    progress.visibility = View.GONE
                    recycler.adapter = SearchUsersAdapter(users) { user ->
                        createChat(user)
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progress.visibility = View.GONE
                    recycler.adapter = SearchUsersAdapter(emptyList()) {}
                }
            }
        }
    }

    private fun createChat(user: SearchUserItem) {
        val token = getSharedPreferences("app", MODE_PRIVATE)
            .getString("token", "") ?: ""

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val chatId = repo.createChat(token, user.id)

                val i = Intent(this@SearchUserActivity, ChatActivity::class.java)
                i.putExtra("chat_id", chatId)
                i.putExtra("chat_name", user.display_name)
                i.putExtra("chat_avatar", user.avatar)
                i.putExtra("target_user_id", user.id) // Передаем ID пользователя явно
                startActivity(i)
                finish()

            } catch (_: Exception) {}
        }
    }
}
