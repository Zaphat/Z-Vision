// screens/MainScreen.kt
package com.zteam.zvision.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zteam.zvision.R

@Composable
fun MainScreen(
    initMode: String,
    onModeChange: (String) -> Unit,
    translateFromLanguage: String,
    translateToLanguage: String,
    onNavigateToLanguageSelection: (Boolean) -> Unit,
    onNavigateToQrCreation: () -> Unit,
    onNavigateToQrStorage: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        //Settings header
        if (initMode == "QR") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "QR More",
                    tint = Color.White
                )
            }
        } else { // Translate mode
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // From language button
                Button(
                    onClick = { onNavigateToLanguageSelection(true) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.DarkGray
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = translateFromLanguage,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Arrow or swap icon
                Icon(
                    painter = painterResource(id = R.drawable.arrow_forward_24px),
                    contentDescription = "Translate Arrow",
                    tint = Color.White,
                )

                Spacer(modifier = Modifier.width(8.dp))

                // To language button
                Button(
                    onClick = { onNavigateToLanguageSelection(false) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.DarkGray
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = translateToLanguage,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "QR More",
                    tint = Color.White,
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
                onClick = { onNavigateToQrStorage() },
                modifier = Modifier
                    .padding(start = 35.dp, end = 10.dp)
                    .size(width = 50.dp, height = 50.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.DarkGray
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.filter_24px),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentDescription = "history_icon",
                )
            }

            // Middle Button - Camera
            Button(
                onClick = { openCamera() },
                modifier = Modifier
                    .size(width = 70.dp, height = 70.dp),
                contentPadding = PaddingValues(0.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.DarkGray
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.brightness_1_24px),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    contentDescription = "camera_icon",
                )
            }

            Button(
                onClick = { onNavigateToQrStorage() },
                modifier = Modifier
                    .padding(start = 10.dp, end = 35.dp)
                    .size(width = 50.dp, height = 50.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.DarkGray
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.history_2_24px),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentDescription = "history_icon",
                )
            }
        }

        // Buttons at the bottom
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
                    containerColor = Color.DarkGray
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.qr_code_24px),
                    contentDescription = "QR More",
                    modifier = Modifier
                        .fillMaxSize()
                )
            }
            Button(
                onClick = { onModeChange("Google Translate") },
                modifier = Modifier
                    .padding(start = 5.dp, end = 35.dp)
                    .size(width = 100.dp, height = 50.dp),
                contentPadding = PaddingValues(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.DarkGray
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.g_translate_24px),
                    contentDescription = "QR More",
                    modifier = Modifier
                        .fillMaxSize()
                )
            }
            Button(
                onClick = { onNavigateToQrCreation() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DE9B6)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(text = "Create QR Code", color = Color.Black, fontSize = 18.sp)
            }
        }
    }
}

fun viewHistoryQRScans() {

}

fun openCamera() {

}