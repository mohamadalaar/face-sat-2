while (r.hasNext()) {
                    val n = r.nextName()
                    when (r.peek()) {
                        JsonToken.STRING, JsonToken.NUMBER -> m[n] = r.nextString()
                        JsonToken.BOOLEAN -> m[n] = r.nextBoolean().toString()
                        else -> r.skipValue()
                    }
                }
                r.endObject()
                block(m)
            }
            r.endArray()
        }
    }

    private suspend fun doLoad(refresh: Boolean) {
        val live = section == "live"
        val cf = File(dir, "${section}_cats.json")
        val sf = File(dir, "${section}_streams.json")
        var partial = false
        val list = withContext(Dispatchers.IO) {
            if (refresh || !sf.exists()) {
                try { download(api(if (live) "get_live_categories" else "get_vod_categories"), cf) } catch (_: Exception) {}
                partial = download(api(if (live) "get_live_streams" else "get_vod_streams"), sf)
            }
            val cats = HashMap<String, String>()
            try { forEachObject(cf) { m -> cats[m["category_id"] ?: ""] = m["category_name"] ?: "" } } catch (_: Exception) {}
            val out = ArrayList<Channel>()
            val u = Uri.encode(username.trim()); val p = Uri.encode(password.trim())
            try {
                forEachObject(sf) { m ->
                    val id = m["stream_id"] ?: return@forEachObject
                    val url = if (live) "$HOST/live/$u/$p/$id.$LIVE_EXT"
                    else "$HOST/movie/$u/$p/$id.${m["container_extension"] ?: "mp4"}"
                    out.add(Channel(m["name"] ?: id, url, m["stream_icon"]?.ifBlank { null },
                        cats[m["category_id"] ?: ""]?.ifBlank { null } ?: "عام"))
                }
            } catch (e: IOException) {
                if (out.isEmpty()) throw e else partial = true
            }
            out
        }
        channels = list
        groups = list.map { it.group }.distinct()
        selectedGroup = null
        error = when
