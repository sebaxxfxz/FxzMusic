package com.fxzmusic.ytpipeline.log

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

val VisitorDataKey = stringPreferencesKey("visitorData")

val ShowAudioFallbackToastKey = booleanPreferencesKey("show_audio_fallback_toast")
