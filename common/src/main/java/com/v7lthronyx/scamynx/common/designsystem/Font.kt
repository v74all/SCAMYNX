package com.v7lthronyx.scamynx.common.designsystem

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.v7lthronyx.scamynx.common.R

val LalezarFontFamily: FontFamily = FontFamily(Font(R.font.lalezar_regular))

val SansFontFamily: FontFamily = FontFamily.SansSerif

val DefaultFontFamily: FontFamily = FontFamily.SansSerif

fun localeAwareFontFamily(isPersian: Boolean): FontFamily =
    if (isPersian) LalezarFontFamily else SansFontFamily
