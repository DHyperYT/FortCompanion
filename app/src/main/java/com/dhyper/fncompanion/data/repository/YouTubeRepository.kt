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
            
            // 1. Try specific videoRenderer for standard results
            val videoRendererRegex = Regex(""""videoRenderer":\{"videoId":"([^"]+)"""")
            val match = videoRendererRegex.find(html)
            var videoId = match?.groupValues?.get(1)
            
            // 2. Fallback to any videoId if the above fails
            if (videoId == null) {
                val fallbackRegex = Regex(""""videoId":"([^"]+)"""")
                videoId = fallbackRegex.find(html)?.groupValues?.get(1)
            }
            
            // Log for debugging
            if (videoId != null) {
                android.util.Log.d("YouTubeSearch", "Found direct video ID: $videoId for query: $query")
            }
            
            videoId
        } catch (e: Exception) {
            null
        }
    }
}
