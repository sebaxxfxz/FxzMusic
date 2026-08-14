package com.fxzmusic.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.fxzmusic.app.data.Song

class PlayerStateManager : ViewModel() {
    var showMiniPlayer by mutableStateOf(false)
        private set
        
    var isFullPlayerVisible by mutableStateOf(false)
        private set
        
    var showEqualizerSheet by mutableStateOf(false)
        private set
        
    var showAudioFxSheet by mutableStateOf(false)
        private set
        
    var showSongInfo by mutableStateOf(false)
        private set
        
    var tagEditorSong by mutableStateOf<Song?>(null)
        private set

    var isCarModeVisible by mutableStateOf(false)
        private set

    fun showMiniPlayer() {
        showMiniPlayer = true
    }

    fun hideMiniPlayer() {
        showMiniPlayer = false
        isFullPlayerVisible = false
        isCarModeVisible = false
    }

    fun openFullPlayer() {
        isFullPlayerVisible = true
        isCarModeVisible = false
    }

    fun closeFullPlayer() {
        isFullPlayerVisible = false
    }

    fun openCarMode() {
        isCarModeVisible = true
        isFullPlayerVisible = false
    }

    fun closeCarMode() {
        isCarModeVisible = false
    }

    fun showEqualizer() {
        showEqualizerSheet = true
    }

    fun hideEqualizer() {
        showEqualizerSheet = false
    }

    fun showAudioFx() {
        showAudioFxSheet = true
    }

    fun hideAudioFx() {
        showAudioFxSheet = false
    }

    fun editTags(song: Song) {
        tagEditorSong = song
    }

    fun hideTagEditor() {
        tagEditorSong = null
    }
}
