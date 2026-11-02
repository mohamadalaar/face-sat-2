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
    private val attr = Regex("""([\w-]+)="([^"]*)"""")

    var playlistUrl by mutableStateOf(prefs.getString("url", "") ?: "")
    var channels by mutableStateOf<List<Channel>>(emptyList())
    var groups by mutableStateOf<List<String>>(emptyList())
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var query by mutableStateOf("")
    var selectedGroup by mutableStateOf<String?>(null)

    val filtered get() = channels.filter {
        (selectedGroup == null || it.group == selectedGroup) &&
            (query.isBlank() || it.name.contains(query, ignoreCase = true))
    }

    init { if (playlistUrl.isNotBlank()) load() }

    private fun parseStream(lines: Sequence<String>): List<Channel> {
        val out = ArrayList<Channel>()
        var name = ""; var logo: String? = null; var group = "عام"
        for (raw in lines) {
            val line = raw.trim()
            when {
                line.startsWith("#EXTINF") -> {
                    val a = attr.findAll(line).associate { it.groupValues[1].lowercase() to it.groupValues[2] }
                    name = line.substringAfterLast(",").trim()
                    logo = a["tvg-logo"]?.ifBlank { null }
                    group = a["group-title"]?.ifBlank { null } ?: "عام"
                }
                line.isNotEmpty() && !line.startsWith("#") -> {
                    out += Channel(name.ifBlank { line.substringAfterLast('/') }, line, logo, group)
                    name = ""; logo = null; group = "عام"
                }
            }
        }
        return out
    }

    fun load() {
        val url = playlistUrl.trim()
        if (url.isEmpty()) return
        prefs.edit().putString("url", url).apply()
        loading = true; error = null
        viewModelScope.launch {
            try {
                val list = withContext(Dispatchers.IO) {
                    val c = URL(url).openConnection() as HttpURLConnection
                    c.connectTimeout = 20000
                    c.readTimeout = 90000
                    c.instanceFollowRedirects = true
                    c.setRequestProperty("User-Agent", "VLC/3.0.18 LibVLC/3.0.18")
                    c.setRequestProperty("Accept", "*/*")
                    val code = c.responseCode
                    if (code !in 200..299) throw Exception("رمز الاستجابة من السيرفر: $code")
                    c.inputStream.bufferedReader().useLines { parseStream(it) }
                }
                channels = list
                groups = list.map { it.group }.distinct()
                selectedGroup = null
                if (list.isEmpty()) error = "القائمة فاضية أو الصيغة غير مدعومة"
            } catch (e: Throwable) {
                error = "تعذّر التحميل: ${e.message ?: e.javaClass.simpleName}"
            } finally { loading = false }
        }
    }
}
