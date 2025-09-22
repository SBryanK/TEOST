package com.example.teost.core.ui.theme

import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween

object MotionDurations {
    const val Fast = 150
    const val Medium = 200
    const val MediumPlus = 220
    const val Slow = 250
}

object MotionTweens {
    fun enter() = tween<Int>(durationMillis = MotionDurations.MediumPlus, easing = EaseOut)
    fun exit() = tween<Int>(durationMillis = MotionDurations.Medium, easing = EaseIn)
    fun fadeIn() = tween<Int>(durationMillis = MotionDurations.Medium, easing = EaseOut)
    fun fadeOut() = tween<Int>(durationMillis = MotionDurations.Medium, easing = EaseIn)
}


