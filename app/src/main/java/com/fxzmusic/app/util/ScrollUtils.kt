package com.fxzmusic.app.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.LazyListState

@Composable
fun observeScrollToTop(
    selectedTab: Int,
    tabIndex: Int,
    lazyListState: LazyListState,
) {
    var lastSelectedTab by remember { mutableIntStateOf(selectedTab) }

    LaunchedEffect(selectedTab) {
        
        if (selectedTab == tabIndex && lastSelectedTab == tabIndex) {
            lazyListState.animateScrollToItem(0)
        }
        lastSelectedTab = selectedTab
    }
}

@Composable
fun rememberTabReselectionToken(selectedTab: Int, tabIndex: Int): Int {
    var token by remember { mutableIntStateOf(0) }
    var lastTab by remember { mutableIntStateOf(selectedTab) }

    LaunchedEffect(selectedTab) {
        if (selectedTab == tabIndex && lastTab == tabIndex) {
            token++
        }
        lastTab = selectedTab
    }
    return token
}
