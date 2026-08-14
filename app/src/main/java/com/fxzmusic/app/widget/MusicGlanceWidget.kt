package com.fxzmusic.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.fxzmusic.app.MainActivity
import com.fxzmusic.app.R
import com.fxzmusic.app.service.PlaybackService

class MusicGlanceWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    companion object {
        val KEY_TITLE = stringPreferencesKey("title")
        val KEY_ARTIST = stringPreferencesKey("artist")
        val KEY_IS_PLAYING = booleanPreferencesKey("is_playing")
        val KEY_COVER_URL = stringPreferencesKey("cover_url")
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                val prefs = currentState<Preferences>()
                val title = prefs[KEY_TITLE] ?: context.getString(R.string.widget_no_track)
                val artist = prefs[KEY_ARTIST] ?: context.getString(R.string.widget_no_artist)
                val isPlaying = prefs[KEY_IS_PLAYING] ?: false

                WidgetContent(
                    context = context,
                    title = title,
                    artist = artist,
                    isPlaying = isPlaying
                )
            }
        }
    }

    @Composable
    private fun WidgetContent(
        context: Context,
        title: String,
        artist: String,
        isPlaying: Boolean
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xFF16161E)))
                .cornerRadius(20.dp)
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .size(46.dp)
                    .background(ColorProvider(Color(0xFF2A2A38)))
                    .cornerRadius(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(R.drawable.app_logo),
                    contentDescription = null,
                    modifier = GlanceModifier.size(32.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = GlanceModifier.width(12.dp))

            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight(),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Text(
                    text = title,
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
                Spacer(modifier = GlanceModifier.height(2.dp))
                Text(
                    text = artist,
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFA0A0B0)),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    maxLines = 1
                )
            }

            Spacer(modifier = GlanceModifier.width(8.dp))

            Row(
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Box(
                    modifier = GlanceModifier
                        .size(34.dp)
                        .cornerRadius(17.dp)
                        .background(ColorProvider(Color(0x22FFFFFF)))
                        .clickable(actionRunCallback<PrevActionCallback>()),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(android.R.drawable.ic_media_previous),
                        contentDescription = context.getString(R.string.widget_previous),
                        modifier = GlanceModifier.size(18.dp)
                    )
                }

                Spacer(modifier = GlanceModifier.width(6.dp))

                Box(
                    modifier = GlanceModifier
                        .size(40.dp)
                        .cornerRadius(20.dp)
                        .background(ColorProvider(Color(0xFF6C5CE7)))
                        .clickable(actionRunCallback<PlayPauseActionCallback>()),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(
                            if (isPlaying) android.R.drawable.ic_media_pause
                            else android.R.drawable.ic_media_play
                        ),
                        contentDescription = context.getString(R.string.widget_play_pause),
                        modifier = GlanceModifier.size(22.dp)
                    )
                }

                Spacer(modifier = GlanceModifier.width(6.dp))

                Box(
                    modifier = GlanceModifier
                        .size(34.dp)
                        .cornerRadius(17.dp)
                        .background(ColorProvider(Color(0x22FFFFFF)))
                        .clickable(actionRunCallback<NextActionCallback>()),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(android.R.drawable.ic_media_next),
                        contentDescription = context.getString(R.string.widget_next),
                        modifier = GlanceModifier.size(18.dp)
                    )
                }
            }
        }
    }
}

class PlayPauseActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val player = PlaybackService.exoPlayerInstance
        if (player != null) {
            if (player.isPlaying) player.pause() else player.play()
        } else {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            context.startActivity(intent)
        }
    }
}

class NextActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val player = PlaybackService.exoPlayerInstance
        if (player != null && player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        }
    }
}

class PrevActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val player = PlaybackService.exoPlayerInstance
        if (player != null && player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        }
    }
}

object MusicWidgetUpdater {
    suspend fun update(
        context: Context,
        title: String,
        artist: String,
        isPlaying: Boolean,
        coverUrl: String? = null
    ) {
        try {
            val glanceManager = GlanceAppWidgetManager(context)
            val glanceIds = glanceManager.getGlanceIds(MusicGlanceWidget::class.java)
            for (glanceId in glanceIds) {
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                    prefs.toMutablePreferences().apply {
                        this[MusicGlanceWidget.KEY_TITLE] = title
                        this[MusicGlanceWidget.KEY_ARTIST] = artist
                        this[MusicGlanceWidget.KEY_IS_PLAYING] = isPlaying
                        this[MusicGlanceWidget.KEY_COVER_URL] = coverUrl ?: ""
                    }
                }
                MusicGlanceWidget().update(context, glanceId)
            }
        } catch (e: Exception) {
            android.util.Log.w("MusicWidgetUpdater", "Widget update error: ${e.message}")
        }
    }
}
