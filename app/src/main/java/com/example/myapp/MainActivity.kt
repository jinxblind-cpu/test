package com.example.myapp

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private lateinit var chatAdapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var recyclerView: RecyclerView
    private lateinit var inputField: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var settingsButton: ImageButton

    private val PREFS_NAME = "JinxdPrefs"
    private val KEY_API_KEY = "api_key"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) 
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.chatRecyclerView)
        inputField = findViewById(R.id.inputField)
        sendButton = findViewById(R.id.sendButton)
        settingsButton = findViewById(R.id.settingsButton)

        chatAdapter = ChatAdapter(messages)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = chatAdapter

        sendButton.setOnClickListener {
            sendMessage()
        }

        settingsButton.setOnClickListener {
            showApiKeyDialog()
        }

        if (getApiKey().isNullOrEmpty()) {
            showApiKeyDialog()
        } else {
            addMessage("System", "The seal is broken. Jinxd AI awaits your curse.", false)
        }
    }

    private fun getApiKey(): String? {
        return getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_API_KEY, "")
    }

    private fun saveApiKey(key: String) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_API_KEY, key).apply()
    }

    private fun showApiKeyDialog() {
        val input = EditText(this)
        input.hint = "Enter OpenRouter API Key"
        input.setText(getApiKey())
        input.setTextColor(Color.WHITE)
        input.setHintTextColor(Color.GRAY)

        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Invoke Permissions")
            .setMessage("Feed me your OpenRouter API key to awaken the spirit.")
            .setView(input)
            .setPositiveButton("Bind") { _, _ ->
                saveApiKey(input.text.toString())
                Toast.makeText(this, "Key bound to the shadows.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Flee", null)
            .show()
    }

    private fun sendMessage() {
        val text = inputField.text.toString().trim()
        val apiKey = getApiKey()

        if (apiKey.isNullOrEmpty()) {
            showApiKeyDialog()
            return
        }

        if (text.isEmpty()) return

        addMessage("You", text, true)
        inputField.setText("")

        lifecycleScope.launch {
            val response = callOpenRouter(text)
            addMessage("Jinxd", response, false)
        }
    }

    private fun addMessage(sender: String, text: String, isUser: Boolean) {
        messages.add(ChatMessage(sender, text, isUser))
        chatAdapter.notifyItemInserted(messages.size - 1)
        recyclerView.scrollToPosition(messages.size - 1)
    }

    private suspend fun callOpenRouter(userText: String): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey() ?: ""
        try {
            val json = JSONObject().apply {
                put("model", "meta-llama/llama-3.1-8b-instruct:free")
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", "You are Jinxd AI. You are a cursed, cryptic, and slightly unsettling chatbot. Your responses should be helpful but delivered with a dark, magical, and mysterious tone. Use gothic or occult metaphors.")
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userText)
                    })
                })
            }

            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://openrouter.ai/api/v1/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("HTTP-Referer", "http://localhost") 
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext "The void rejects your request: ${response.code}"
                val responseBody = response.body?.string() ?: return@withContext "The spirits are silent."
                val jsonResponse = JSONObject(responseBody)
                val choices = jsonResponse.getJSONArray("choices")
                if (choices.length() > 0) {
                    choices.getJSONObject(0).getJSONObject("message").getString("content")
                } else {
                    "The prophecy is empty."
                }
            }
        } catch (e: Exception) {
            "A ritual error occurred: ${e.localizedMessage}"
        }
    }

    data class ChatMessage(val sender: String, val text: String, val isUser: Boolean)

    class ChatAdapter(private val chatMessages: List<ChatMessage>) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

        class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val textContainer: LinearLayout = view.findViewById(R.id.textContainer)
            val senderText: TextView = view.findViewById(R.id.senderText)
            val bodyText: TextView = view.findViewById(R.id.bodyText)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message, parent, false)
            return ChatViewHolder(view)
        }

        override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
            val msg = chatMessages[position]
            holder.senderText.text = msg.sender
            holder.bodyText.text = msg.text

            if (msg.isUser) {
                holder.textContainer.setBackgroundResource(R.drawable.bg_user_bubble)
                holder.senderText.setTextColor(Color.parseColor("#FF3333")) // Cursed Red
            } else {
                holder.textContainer.setBackgroundResource(R.drawable.bg_ai_bubble)
                holder.senderText.setTextColor(Color.parseColor("#BB86FC")) // Ghostly Purple
            }
        }

        override fun getItemCount() = chatMessages.size
    }
}