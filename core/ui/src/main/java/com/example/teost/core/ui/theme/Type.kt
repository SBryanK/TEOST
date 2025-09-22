package com.example.teost.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.teost.core.ui.R

private fun buildGoogleFontFamily(): FontFamily {
	val provider = GoogleFont.Provider(
		providerAuthority = "com.google.android.gms.fonts",
		providerPackage = "com.google.android.gms",
		certificates = R.array.com_google_android_gms_fonts_certs
	)
	val openSans = GoogleFont("Open Sans")
	val notoSansSc = GoogleFont("Noto Sans SC")
	return FontFamily(
		// Primary: Open Sans
		Font(googleFont = openSans, fontProvider = provider, weight = FontWeight.Normal),
		Font(googleFont = openSans, fontProvider = provider, weight = FontWeight.Medium),
		Font(googleFont = openSans, fontProvider = provider, weight = FontWeight.SemiBold),
		Font(googleFont = openSans, fontProvider = provider, weight = FontWeight.Bold),
		// zh-Hans fallback: Noto Sans SC
		Font(googleFont = notoSansSc, fontProvider = provider, weight = FontWeight.Normal),
		Font(googleFont = notoSansSc, fontProvider = provider, weight = FontWeight.Medium),
		Font(googleFont = notoSansSc, fontProvider = provider, weight = FontWeight.SemiBold),
		Font(googleFont = notoSansSc, fontProvider = provider, weight = FontWeight.Bold)
	)
}

private val BaseFontFamily: FontFamily = try { buildGoogleFontFamily() } catch (_: Throwable) { FontFamily.SansSerif }

fun buildTypography(fontFamily: FontFamily = BaseFontFamily): Typography = Typography(
	displayLarge = TextStyle(
		fontFamily = fontFamily,
		fontWeight = FontWeight.Normal,
		fontSize = 57.sp,
		lineHeight = 64.sp,
		letterSpacing = (-0.25).sp
	),
	displayMedium = TextStyle(
		fontFamily = fontFamily,
		fontWeight = FontWeight.Normal,
		fontSize = 45.sp,
		lineHeight = 52.sp,
		letterSpacing = 0.sp
	),
	displaySmall = TextStyle(
		fontFamily = fontFamily,
		fontWeight = FontWeight.Normal,
		fontSize = 36.sp,
		lineHeight = 44.sp,
		letterSpacing = 0.sp
	),
	headlineLarge = TextStyle(
		fontFamily = fontFamily,
		fontWeight = FontWeight.Bold,
		fontSize = 30.sp,
		lineHeight = 38.sp,
		letterSpacing = (-0.15).sp
	),
	headlineMedium = TextStyle(
		fontFamily = fontFamily,
		fontWeight = FontWeight.Normal,
		fontSize = 28.sp,
		lineHeight = 36.sp,
		letterSpacing = 0.sp
	),
	headlineSmall = TextStyle(
		fontFamily = fontFamily,
		fontWeight = FontWeight.Normal,
		fontSize = 24.sp,
		lineHeight = 32.sp,
		letterSpacing = 0.sp
	),
	titleLarge = TextStyle(
		fontFamily = fontFamily,
		fontWeight = FontWeight.Bold,
		fontSize = 22.sp,
		lineHeight = 28.sp,
		letterSpacing = 0.sp
	),
	titleMedium = TextStyle(
		fontFamily = fontFamily,
		fontWeight = FontWeight.Medium,
		fontSize = 16.sp,
		lineHeight = 24.sp,
		letterSpacing = 0.15.sp
	),
	titleSmall = TextStyle(
		fontFamily = fontFamily,
		fontWeight = FontWeight.Medium,
		fontSize = 14.sp,
		lineHeight = 20.sp,
		letterSpacing = 0.1.sp
	),
	bodyLarge = TextStyle(
		fontFamily = fontFamily,
		fontWeight = FontWeight.Normal,
		fontSize = 16.sp,
		lineHeight = 24.sp,
		letterSpacing = 0.25.sp
	),
	bodyMedium = TextStyle(
		fontFamily = fontFamily,
		fontWeight = FontWeight.Normal,
		fontSize = 14.sp,
		lineHeight = 20.sp,
		letterSpacing = 0.25.sp
	),
	bodySmall = TextStyle(
		fontFamily = fontFamily,
		fontWeight = FontWeight.Normal,
		fontSize = 12.sp,
		lineHeight = 16.sp,
		letterSpacing = 0.4.sp
	),
	labelLarge = TextStyle(
		fontFamily = fontFamily,
		fontWeight = FontWeight.Medium,
		fontSize = 13.sp,
		lineHeight = 18.sp,
		letterSpacing = 0.1.sp
	),
	labelMedium = TextStyle(
		fontFamily = fontFamily,
		fontWeight = FontWeight.Medium,
		fontSize = 12.sp,
		lineHeight = 16.sp,
		letterSpacing = 0.5.sp
	),
	labelSmall = TextStyle(
		fontFamily = fontFamily,
		fontWeight = FontWeight.Medium,
		fontSize = 11.sp,
		lineHeight = 16.sp,
		letterSpacing = 0.5.sp
	)
)

val Typography: Typography = buildTypography()