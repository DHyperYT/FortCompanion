package com.dhyper.fncompanion.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.URLEncoder

class YouTubeRepository {
    
    suspend fun searchVideoId(query: String): String? = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://www.youtube.com/results?search_query=$encodedQuery"
            
            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept-Language", "en-US,en;q=0.9")
                .get()

            val html = doc.html()
            
            // Look for videoId specifically within a videoRenderer block to avoid Shorts and Playlists
            // videoRenderer is used for standard long-form videos
            val videoRendererRegex = Regex(""""videoRenderer":\{"videoId":"([^"]+)"""")
            val match = videoRendererRegex.find(html)
            
            match?.groupValues?.get(1)
        } catch (e: Exception) {
            null
        }
    }
}
