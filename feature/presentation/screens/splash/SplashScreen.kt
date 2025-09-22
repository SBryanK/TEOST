package com.example.teost.presentation.screens.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun SplashScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(id = com.example.teost.core.ui.R.drawable.teoc_logo),
            contentDescription = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.teoc_logo),
            modifier = Modifier.size(120.dp)
        )
    }
}

 