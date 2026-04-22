package Com.jinxdai.app

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
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

    private lateinit var chatContainer: LinearLayout
    private lateinit var scrollView: ScrollView
    private lateinit var inputField: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var settingsButton: ImageButton

    private val PREFS_NAME = "JinxDPrefs"
    private val KEY_API = "openrouter_api_key"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) 
        setContentView(R.layout.activity_main)

        chatContainer = findViewById(R.id.chatContainer)
        scrollView = findViewById(R.id.scrollView)
        inputField = findViewById(R.id.inputField)
        sendButton = findViewById(R.id.sendButton)
        settingsButton = findViewById(R.id.settingsButton)

        settingsButton.setOnClickListener { showApiKeyDialog() }

        sendButton.setOnClickListener {
            val message = inputField.text.toString().trim()
            val apiKey = getApiKey()

            if (apiKey.isNullOrBlank()) {
                addMessage("SYSTEM: FEED ME THE API KEY FIRST...", false)
                showApiKeyDialog()
                return@setOnClickListener
            }

            if (message.isNotEmpty()) {
                addMessage("YOU: $message", true)
                inputField.setText("")
                performChat(message, apiKey)
            }
        }

        addMessage("JINX: THE VOID IS LISTENING...", false)
    }

    private fun getApiKey(): String? = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_API, null)

    private fun showApiKeyDialog() {
        val input = EditText(this)
        input.hint = "OpenRouter API Key"
        input.setText(getApiKey())
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("CONFIGURE THE BEAST")
            .setMessage("Paste your OpenRouter API Key to grant me vision.")
            .setView(input)
            .setPositiveButton("BIND") { _, _ ->
                val key = input.text.toString().trim()
                getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_API, key).apply()
            }
            .setNegativeButton("ESCAPE", null)
            .show()
    }

    private fun addMessage(text: String, isUser: Boolean) {
        val textView = TextView(this)
        textView.text = text
        textView.setTextColor(if (isUser) 0xFF39FF14.toInt() else 0xFFFF0033.toInt())
        textView.typeface = Typeface.MONOSPACE
        textView.textSize = 16f
        textView.setPadding(16, 8, 16, 8)
        
        val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.setMargins(0, 4, 0, 4)
        textView.layoutParams = params

        chatContainer.addView(textView)
        scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
    }

    private fun performChat(message: String, apiKey: String) {
        val thinkingView = TextView(this).apply {
            text = "JINX: COMMUNICING WITH CHAOS..."
            setTextColor(0xFF888888.toInt())
            typeface = Typeface.MONOSPACE
            textSize = 14f
        }
        chatContainer.addView(thinkingView)

        lifecycleScope.launch(Dispatchers.IO) {
            val response = callOpenRouter(message, apiKey)
            withContext(Dispatchers.Main) {
                chatContainer.removeView(thinkingView)
                addMessage("JINX: $response", false)
            }
        }
    }

    private fun callOpenRouter(message: String, apiKey: String): String {
        return try {
            val json = JSONObject().apply {
                put("model", "google/gemini-2.0-flash-lite-preview-02-05:free")
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", message)
                    })
                })
            }

            val request = Request.Builder()
                .url("https://openrouter.ai/api/v1/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("HTTP-Referer", "http://jinx.ai")
                .addHeader("X-Title", "JinxD ai")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return "THE VOID REJECTED YOU: ${response.code}"
                val body = response.body?.string() ?: return "SILENCE FROM THE VOID"
                val jsonResp = JSONObject(body)
                jsonResp.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
            }
        } catch (e: Exception) {
            "THE VOID IS CORRUPT: ${e.message}"
        }
    }
}