package com.dhyper.fncompanion.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

@Composable
fun FortniteYouTubePlayer(
    videoId: String,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        AndroidView(
            factory = { context ->
                val view = YouTubePlayerView(context)
                
                // Access internal WebView to set custom User-Agent and Headers
                val webView = view.getChildAt(0) as? android.webkit.WebView
                webView?.settings?.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"
                
                val options = IFramePlayerOptions.Builder()
                    .controls(1)
                    .origin("https://www.youtube.com") 
                    .rel(0)
                    .ivLoadPolicy(3)
                    .ccLoadPolicy(1)
                    .build()

                view.enableAutomaticInitialization = false
                view.initialize(object : AbstractYouTubePlayerListener() {
                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        youTubePlayer.loadVideo(videoId, 0f)
                    }
                }, options)
                
                lifecycleOwner.lifecycle.addObserver(view)
                view
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
