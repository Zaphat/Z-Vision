package com.zteam.zvision.presentation.ui.components

import android.view.Gravity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrResultBottomSheet(
    resultText: String,
    copyEnabled: Boolean,
    onDismiss: () -> Unit,
    sheetState: SheetState,
    onOpenUrl: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.Absolute.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SelectionContainer(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = resultText,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        textAlign = TextAlign.Justify,
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(enabled = copyEnabled) {
                            clipboard.setText(AnnotatedString(resultText))
                            val toast = Toast.makeText(
                                context,
                                "Copied to clipboard",
                                Toast.LENGTH_SHORT
                            )
                            toast.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 0)
                            toast.show()
                        }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "Copy to clipboard"
                    )
                }
            }

            // Add "Go to link" button for http(s) URLs
            val canOpenLink = isValidUri(resultText)
            if (canOpenLink && onOpenUrl != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        onDismiss()
                        onOpenUrl(resultText)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Go to link")
                }
            }
        }
    }
}

fun isValidUri(uriString: String): Boolean {
    return try {
        val uri = uriString.toUri()
        uri.scheme != null && uri.scheme!!.isNotBlank()
    } catch (_: Exception) {
        false
    }
}
