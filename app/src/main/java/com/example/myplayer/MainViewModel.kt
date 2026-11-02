package com.example.myplayer

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("prefs", 0)

    var playlistUrl by mutableStateOf(prefs.getString("url", "") ?: "")
    var channels by mutableStateOf<List<Channel>>(emptyList())
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var query by mutableStateOf("")
    var selectedGroup by mutableStateOf<String?>(null)

    val groups get() = channels.map { it.group }.distinct()
    val filtered get() = channels.filter {
        (selectedGroup == null || it.group == selectedGroup) &&
            (query.isBlank() || it.name.contains(query, ignoreCase = true))
    }

    init { if (playlistUrl.isNotBlank()) load() }

    fun load() {
        val url = playlistUrl.trim()
        if (url.isEmpty()) return
        prefs.edit().putString("url", url).apply()
        loading = true; error = null
        viewModelScope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    val c = URL(url).openConnection() as HttpURLConnection
                    c.connectTimeout = 15000; c.readTimeout = 30000
                    c.inputStream.bufferedReader().use { it.readText() }
                }
                channels = M3uParser.parse(text)
                selectedGroup = null
                if (channels.isEmpty()) error = "القائمة فاضية أو الصيغة غير مدعومة"
            } catch (e: Exception) {
                error = "تعذّر التحميل: ${e.message}"
            } finally { loading = false }
        }
    }
}
