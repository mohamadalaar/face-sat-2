package com.example.myplayer

data class Channel(val name: String, val url: String, val logo: String?, val group: String)

object M3uParser {
    private val attr = Regex("""([\w-]+)="([^"]*)"""")

    fun parse(text: String): List<Channel> {
        val out = ArrayList<Channel>()
        var name = ""; var logo: String? = null; var group = "عام"
        for (raw in text.lineSequence()) {
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
}
