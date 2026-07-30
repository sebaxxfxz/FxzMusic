package com.fxzmusic.ytpipeline

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

internal const val DataStoreFile = "ytpipeline_settings"

internal val Context.ytpipelineDataStore: DataStore<Preferences> by preferencesDataStore(
    name = DataStoreFile,
)

val Context.ytPipelineDataStore: DataStore<Preferences>
    get() = ytpipelineDataStore
