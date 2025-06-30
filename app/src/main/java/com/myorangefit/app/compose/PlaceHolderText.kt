package com.myorangefit.app.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import com.myorangefit.app.R


@Composable
fun PlaceHolderText(string: String) {

    val customFont = ResourcesCompat.getFont(LocalContext.current, R.font.comfortaa)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = string,
            color = Color.White,
            textAlign = TextAlign.Center,
            fontFamily = customFont?.let { FontFamily(it) }
        )
    }
}

@Preview
@Composable
private fun PlaceHolderTextPreview() {
    PlaceHolderText("Prova Prova Prova Prova Prova Prova Prova Prova Prova")
}