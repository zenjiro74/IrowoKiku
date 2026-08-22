package com.example.zenjiro74.irowokiku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.zenjiro74.irowokiku.ui.IrowoKikuApp
import com.example.zenjiro74.irowokiku.ui.theme.IrowoKikuTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IrowoKikuTheme {
                IrowoKikuApp()
            }
        }
    }
}
