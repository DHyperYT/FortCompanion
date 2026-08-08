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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Button(
        onClick = {
            val url = if (!videoId.isNullOrEmpty()) {
                "https://www.youtube.com/watch?v=$videoId"
            } else {
                "https://www.youtube.com/results?search_query=${java.net.URLEncoder.encode(query, "UTF-8")}"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        },
        modifier = modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
    ) {
        Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Color.White)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            if (videoId != null) "Watch on YouTube" else "Search on YouTube",
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 14.sp
        )
    }
}
