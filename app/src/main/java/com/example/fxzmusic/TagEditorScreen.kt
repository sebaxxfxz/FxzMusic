package com.example.fxzmusic

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.mpatric.mp3agic.ID3v24Tag
import com.mpatric.mp3agic.Mp3File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

private enum class SaveResult { SUCCESS, FAIL }

@Composable
fun TagEditorScreen(
    song: Song,
    onDismiss: () -> Unit,
    onSaved: (Song) -> Unit
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var title  by remember { mutableStateOf(song.title) }
    var artist by remember { mutableStateOf(song.artist) }
    var album  by remember { mutableStateOf(song.album) }
    var genre  by remember { mutableStateOf("") }
    var year   by remember { mutableStateOf("") }

    var isSaving   by remember { mutableStateOf(false) }
    var saveResult by remember { mutableStateOf<SaveResult?>(null) }

    val hasChanges = title != song.title || artist != song.artist || album != song.album || genre.isNotBlank() || year.isNotBlank()

    val songUri: Uri = remember(song.id) {
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.buildUpon()
            .appendPath(song.id)
            .build()
    }

    val pendingTitle  = remember { mutableStateOf(song.title) }
    val pendingArtist = remember { mutableStateOf(song.artist) }
    val pendingAlbum  = remember { mutableStateOf(song.album) }
    val pendingGenre  = remember { mutableStateOf("") }
    val pendingYear   = remember { mutableStateOf("") }

    val writeRequestLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    writeTagsViaOutputStream(
                        context, songUri, song.filePath,
                        pendingTitle.value, pendingArtist.value,
                        pendingAlbum.value, pendingGenre.value,
                        pendingYear.value
                    )
                }
                isSaving   = false
                saveResult = if (ok) SaveResult.SUCCESS else SaveResult.FAIL
                if (ok) {
                    val values = ContentValues().apply {
                        put(MediaStore.Audio.Media.TITLE,  pendingTitle.value)
                        put(MediaStore.Audio.Media.ARTIST, pendingArtist.value)
                        put(MediaStore.Audio.Media.ALBUM,  pendingAlbum.value)
                    }
                    context.contentResolver.update(songUri, values, null, null)
                    onSaved(song.copy(
                        title  = pendingTitle.value,
                        artist = pendingArtist.value,
                        album  = pendingAlbum.value
                    ))
                }
            }
        } else {
            isSaving   = false
            saveResult = SaveResult.FAIL
        }
    }

    fun doSave() {
        if (!hasChanges || song.filePath.isEmpty()) return
        isSaving   = true
        saveResult = null
        pendingTitle.value  = title
        pendingArtist.value = artist
        pendingAlbum.value  = album
        pendingGenre.value  = genre
        pendingYear.value   = year

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val req = MediaStore.createWriteRequest(context.contentResolver, listOf(songUri))
                writeRequestLauncher.launch(IntentSenderRequest.Builder(req.intentSender).build())
            } catch (_: Exception) {
                isSaving   = false
                saveResult = SaveResult.FAIL
            }
        } else {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    writeTagsViaOutputStream(
                        context, songUri, song.filePath,
                        title, artist, album, genre, year
                    )
                }
                if (ok) {
                    val values = ContentValues().apply {
                        put(MediaStore.Audio.Media.TITLE,  title)
                        put(MediaStore.Audio.Media.ARTIST, artist)
                        put(MediaStore.Audio.Media.ALBUM,  album)
                    }
                    context.contentResolver.update(songUri, values, null, null)
                }
                isSaving   = false
                saveResult = if (ok) SaveResult.SUCCESS else SaveResult.FAIL
                if (ok) onSaved(song.copy(title = title, artist = artist, album = album))
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A))) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 56.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BouncyIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, tint = Color.White, onClick = onDismiss)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Editor de Etiquetas", color = LocalFxzTheme.current.accent, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Escribe en el archivo MP3", color = Color.Gray, fontSize = 12.sp)
                    }
                    Box(modifier = Modifier.size(44.dp))
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(24.dp),
                    colors   = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(Brush.linearGradient(song.albumArt), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (song.coverUrl != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current).data(song.coverUrl).crossfade(true).build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(40.dp))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(formatTime(song.duration), color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(song.filePath.substringAfterLast("/"), color = Color.Gray, fontSize = 10.sp, maxLines = 1)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(song.title, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, maxLines = 1)
                            Text(song.artist, color = Color.Gray, fontSize = 13.sp, maxLines = 1)
                            Text(formatTime(song.duration), color = LocalFxzTheme.current.accent, fontSize = 12.sp)
                        }
                    }
                }
            }

            item { Text("Metadatos", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold) }

            item { TagField(value = title,  onValueChange = { title  = it }, label = "Titulo",  icon = Icons.Filled.MusicNote) }
            item { TagField(value = artist, onValueChange = { artist = it }, label = "Artista", icon = Icons.Filled.Person) }
            item { TagField(value = album,  onValueChange = { album  = it }, label = "Album",   icon = Icons.Filled.Album) }
            item { TagField(value = genre,  onValueChange = { genre  = it }, label = "Genero",  icon = Icons.Filled.LibraryMusic) }
            item { TagField(value = year,   onValueChange = { year   = it }, label = "Año",     icon = Icons.Filled.CalendarToday, keyboardType = KeyboardType.Number) }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(20.dp),
                    colors   = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Informacion del archivo", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        InfoRow("Duracion", formatTime(song.duration))
                        InfoRow("Ruta",     song.filePath.substringAfterLast("/"))
                    }
                }
            }

            item {
                AnimatedVisibility(
                    visible = saveResult != null,
                    enter   = expandVertically() + fadeIn(),
                    exit    = shrinkVertically() + fadeOut()
                ) {
                    val color = if (saveResult == SaveResult.SUCCESS) LocalFxzTheme.current.accent else Color(0xFFFF5252)
                    val msg   = if (saveResult == SaveResult.SUCCESS) "Etiquetas guardadas en el archivo" else "No se pudo guardar. Intenta de nuevo."
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(16.dp),
                        colors   = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (saveResult == SaveResult.SUCCESS) Icons.Filled.CheckCircle else Icons.Filled.Error,
                                contentDescription = null, tint = color, modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(msg, color = color, fontSize = 13.sp)
                        }
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick  = { title = song.title; artist = song.artist; album = song.album; genre = ""; year = ""; saveResult = null },
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(14.dp),
                        border   = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f))
                    ) {
                        Text("Restablecer", color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick  = { doSave() },
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(14.dp),
                        enabled  = hasChanges && !isSaving,
                        colors   = ButtonDefaults.buttonColors(
                            containerColor        = LocalFxzTheme.current.accent,
                            disabledContainerColor = Color(0xFF2A2A2A)
                        )
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Save, contentDescription = null, tint = if (hasChanges) Color.Black else Color.Gray, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Guardar", color = if (hasChanges) Color.Black else Color.Gray, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private fun writeTagsViaOutputStream(
    context: Context,
    uri: Uri,
    filePath: String,
    title: String,
    artist: String,
    album: String,
    genre: String,
    year: String = ""
): Boolean {
    return try {
        val sourceFile = File(filePath)
        if (!sourceFile.exists()) return false

        val mp3 = Mp3File(sourceFile)
        val tag = if (mp3.hasId3v2Tag()) mp3.id3v2Tag else ID3v24Tag()
        tag.title            = title
        tag.artist           = artist
        tag.album            = album
        tag.genreDescription = genre.ifBlank { null }
        if (year.isNotBlank()) tag.year = year
        mp3.id3v2Tag = tag

        val tempOut = File(context.cacheDir, "tag_tmp_${System.currentTimeMillis()}.mp3")
        try {
            mp3.save(tempOut.absolutePath)
            val outputStream = context.contentResolver.openOutputStream(uri, "wt")
                ?: context.contentResolver.openOutputStream(uri)
            if (outputStream != null) {
                FileInputStream(tempOut).use { input ->
                    outputStream.use { output -> input.copyTo(output) }
                }
                true
            } else {
                false
            }
        } finally {
            tempOut.delete()
        }
    } catch (_: Exception) {
        false
    }
}

@Composable
private fun TagField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        label         = { Text(label, color = Color.Gray) },
        leadingIcon   = { Icon(icon, contentDescription = null, tint = LocalFxzTheme.current.accent, modifier = Modifier.size(20.dp)) },
        modifier      = Modifier.fillMaxWidth(),
        shape         = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors        = OutlinedTextFieldDefaults.colors(
            focusedTextColor        = Color.White,
            unfocusedTextColor      = Color.White,
            focusedBorderColor      = LocalFxzTheme.current.accent,
            unfocusedBorderColor    = Color(0xFF2A2A2A),
            focusedContainerColor   = Color(0xFF1E1E1E),
            unfocusedContainerColor = Color(0xFF1E1E1E),
            cursorColor             = LocalFxzTheme.current.accent
        ),
        singleLine = true
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray,  fontSize = 12.sp)
        Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}