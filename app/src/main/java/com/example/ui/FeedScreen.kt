package com.example.ui

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.example.data.model.Post
import com.example.data.model.PostMediaType
import com.example.ui.theme.ChatProCardDark
import com.example.ui.theme.ChatProCyan
import com.example.ui.theme.ChatProDarkBg
import com.example.ui.theme.ChatProDarkSurface
import com.example.ui.theme.ChatProErrorRed
import com.example.ui.theme.ChatProOnlineGreen
import com.example.ui.theme.ChatProTeal
import com.example.ui.theme.ChatProTextPrimary
import com.example.ui.theme.ChatProTextSecondary
import com.example.util.AudioPlayerHelper
import com.example.util.ImageHelper
import com.example.util.VoiceRecorderHelper
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FeedScreen(
    viewModel: MainViewModel,
    onOpenProfile: () -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val currentAccount by viewModel.currentAccount.collectAsState()
    val allPosts by viewModel.allPosts.collectAsState()
    val isConnected by viewModel.isMoodleConnected.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            AudioPlayerHelper.stopAudio()
        }
    }

    var postText by remember { mutableStateOf("") }
    var selectedPhotoFile by remember { mutableStateOf<File?>(null) }
    var recordedAudioFile by remember { mutableStateOf<File?>(null) }
    var recordedAudioDuration by remember { mutableIntStateOf(0) }
    var isSubmitting by remember { mutableStateOf(false) }
    var submitErrorMessage by remember { mutableStateOf<String?>(null) }

    // Voice recording state
    val voiceRecorder = remember { VoiceRecorderHelper(context) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingSeconds = 0
            while (isRecording) {
                delay(1000L)
                recordingSeconds++
            }
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val file = voiceRecorder.startRecording()
            if (file != null) {
                isRecording = true
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val compressed = ImageHelper.compressAndSaveImage(
                context = context,
                sourceUri = uri,
                fileName = "post_photo_${System.currentTimeMillis()}.jpg",
                maxDimension = 1280
            )
            selectedPhotoFile = compressed
        }
    }

    var postForComments by remember { mutableStateOf<Post?>(null) }
    var postForLikesDialog by remember { mutableStateOf<Post?>(null) }
    var fullScreenImageUrl by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatProDarkBg),
        contentPadding = PaddingValues(12.dp)
    ) {
        // 1. Connection Status Banner
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp, start = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(if (isConnected) ChatProOnlineGreen else Color(0xFFFFB300))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isConnected) "Conectado" else "Conectando…",
                    fontSize = 12.sp,
                    color = if (isConnected) ChatProOnlineGreen else Color(0xFFFFB300),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // 2. "Crear Publicación" Card (Estilo Facebook)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ChatProCardDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // User Avatar
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(getAvatarColor(currentAccount?.username ?: "me"))
                                .clickable { onOpenProfile() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (!currentAccount?.avatarUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = currentAccount?.avatarUrl,
                                    contentDescription = "Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(
                                    text = currentAccount?.displayName?.take(1)?.uppercase() ?: "U",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Text Field para Comunicados y Actualizaciones
                        OutlinedTextField(
                            value = postText,
                            onValueChange = { postText = it },
                            placeholder = {
                                Text(
                                    "Escribe un comunicado o actualización para la comunidad…",
                                    color = ChatProTextSecondary,
                                    fontSize = 13.sp
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("create_post_input"),
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1E242B),
                                unfocusedContainerColor = Color(0xFF1E242B),
                                focusedBorderColor = ChatProTeal,
                                unfocusedBorderColor = Color(0xFF263238),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            maxLines = 5
                        )
                    }

                    // Attached Photo Preview
                    if (selectedPhotoFile != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(14.dp))
                        ) {
                            AsyncImage(
                                model = selectedPhotoFile,
                                contentDescription = "Foto adjunta",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            IconButton(
                                onClick = { selectedPhotoFile = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(30.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Quitar foto", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // Attached Voice Note Preview
                    if (recordedAudioFile != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = ChatProCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Nota de voz grabada (${recordedAudioDuration}s)",
                                color = Color.White,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                recordedAudioFile = null
                                recordedAudioDuration = 0
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Quitar audio", tint = Color.Gray, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // Voice recording in progress
                    if (isRecording) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF2C1E1E), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val infiniteTransition = rememberInfiniteTransition(label = "RecPulse")
                            val alpha by infiniteTransition.animateFloat(
                                initialValue = 0.3f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(tween(500), repeatMode = RepeatMode.Reverse),
                                label = "Pulse"
                            )
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(ChatProErrorRed.copy(alpha = alpha))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Grabando audio: %02d:%02d".format(recordingSeconds / 60, recordingSeconds % 60),
                                color = Color.White,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                voiceRecorder.stopRecording()
                                isRecording = false
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancelar", tint = Color.Gray)
                            }
                            IconButton(
                                onClick = {
                                    val (file, duration) = voiceRecorder.stopRecording()
                                    isRecording = false
                                    if (file != null && file.exists()) {
                                        recordedAudioFile = file
                                        recordedAudioDuration = duration
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(ChatProTeal, CircleShape)
                            ) {
                                Icon(Icons.Default.Done, contentDescription = "Listo", tint = Color.Black, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFF2B2B2B), thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Action buttons: Foto, Audio, Publicar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row {
                            TextButton(
                                onClick = { photoPickerLauncher.launch("image/*") },
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = ChatProTeal, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Foto", color = ChatProTeal, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            TextButton(
                                onClick = { audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null, tint = ChatProCyan, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Audio", color = ChatProCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Submit Post Button (Hides keyboard automatically)
                        val canSubmit = postText.isNotBlank() || selectedPhotoFile != null || recordedAudioFile != null
                        Button(
                            onClick = {
                                if (canSubmit && !isSubmitting) {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()

                                    val mediaType = when {
                                        selectedPhotoFile != null -> PostMediaType.IMAGE
                                        recordedAudioFile != null -> PostMediaType.AUDIO
                                        else -> PostMediaType.NONE
                                    }
                                    val mediaFile = selectedPhotoFile ?: recordedAudioFile
                                    isSubmitting = true
                                    submitErrorMessage = null
                                    viewModel.createPost(
                                        content = postText.trim(),
                                        mediaType = mediaType,
                                        mediaFile = mediaFile,
                                        audioDurationSeconds = recordedAudioDuration,
                                        onSuccess = {
                                            isSubmitting = false
                                            postText = ""
                                            selectedPhotoFile = null
                                            recordedAudioFile = null
                                            recordedAudioDuration = 0
                                            submitErrorMessage = null
                                        },
                                        onError = { err ->
                                            isSubmitting = false
                                            submitErrorMessage = err
                                        }
                                    )
                                }
                            },
                            enabled = canSubmit && !isSubmitting,
                            colors = ButtonDefaults.buttonColors(containerColor = ChatProTeal),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("submit_post_button")
                        ) {
                            if (isSubmitting) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.Black,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Publicando…", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            } else {
                                Text("Publicar", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }

                    if (submitErrorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = submitErrorMessage ?: "",
                            color = Color(0xFFFF5252),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        }

        // 3. Feed Posts List
        if (allPosts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Aún no hay publicaciones",
                            color = ChatProTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "¡Sé el primero en compartir una foto, audio o pensamiento!",
                            color = ChatProTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            items(allPosts, key = { it.id }) { post ->
                PostCard(
                    post = post,
                    currentUsername = currentAccount?.username ?: "",
                    isAdmin = currentAccount?.isAdmin == true,
                    onLike = { viewModel.toggleLikePost(post.id) },
                    onLikesCountClick = { postForLikesDialog = post },
                    onCommentClick = { postForComments = post },
                    onImageClick = { fullScreenImageUrl = it },
                    onDelete = { viewModel.deletePost(post.id) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    // Likes Dialog (Ver quién le dio like)
    if (postForLikesDialog != null) {
        val targetPost = allPosts.firstOrNull { it.id == postForLikesDialog?.id } ?: postForLikesDialog!!
        LikesDialog(
            likes = targetPost.likes,
            onDismiss = { postForLikesDialog = null }
        )
    }

    // Comments Sheet / Dialog with Comment Deletion
    if (postForComments != null) {
        val targetPost = allPosts.firstOrNull { it.id == postForComments?.id } ?: postForComments!!
        CommentsDialog(
            post = targetPost,
            currentUsername = currentAccount?.username ?: "",
            isAdmin = currentAccount?.isAdmin == true,
            onDismiss = { postForComments = null },
            onSendComment = { commentText ->
                viewModel.addComment(targetPost.id, commentText)
            },
            onDeleteComment = { commentId ->
                viewModel.deleteComment(targetPost.id, commentId)
            }
        )
    }

    // Fullscreen Photo Viewer Dialog
    if (fullScreenImageUrl != null) {
        FullScreenPhotoDialog(
            imageUrl = fullScreenImageUrl!!,
            onDismiss = { fullScreenImageUrl = null }
        )
    }
}

@Composable
fun PostCard(
    post: Post,
    currentUsername: String,
    isAdmin: Boolean,
    onLike: () -> Unit,
    onLikesCountClick: () -> Unit,
    onCommentClick: () -> Unit,
    onImageClick: (String) -> Unit = {},
    onDelete: () -> Unit
) {
    val isLikedByMe = post.isLikedBy(currentUsername)
    val heartColor by animateColorAsState(
        targetValue = if (isLikedByMe) Color(0xFFE91E63) else ChatProTextSecondary,
        label = "HeartColor"
    )

    val timeFormatted = remember(post.timestamp) {
        val diff = System.currentTimeMillis() - post.timestamp
        when {
            diff < 60_000 -> "Hace unos momentos"
            diff < 3600_000 -> "Hace ${diff / 60_000} min"
            diff < 86400_000 -> "Hace ${diff / 3600_000} h"
            else -> SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date(post.timestamp))
        }
    }

    val currentlyPlayingId by AudioPlayerHelper.currentlyPlayingId.collectAsState()
    val isPlayingThisAudio = currentlyPlayingId == post.id

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ChatProCardDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Avatar, Name, Time, Delete
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(getAvatarColor(post.authorUsername)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!post.authorAvatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = post.authorAvatarUrl,
                            contentDescription = "Avatar de ${post.authorDisplayName}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = post.authorDisplayName.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = post.authorDisplayName,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (post.authorUsername.equals("Eliel_21", ignoreCase = true)) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF004D40), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text("ADMIN", color = ChatProCyan, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "@${post.authorUsername} · $timeFormatted",
                            color = ChatProTextSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Public,
                            contentDescription = "Público",
                            tint = ChatProTextSecondary,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }

                // Delete option if author or admin
                if (post.authorUsername == currentUsername || isAdmin) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Post Text
            if (post.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = post.content,
                    color = Color.White,
                    fontSize = 15.sp,
                    lineHeight = 21.sp
                )
            }

            // Post Media (Image / Audio)
            when (post.mediaType) {
                PostMediaType.IMAGE -> {
                    if (!post.mediaUrl.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF141414))
                                .border(1.dp, Color(0xFF262626), RoundedCornerShape(14.dp))
                                .clickable { onImageClick(post.mediaUrl) }
                        ) {
                            SubcomposeAsyncImage(
                                model = post.mediaUrl,
                                contentDescription = "Imagen de la publicación",
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 160.dp, max = 460.dp),
                                loading = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        androidx.compose.material3.CircularProgressIndicator(
                                            modifier = Modifier.size(28.dp),
                                            color = ChatProTeal,
                                            strokeWidth = 2.5.dp
                                        )
                                    }
                                },
                                error = {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.BrokenImage, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(36.dp))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("No se pudo cargar la imagen", color = Color.Gray, fontSize = 12.sp)
                                    }
                                }
                            )

                            // Badge para indicar que se puede ver a tamaño completo
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ZoomIn, contentDescription = "Ver completa", tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Ver completa", color = Color.White, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                PostMediaType.AUDIO -> {
                    if (!post.mediaUrl.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1E1E1E), RoundedCornerShape(14.dp))
                                .border(1.dp, Color(0xFF2C2C2C), RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(ChatProTeal)
                                    .clickable {
                                        AudioPlayerHelper.playAudio(post.id, post.mediaUrl)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlayingThisAudio) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlayingThisAudio) "Detener" else "Reproducir",
                                    tint = Color.Black,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Canvas(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(26.dp)
                            ) {
                                val barWidth = 3.5.dp.toPx()
                                val spacing = 2.5.dp.toPx()
                                val heights = listOf(0.3f, 0.7f, 0.9f, 0.4f, 0.8f, 0.5f, 1.0f, 0.6f, 0.3f, 0.7f, 0.4f, 0.8f)
                                var curX = 0f
                                heights.forEach { f ->
                                    val h = size.height * f
                                    val top = (size.height - h) / 2f
                                    drawRoundRect(
                                        color = if (isPlayingThisAudio) ChatProCyan else Color.White.copy(alpha = 0.8f),
                                        topLeft = Offset(curX, top),
                                        size = Size(barWidth, h),
                                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                                    )
                                    curX += barWidth + spacing
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "${post.audioDurationSeconds}s",
                                color = ChatProCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                else -> {}
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Counters Row (Likes and Comments count)
            if (post.likesCount > 0 || post.commentsCount > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (post.likesCount > 0) {
                        Text(
                            text = "❤️ ${post.likesCount} me gusta",
                            color = ChatProCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onLikesCountClick() }
                                .padding(vertical = 2.dp, horizontal = 4.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    if (post.commentsCount > 0) {
                        Text(
                            text = "${post.commentsCount} comentarios",
                            color = ChatProTextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.clickable { onCommentClick() }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            HorizontalDivider(color = Color(0xFF282828), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(4.dp))

            // Action Buttons: Me gusta & Comentar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                TextButton(
                    onClick = onLike,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (isLikedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Me gusta",
                        tint = heartColor,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Me gusta",
                        color = heartColor,
                        fontSize = 13.sp,
                        fontWeight = if (isLikedByMe) FontWeight.Bold else FontWeight.Normal
                    )
                }

                TextButton(
                    onClick = onCommentClick,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Comment,
                        contentDescription = "Comentar",
                        tint = ChatProTextSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Comentar",
                        color = ChatProTextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun LikesDialog(
    likes: List<String>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ChatProDarkSurface,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Les gusta (${likes.size})",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.Gray)
                    }
                }

                HorizontalDivider(color = Color(0xFF282828), modifier = Modifier.padding(vertical = 8.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(likes) { username ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF222222), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(getAvatarColor(username)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = username.take(1).uppercase(),
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "@$username",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color(0xFFE91E63),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CommentsDialog(
    post: Post,
    currentUsername: String,
    isAdmin: Boolean,
    onDismiss: () -> Unit,
    onSendComment: (String) -> Unit,
    onDeleteComment: (String) -> Unit
) {
    var newCommentText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ChatProDarkSurface,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Comentarios (${post.commentsCount})",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.Gray)
                    }
                }

                HorizontalDivider(color = Color(0xFF282828), modifier = Modifier.padding(vertical = 8.dp))

                // List of Comments
                if (post.comments.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sé el primero en comentar esta publicación.",
                            color = ChatProTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(post.comments, key = { it.id }) { c ->
                            val canDelete = c.authorUsername == currentUsername || post.authorUsername == currentUsername || isAdmin

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(getAvatarColor(c.authorUsername)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!c.authorAvatarUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = c.authorAvatarUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Text(
                                            text = c.authorDisplayName.take(1).uppercase(),
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(
                                    modifier = Modifier
                                        .background(Color(0xFF242424), RoundedCornerShape(14.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                        .weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = c.authorDisplayName,
                                            color = ChatProCyan,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )

                                        if (canDelete) {
                                            IconButton(
                                                onClick = { onDeleteComment(c.id) },
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Eliminar comentario",
                                                    tint = Color.Gray,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = c.text,
                                        color = Color.White,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Input bar for adding a comment
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newCommentText,
                        onValueChange = { newCommentText = it },
                        placeholder = { Text("Escribe un comentario…", color = ChatProTextSecondary, fontSize = 13.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1E1E1E),
                            unfocusedContainerColor = Color(0xFF1E1E1E),
                            focusedBorderColor = ChatProTeal,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = {
                            if (newCommentText.isNotBlank()) {
                                onSendComment(newCommentText)
                                newCommentText = ""
                            }
                        },
                        enabled = newCommentText.isNotBlank(),
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (newCommentText.isNotBlank()) ChatProTeal else Color(0xFF333333))
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Enviar",
                            tint = if (newCommentText.isNotBlank()) Color.Black else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FullScreenPhotoDialog(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
        ) {
            SubcomposeAsyncImage(
                model = imageUrl,
                contentDescription = "Foto en pantalla completa",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                loading = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = ChatProTeal,
                            strokeWidth = 3.dp
                        )
                    }
                },
                error = {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.BrokenImage, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No se pudo cargar la imagen", color = Color.White, fontSize = 14.sp)
                    }
                }
            )

            // Botón de Cerrar en la esquina superior derecha
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(44.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
            }
        }
    }
}

