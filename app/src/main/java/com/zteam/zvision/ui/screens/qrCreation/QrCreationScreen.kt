package com.zteam.zvision.ui.screens.qrCreation

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.zteam.zvision.ui.features.qrCreation.*
import com.zteam.zvision.data.local.AppDatabase
import com.zteam.zvision.data.repository.QrRepository
import com.zteam.zvision.domain.QrUsecase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.asImageBitmap

@Composable
fun QrCreationScreen(
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    // ViewModel creation (not recommended for prod, but matches Fragment)
    val viewModel = remember {
        val db = AppDatabase.getInstance(context)
        val repo = QrRepository(db.qrDao())
        val usecase = QrUsecase(repo)
        QrCreationViewModel(usecase)
    }
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("URL") }
    var content by remember { mutableStateOf("") }
    var favorite by remember { mutableStateOf(false) }
    var showToast by remember { mutableStateOf<String?>(null) }
    val generatedBitmap by viewModel.generatedBitmap.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Create QR Code", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("QR Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Type:")
            Spacer(Modifier.width(8.dp))
            DropdownMenuBox(type, onTypeChange = { type = it })
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            label = { Text("Content") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done)
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = favorite, onCheckedChange = { favorite = it })
            Text("Favorite")
        }
        Spacer(Modifier.height(16.dp))
        Row {
            Button(onClick = {
                val qrContent = when (type) {
                    "URL" -> if (content.isNotBlank()) UrlQR(content) else null
                    "Text" -> if (content.isNotBlank()) TextQR(content) else null
                    else -> null
                }
                if (qrContent != null) {
                    viewModel.generateQrBitmap(qrContent)
                } else {
                    showToast = "Invalid content"
                }
            }) {
                Text("Generate QR")
            }
            Spacer(Modifier.width(16.dp))
            Button(onClick = {
                val qrContent = when (type) {
                    "URL" -> if (content.isNotBlank()) UrlQR(content) else null
                    "Text" -> if (content.isNotBlank()) TextQR(content) else null
                    else -> null
                }
                if (name.isNotBlank() && qrContent != null) {
                    viewModel.createAndSaveQr(name, qrContent, favorite)
                    showToast = "QR saved!"
                } else {
                    showToast = "Fill all fields"
                }
            }) {
                Text("Save QR")
            }
        }
        Spacer(Modifier.height(24.dp))
        if (generatedBitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = generatedBitmap!!.asImageBitmap(),
                contentDescription = "Generated QR",
                modifier = Modifier.size(200.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onBack) { Text("Back") }
        if (showToast != null) {
            LaunchedEffect(showToast) {
                kotlinx.coroutines.delay(1500)
                showToast = null
            }
            Snackbar { Text(showToast!!) }
        }
    }
}

@Composable
private fun DropdownMenuBox(selected: String, onTypeChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Button(onClick = { expanded = true }) {
            Text(selected)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("URL") }, onClick = {
                onTypeChange("URL"); expanded = false
            })
            DropdownMenuItem(text = { Text("Text") }, onClick = {
                onTypeChange("Text"); expanded = false
            })
        }
    }
} 