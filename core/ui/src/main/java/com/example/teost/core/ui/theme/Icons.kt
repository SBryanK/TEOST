package com.example.teost.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import com.example.teost.core.ui.R
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size

@Composable
fun BrandIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    contentDescription: String? = null
) {
    Icon(
        painter = painterResource(id = R.drawable.teoc_logo),
        contentDescription = contentDescription,
        modifier = modifier
            .size(size)
            .semantics { this.contentDescription = contentDescription ?: "" },
        tint = androidx.compose.ui.graphics.Color.Unspecified
    )
}

@Composable
fun BrandIcon24(
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    BrandIcon(modifier = modifier, size = 24.dp, contentDescription = contentDescription)
}

@Composable
fun BrandIcon56(
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    BrandIcon(modifier = modifier, size = 56.dp, contentDescription = contentDescription)
}


