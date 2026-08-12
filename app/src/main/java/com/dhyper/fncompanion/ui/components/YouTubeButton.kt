package com.dhyper.fncompanion.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhyper.fncompanion.ui.theme.SleekTextPrimary

@Composable
fun YouTubeButton(
    query: String,
    videoId: String? = null,
    label: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Button(
        onClick = {
            val url = if (!videoId.isNullOrEmpty()) {
                // Use vnd.youtube scheme to force opening in YouTube app and starting playback
                "vnd.youtube:$videoId"
            } else {
                "https://www.youtube.com/results?search_query=${java.net.URLEncoder.encode(query, "UTF-8")}"
            }
            
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            
            // Fallback for vnd.youtube if the app is not installed
            if (!videoId.isNullOrEmpty()) {
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$videoId"))
                    context.startActivity(webIntent)
                }
            } else {
                context.startActivity(intent)
            }
        },
        modifier = modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
    ) {
        Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Color.White)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label ?: if (videoId != null) "Watch on YouTube" else "Search on YouTube",
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 14.sp
        )
    }
}
