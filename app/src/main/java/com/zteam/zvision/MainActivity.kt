package com.zteam.zvision

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.zteam.zvision.ui.theme.ZVisionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ZVisionTheme {
                var initMode by remember { mutableStateOf("QR") }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    //Settings header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.End // <-- Align items to the start (left)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "QR More",
                            tint = Color.White
                            //modifier = Modifier.padding(start = 16.dp) // Optional: adds spacing from the left edge
                        )
                    }
                    // Mode text in the center
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.8f)
                            .background(Color.Gray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Camera Preview for ${initMode}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White
                        )
                    }
                    Row (
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.5f)
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    )
                    {
                        Button(
                            onClick = { viewHistoryQRScans() },
                            modifier = Modifier
                                .padding(start = 35.dp, end = 10.dp)
                                .size(width = 50.dp, height = 50.dp),
                            contentPadding = PaddingValues(0.dp), // Remove default padding
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.DarkGray // sets the background to gray
                            )
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.history_2_24px),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp), // Optional inner spacing
                                contentDescription = "history_icon",
                            )
                        }

                        // Middle Button - Camera
                        Button(
                            onClick = { openCamera() }, // Replace with your actual camera action
                            modifier = Modifier
                                .size(width = 70.dp, height = 70.dp), // Slightly bigger for emphasis (optional)
                            contentPadding = PaddingValues(0.dp),
                            shape = CircleShape, // Optional: Make it circular
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.DarkGray // sets the background to gray
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
                            onClick = { viewHistoryQRScans() },
                            modifier = Modifier
                                .padding(start = 10.dp, end = 35.dp)
                                .size(width = 50.dp, height = 50.dp),
                            contentPadding = PaddingValues(0.dp), // Remove default padding
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.DarkGray // sets the background to gray
                            )
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.filter_24px),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp), // Optional inner spacing
                                contentDescription = "history_icon",
                            )
                        }
                    }


                    // Buttons at the bottom
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp),
                        //verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,

                    ) {
                        Button(
                            onClick = { initMode = "QR" },
                            modifier = Modifier
                                .padding(start = 35.dp, end = 5.dp)
                                .size(width = 100.dp, height = 50.dp),
                            contentPadding = PaddingValues(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.DarkGray // sets the background to gray
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
                            onClick = { initMode = "Google Translate" },
                            modifier = Modifier
                                .padding(start = 5.dp, end = 35.dp)
                                .size(width = 100.dp, height = 50.dp),
                            contentPadding = PaddingValues(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.DarkGray // sets the background to gray
                            )
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.g_translate_24px),
                                contentDescription = "QR More",
                                modifier = Modifier
                                    .fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}

fun viewHistoryQRScans() {

}
fun openCamera(){

}