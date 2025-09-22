package com.example.teost.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

// Custom shapes for specific components
val CardShape = RoundedCornerShape(20.dp)
val BottomNavigationShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
val ButtonShape = RoundedCornerShape(12.dp)
val TextFieldShape = RoundedCornerShape(16.dp)
val DialogShape = RoundedCornerShape(28.dp)
val BottomSheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
