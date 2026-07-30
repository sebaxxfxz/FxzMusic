package com.fxzmusic.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

enum class ServiceStatus { CHECKING, ONLINE, OFFLINE }

data class ServiceEntry(
    val name: String,
    val url: String,
    var status: ServiceStatus = ServiceStatus.CHECKING,
    var latencyMs: Long = 0L
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UptimeMonitorScreen(onBack: () -> Unit) {
    val services = remember { mutableStateListOf<ServiceEntry>() }

    LaunchedEffect(Unit) {
        services.addAll(listOf(
            ServiceEntry("LRCLIB", "https://lrclib.net/"),
            ServiceEntry("Unison", "https://unison.boidu.dev/"),
            ServiceEntry("BetterLyrics", "https://lyrics-api.boidu.dev/"),
            ServiceEntry("SimpMusic", "https://api-lyrics.simpmusic.org/"),
            ServiceEntry("Paxsenix", "https://lyrics.paxsenix.org/"),
            ServiceEntry("KuGou", "https://mobileservice.kugou.com/"),
        ))

        while (isActive) {
            val client = OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build()

            for (i in services.indices) {
                services[i] = services[i].copy(status = ServiceStatus.CHECKING)
                val start = System.currentTimeMillis()
                try {
                    val request = Request.Builder().url(services[i].url).head().build()
                    val response = client.newCall(request).execute()
                    val elapsed = System.currentTimeMillis() - start
                    services[i] = services[i].copy(
                        status = if (response.isSuccessful || response.code in 300..399) ServiceStatus.ONLINE else ServiceStatus.OFFLINE,
                        latencyMs = elapsed
                    )
                    response.close()
                } catch (e: Exception) {
                    val elapsed = System.currentTimeMillis() - start
                    services[i] = services[i].copy(status = ServiceStatus.OFFLINE, latencyMs = elapsed)
                }
            }
            delay(60_000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estado de Servicios", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "Proveedores de Letra",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(services.size) { index ->
                ServiceCard(services[index])
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                Text(
                    "Auto-refresh cada 60s",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun ServiceCard(service: ServiceEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AnimatedContent(
            targetState = service.status,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "status_icon"
        ) { status ->
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        when (status) {
                            ServiceStatus.ONLINE -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
                            ServiceStatus.OFFLINE -> MaterialTheme.colorScheme.error
                            ServiceStatus.CHECKING -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        }
                    )
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                service.name,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                service.url,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                fontSize = 11.sp
            )
        }

        AnimatedContent(
            targetState = service.status,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "status_detail"
        ) { status ->
            when (status) {
                ServiceStatus.ONLINE -> Text(
                    "${service.latencyMs}ms",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                ServiceStatus.OFFLINE -> Text(
                    "Offline",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                ServiceStatus.CHECKING -> CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 1.5.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
