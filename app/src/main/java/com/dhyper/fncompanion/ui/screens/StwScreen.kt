package com.dhyper.fncompanion.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.dhyper.fncompanion.ui.theme.*
import com.dhyper.fncompanion.ui.viewmodels.StwViewModel
import java.io.ByteArrayInputStream

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun StwScreen(
    viewModel: StwViewModel
) {
    var isLoading by remember { mutableStateOf(true) }
    val missionUrl = "https://fortnitedb.com/"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SleekBackground)
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SleekSurface,
            tonalElevation = 4.dp
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                Text(
                    "SAVE THE WORLD MISSIONS",
                    style = MaterialTheme.typography.titleLarge,
                    color = SleekCyan,
                    fontWeight = FontWeight.Black
                )
                
                if (isLoading) {
                    CircularProgressIndicator(
                        color = SleekCyan,
                        modifier = Modifier.size(24.dp).align(Alignment.CenterEnd)
                    )
                }
            }
        }

        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false
                            
                            // Inject CSS to hide ads, headers, footers, and other clutter
                            val css = """
                                header, footer, .sidebar, .adsbygoogle, .ad-unit, 
                                .social-share, .newsletter-signup, .top-nav, 
                                .bottom-nav, #cookie-consent, .mobile-ad, 
                                [id*='google_ads'], [class*='ad-'], 
                                .announcement-banner, iframe, video, 
                                .video-container, .video-player, 
                                [class*='player'], [id*='player'],
                                .primis-player-container, .vidoomy-video { 
                                    display: none !important; 
                                    visibility: hidden !important;
                                    height: 0 !important;
                                    width: 0 !important;
                                    opacity: 0 !important;
                                    pointer-events: none !important;
                                }
                                body { 
                                    padding-top: 0 !important; 
                                    margin-top: 0 !important;
                                    background-color: #0b111a !important;
                                    color: #e2e8f0 !important;
                                }
                                .container, .main-content {
                                    max-width: 100% !important;
                                    padding: 8px !important;
                                    margin: 0 !important;
                                }
                            """.trimIndent().replace("\n", " ")

                            view?.evaluateJavascript(
                                "(function() {" +
                                "var style = document.createElement('style');" +
                                "style.innerHTML = '$css';" +
                                "document.head.appendChild(style);" +
                                // Also kill any existing video elements via JS
                                "var videos = document.getElementsByTagName('video');" +
                                "for (var i = 0; i < videos.length; i++) {" +
                                "  videos[i].pause();" +
                                "  videos[i].src = '';" +
                                "  videos[i].load();" +
                                "  videos[i].remove();" +
                                "}" +
                                "})()"
                            ) { }
                        }

                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            val url = request?.url?.toString() ?: ""
                            val adDomains = listOf(
                                "googleads", "doubleclick", "adnxs", "advertising",
                                "amazon-adsystem", "casalemedia", "criteo", "openx",
                                "pubmatic", "rubiconproject", "taboola", "outbrain",
                                "vidoomy", "vidverto", "primis", "dailymotion", 
                                "glomex", "anyclip", "connatix", "jwplayer"
                            )
                            
                            val isAd = adDomains.any { url.contains(it) }
                            val isVideo = url.endsWith(".mp4") || url.endsWith(".m3u8") || url.contains("video")
                            
                            if (isAd || isVideo) {
                                return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream("".toByteArray()))
                            }
                            
                            return super.shouldInterceptRequest(view, request)
                        }
                    }
                    loadUrl(missionUrl)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
