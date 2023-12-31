package com.example.statemachine.android

import androidx.compose.foundation.layout.Box
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun LoadingUi(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        CircularProgressIndicator(Modifier.align(Alignment.Center))
    }
}
