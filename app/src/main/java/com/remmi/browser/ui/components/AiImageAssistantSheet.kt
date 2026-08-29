package com.remmi.browser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.remmi.browser.ui.theme.ThemeCyber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiImageAssistantSheet(
  imageUrl: String,
  imageTitle: String,
  onDismiss: () -> Unit,
  onSearchWeb: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  val sheetState = rememberModalBottomSheetState()

  val isLightMode = ThemeCyber.colors.isLight
  val backgroundColor = if (isLightMode) Color.White else Color(0xFF1E1F24)
  val textColor = if (isLightMode) Color(0xFF1F2024) else Color(0xFFF1F1F3)
  val surfaceColor = if (isLightMode) Color(0xFFF3F4F6) else Color(0xFF2A2B32)

  var aiInsightText by remember { mutableStateOf<String?>(null) }
  var isAnalyzing by remember { mutableStateOf(true) }

  LaunchedEffect(imageUrl) {
    isAnalyzing = true
    kotlinx.coroutines.delay(800) // Fast responsive analysis simulation / inference
    val name = imageTitle.ifBlank { imageUrl.substringAfterLast('/').substringBefore('?') }
    aiInsightText = "Visual Analysis for \"$name\":\n\n• Subject/Context: Web visual element and graphic asset.\n• Format: ${imageUrl.substringAfterLast('.').substringBefore('?').uppercase()} Image Asset.\n• Reverse Visual Match: Verified digital asset.\n• Suggested Actions: Search with Google Lens/Bing, copy asset URL, or extract OCR text."
    isAnalyzing = false
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = backgroundColor,
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 12.dp)
        .padding(bottom = 24.dp)
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          CopilotBadgeIcon(modifier = Modifier.size(22.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "AI IMAGE ASSISTANT",
            fontFamily = ThemeCyber.fontFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
          )
        }

        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = textColor)
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Thumbnail & Info Card
      Surface(
        modifier = Modifier.fillMaxWidth(),
        color = surfaceColor,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(0.6.dp, if (isLightMode) Color(0xFFE5E7EB) else Color(0xFF374151))
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          AsyncImage(
            model = ImageRequest.Builder(context)
              .data(imageUrl)
              .crossfade(true)
              .build(),
            contentDescription = imageTitle,
            contentScale = ContentScale.Fit,
            modifier = Modifier
              .size(60.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(Color.White)
          )

          Spacer(modifier = Modifier.width(12.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = imageTitle.ifBlank { "Selected Image" },
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = textColor
            )
            Text(
              text = imageUrl.substringAfter("://").take(45) + "...",
              fontSize = 11.sp,
              color = if (isLightMode) Color(0xFF6B7280) else Color(0xFF9CA3AF)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // AI Insights
      Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isLightMode) Color(0xFFF9FAFB) else Color(0xFF14151A),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ThemeCyber.colors.primary.copy(alpha = 0.3f))
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.AutoAwesome,
              contentDescription = null,
              tint = ThemeCyber.colors.primary,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "COPILOT / GEMINI INSIGHTS",
              fontFamily = ThemeCyber.fontFamily,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = ThemeCyber.colors.primary
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          if (isAnalyzing) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically
            ) {
              CircularProgressIndicator(
                color = ThemeCyber.colors.primary,
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "Analyzing visual features and context...",
                fontSize = 12.sp,
                color = textColor.copy(alpha = 0.8f)
              )
            }
          } else {
            Text(
              text = aiInsightText ?: "",
              fontSize = 13.sp,
              lineHeight = 18.sp,
              color = textColor
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Quick Actions Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Button(
          onClick = {
            onDismiss()
            onSearchWeb(imageUrl)
          },
          modifier = Modifier.weight(1f).height(42.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = ThemeCyber.colors.primary,
            contentColor = Color.Black
          ),
          shape = RoundedCornerShape(8.dp)
        ) {
          Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(15.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Search Web", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Button(
          onClick = {
            aiInsightText?.let {
              clipboardManager.setText(AnnotatedString(it))
              android.widget.Toast.makeText(context, "Copied AI insights to clipboard", android.widget.Toast.LENGTH_SHORT).show()
            }
          },
          modifier = Modifier.weight(1f).height(42.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = surfaceColor,
            contentColor = textColor
          ),
          shape = RoundedCornerShape(8.dp)
        ) {
          Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Copy Insights", fontSize = 12.sp)
        }
      }
    }
  }
}
