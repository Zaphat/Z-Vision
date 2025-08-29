// screens/MainScreen.kt
package com.zteam.zvision.ui.screens

import android.content.res.Resources
import android.graphics.DashPathEffect
import android.graphics.Paint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zteam.zvision.R
import com.zteam.zvision.ui.commons.SettingsPopup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.platform.LocalContext
import com.zteam.zvision.ui.features.qrScan.QRDecoder
import com.zteam.zvision.data.local.AppDatabase
import com.zteam.zvision.data.repository.QrRepository
import com.zteam.zvision.domain.QrUsecase
import com.zteam.zvision.data.model.QrModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.UUID

@ExperimentalMaterial3Api
@Composable
fun MainScreen(
    initMode: String,
    onModeChange: (String) -> Unit,
    translateFromLanguage: String,
    translateToLanguage: String,
    onNavigateToLanguageSelection: (Boolean) -> Unit,
    onNavigateToQrStorage: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showResultSheet by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf("") }
    var copyEnabled by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val clipboard = LocalClipboardManager.current
    val pickMedia = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) {
            Log.d("PhotoPicker", "Selected URI: $uri")
            scope.launch {
                val decoded = withContext(Dispatchers.Default) {
                    QRDecoder.decodeFromUri(context, uri)
                }
                if (decoded != null) {
                    // Persist QR in database
                    withContext(Dispatchers.IO) {
                        val db = AppDatabase.getInstance(context)
                        val repo = QrRepository(db.qrDao())
                        val usecase = QrUsecase(repo)
                        val name = deriveNameFromContent(decoded)
                        val qr = QrModel(
                            id = UUID.randomUUID(),
                            name = name,
                            createdAt = Date(),
                            content = decoded.toByteArray(),
                            favorite = false
                        )
                        usecase.insertQr(qr)
                    }
                    // Show result in bottom sheet
                    resultText = decoded
                    copyEnabled = true
                    showResultSheet = true
                } else {
                    // Show toast only if no QR is found
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "No QR code found in image", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            Log.d("PhotoPicker", "No media selected")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Selection state for bottom mode buttons
        val isQRSelected = (initMode == "QR")
        val isTranslateSelected = initMode == "Translate"

        if (showResultSheet) {
            ModalBottomSheet(
                onDismissRequest = { showResultSheet = false },
                sheetState = sheetState,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Absolute.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SelectionContainer(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = resultText,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.verticalScroll(rememberScrollState())
                            )
                        }
                        if (copyEnabled) {
                            IconButton(onClick = {
                                clipboard.setText(AnnotatedString(resultText))
                            }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.content_copy_24px),
                                    contentDescription = "Copy to clipboard",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer // 👈 makes it follow your theme
                                )
                            }
                        }
                    }
                }
            }
        }

        // Settings header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 8.dp, bottom = 8.dp, top = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (initMode == "Translate") {
                Button(
                    onClick = { onNavigateToLanguageSelection(true) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = translateFromLanguage,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    painter = painterResource(id = R.drawable.arrow_forward_24px),
                    contentDescription = "Translate Arrow",
                    tint = MaterialTheme.colorScheme.primaryContainer,
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { onNavigateToLanguageSelection(false) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = translateToLanguage,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 16.sp
                    )
                }
            }
            else if (initMode == "QR") {
                Button (
                    onClick = {onNavigateToQrStorage()},
                    modifier = Modifier
                        .padding(start = 0.dp, end = 105.dp)
                        .size(width = 100.dp, height = 50.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ){
                    Text(text = "My QR" ,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 16.sp
                    )
                }
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "QR More",
                        tint = MaterialTheme.colorScheme.primaryContainer,
                    )
                }

                SettingsPopup(
                    expanded = menuExpanded,
                    onDismiss = { menuExpanded = false }
                )
            }
        }

        // Mode text in the center
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .background(Color.Gray),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Camera Preview for $initMode",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .padding(6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = {
                    if (initMode != "QR") {
                        Toast.makeText(context, "TODO: Translate from image not implemented yet", Toast.LENGTH_SHORT).show()
                    } else {
                        pickMedia.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                    }
                },
                modifier = Modifier
                    .padding(start = 35.dp, end = 10.dp)
                    .size(width = 50.dp, height = 50.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.filter_24px),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentDescription = "gallery_icon",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Button(
                onClick = { openCamera() },
                modifier = Modifier
                    .size(width = 70.dp, height = 70.dp),
                contentPadding = PaddingValues(0.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.brightness_1_24px),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    contentDescription = "camera_icon",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Button(
                onClick = { viewHistoryQRScans() },
                modifier = Modifier
                    .padding(start = 10.dp, end = 35.dp)
                    .size(width = 50.dp, height = 50.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.history_2_24px),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentDescription = "history_icon",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Bottom buttons
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Button(
                onClick = { onModeChange("QR") },
                modifier = Modifier
                    .padding(start = 35.dp, end = 5.dp)
                    .size(width = 100.dp, height = 50.dp),
                contentPadding = PaddingValues(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isQRSelected) MaterialTheme.colorScheme.primaryContainer else Color.DarkGray,
                    contentColor = if (isQRSelected) MaterialTheme.colorScheme.onPrimaryContainer else Color.White
                ),
                elevation = if (isQRSelected) ButtonDefaults.buttonElevation(defaultElevation = 6.dp) else ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.qr_code_24px),
                    contentDescription = "QR More",
                    modifier = Modifier.fillMaxSize(),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Button(
                onClick = { onModeChange("Translate") },
                modifier = Modifier
                    .padding(start = 5.dp, end = 35.dp)
                    .size(width = 100.dp, height = 50.dp),
                contentPadding = PaddingValues(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTranslateSelected) MaterialTheme.colorScheme.primaryContainer else Color.DarkGray,
                    contentColor = if (isTranslateSelected) MaterialTheme.colorScheme.onPrimary else Color.White
                ),
                elevation = if (isTranslateSelected) ButtonDefaults.buttonElevation(defaultElevation = 6.dp) else ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.g_translate_24px),
                    contentDescription = "QR More",
                    modifier = Modifier.fillMaxSize(),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}


fun viewHistoryQRScans() {
    // TODO: Implement navigation to history screen
}

fun openCamera() {
    // TODO: Implement camera opening
}

private fun deriveNameFromContent(content: String, maxLen: Int = 32): String {
    val trimmed = content.trim()
    if (trimmed.isEmpty()) return "Scanned QR"
    val singleLine = trimmed.lineSequence().firstOrNull() ?: trimmed
    return if (singleLine.length <= maxLen) singleLine else singleLine.take(maxLen - 1) + "…"
}
