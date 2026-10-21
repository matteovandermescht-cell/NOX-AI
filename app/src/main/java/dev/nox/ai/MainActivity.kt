// language: Kotlin, file: MainActivity.kt, runtime: Android 8+, target: NOX AI APK
package dev.nox.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
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

private val NoxBackground = Color(0xFF101114)
private val NoxSurface = Color(0xFF191B20)
private val NoxAccent = Color(0xFFB7C8FF)
private val NoxText = Color(0xFFE6E7EB)
private val NoxMuted = Color(0xFF9A9DA7)

data class ChatMessage(val role: String, val content: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NoxApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoxApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("nox_settings", android.content.Context.MODE_PRIVATE) }
    var endpoint by remember { mutableStateOf(prefs.getString("endpoint", "https://api.openai.com/v1/chat/completions") ?: "") }
    var apiKey by remember { mutableStateOf(prefs.getString("api_key", "") ?: "") }
    var model by remember { mutableStateOf(prefs.getString("model", "gpt-4o-mini") ?: "") }
    var showSettings by remember { mutableStateOf(apiKey.isBlank()) }
    var draft by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val client = remember { OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).readTimeout(120, TimeUnit.SECONDS).callTimeout(3, TimeUnit.MINUTES).build() }

    fun submitMessage() {
        val text = draft.trim()
        if (text.isEmpty() || busy) return
        if (apiKey.isBlank()) {
            showSettings = true
            error = "Add your API key in Settings to start chatting."
            return
        }
        if (!endpoint.startsWith("https://")) {
            error = "Use an HTTPS API endpoint."
            return
        }
        draft = ""
        error = ""
        messages.add(ChatMessage("user", text))
        busy = true
        scope.launch {
            try {
                val requestMessages = JSONArray().put(
                    JSONObject().put("role", "system").put(
                        "content",
                        "You are NOX AI, a clear, capable, concise assistant. Help with writing, coding, explanations, and problem solving. Be honest about uncertainty and capabilities."
                    )
                )
                messages.forEach { message ->
                    requestMessages.put(JSONObject().put("role", message.role).put("content", message.content))
                }
                val payload = JSONObject().put("model", model.trim()).put("messages", requestMessages)
                val answer = withContext(Dispatchers.IO) {
                    val request = Request.Builder()
                        .url(endpoint.trim())
                        .header("Authorization", "Bearer ${apiKey.trim()}")
                        .header("Content-Type", "application/json")
                        .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                        .build()
                    client.newCall(request).execute().use { response ->
                        val body = response.body?.string().orEmpty()
                        if (!response.isSuccessful) {
                            val detail = runCatching { JSONObject(body).optString("error", body) }.getOrDefault(body)
                            throw IllegalStateException("API error ${response.code}: ${detail.take(300)}")
                        }
                        val choices = JSONObject(body).optJSONArray("choices")
                            ?: throw IllegalStateException("The API response did not contain choices.")
                        choices.optJSONObject(0)?.optJSONObject("message")?.optString("content")
                            ?.takeIf { it.isNotBlank() }
                            ?: throw IllegalStateException("The API returned an empty response.")
                    }
                }
                messages.add(ChatMessage("assistant", answer))
            } catch (e: Exception) {
                error = e.message?.take(400) ?: "Request failed. Check your connection and API settings."
            } finally {
                busy = false
            }
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = NoxAccent,
            onPrimary = Color(0xFF17203A),
            background = NoxBackground,
            surface = NoxSurface,
            onBackground = NoxText,
            onSurface = NoxText
        )
    ) {
        Scaffold(
            containerColor = NoxBackground,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("NOX AI", fontWeight = FontWeight.Bold, fontSize = 19.sp)
                            Text("Your AI workspace", fontSize = 11.sp, color = NoxMuted)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { if (!busy) { messages.clear(); error = "" } }) {
                            Icon(Icons.Default.Add, contentDescription = "New chat", tint = NoxText)
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = NoxText)
                        }
                    }
                )
            },
            bottomBar = {
                Column(Modifier.fillMaxWidth().background(NoxSurface).padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = draft,
                            onValueChange = { draft = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Message NOX AI…", color = NoxMuted) },
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 5,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { submitMessage() })
                        )
                        Surface(shape = CircleShape, color = NoxAccent, modifier = Modifier.size(48.dp)) {
                            IconButton(onClick = { submitMessage() }, enabled = !busy) {
                                Icon(Icons.Default.Send, contentDescription = "Send message", tint = Color(0xFF17203A))
                            }
                        }
                    }
                    Text("AI can make mistakes. Check important information.", color = NoxMuted, fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 7.dp))
                }
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                if (error.isNotBlank()) {
                    Surface(color = Color(0xFF382329), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(error, color = Color(0xFFFFC7CC), modifier = Modifier.padding(12.dp), fontSize = 13.sp)
                    }
                }
                if (messages.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(shape = RoundedCornerShape(24.dp), color = NoxSurface, modifier = Modifier.size(72.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("N", color = NoxAccent, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(Modifier.height(20.dp))
                            Text("What can I help with?", color = NoxText, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))
                            Text("Ask a question, build something, or explore an idea.", color = NoxMuted, fontSize = 14.sp)
                            Spacer(Modifier.height(20.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SuggestionChip("Write", { draft = "Help me write " })
                                SuggestionChip("Code", { draft = "Help me build " })
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(messages) { message ->
                            val isUser = message.role == "user"
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
                                Surface(
                                    color = if (isUser) Color(0xFF252A35) else NoxBackground,
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier.fillMaxWidth(if (isUser) 0.88f else 1f)
                                ) {
                                    Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                                        Text(if (isUser) "You" else "NOX AI", fontSize = 11.sp, color = if (isUser) NoxAccent else NoxMuted, fontWeight = FontWeight.SemiBold)
                                        Spacer(Modifier.height(5.dp))
                                        Text(message.content, color = NoxText, fontSize = 15.sp, lineHeight = 22.sp)
                                    }
                                }
                            }
                        }
                        if (busy) {
                            item {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = NoxAccent)
                                    Text("NOX AI is thinking…", color = NoxMuted, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
        LaunchedEffect(messages.size, busy) {
            if (messages.isNotEmpty()) listState.animateScrollToItem(if (busy) messages.size else messages.lastIndex)
        }
        if (showSettings) {
            AlertDialog(
                onDismissRequest = { showSettings = false },
                title = { Text("AI settings", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Connect an OpenAI-compatible Chat Completions API.", color = NoxMuted, fontSize = 13.sp)
                        OutlinedTextField(value = endpoint, onValueChange = { endpoint = it }, label = { Text("HTTPS API endpoint") }, singleLine = true)
                        OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("Model") }, singleLine = true)
                        OutlinedTextField(value = apiKey, onValueChange = { apiKey = it }, label = { Text("API key") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        prefs.edit().putString("endpoint", endpoint.trim()).putString("model", model.trim()).putString("api_key", apiKey.trim()).apply()
                        showSettings = false
                        error = ""
                    }) { Text("Save settings") }
                },
                dismissButton = { TextButton(onClick = { showSettings = false }) { Text("Cancel") } }
            )
        }
    }
}

@Composable
private fun SuggestionChip(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = NoxSurface,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30333C))
    ) {
        Text(label, color = NoxText, modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp), fontSize = 13.sp)
    }
}
