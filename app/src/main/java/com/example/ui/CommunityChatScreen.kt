package com.example.ui

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.data.model.ChatMessage
import com.example.data.model.MessageStatus
import com.example.data.model.MessageType
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
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityChatScreen(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    val currentAccount by viewModel.currentAccount.collectAsState()
    val allMessages by viewModel.allMessages.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val isConnected by viewModel.isMoodleConnected.collectAsState()

    // Filter community messages
    val communityMessages = remember(allMessages) {
        allMessages.filter {
            it.chatId == "comunidad_general" || it.recipientUsername == "comunidad_general"
        }.sortedBy { it.timestamp }
    }

    val listState = rememberLazyListState()

    LaunchedEffect(communityMessages.size) {
        if (communityMessages.isNotEmpty()) {
            listState.animateScrollToItem(communityMessages.size - 1)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            AudioPlayerHelper.stopAudio()
        }
    }

    var messageText by remember { mutableStateOf("") }
    var selectedPhotoFile by remember { mutableStateOf<File?>(null) }
    var fullImagePreviewUrl by remember { mutableStateOf<String?>(null) }

    // Voice recorder state
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
                fileName = "chat_photo_${System.currentTimeMillis()}.jpg",
                maxDimension = 1280
            )
            selectedPhotoFile = compressed
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatProDarkBg)
    ) {
        // Top Bar: Community Header
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ChatProTeal),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Groups,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Comunidad ChatPro",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isConnected) ChatProOnlineGreen else Color(0xFFFFB300))
                            )
                        }

                        Text(
                            text = "Canal público oficial · ${allUsers.size} miembros",
                            color = ChatProTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            actions = {
                IconButton(onClick = { viewModel.refreshCommunityData() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualizar", tint = ChatProCyan)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = ChatProDarkSurface)
        )

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (communityMessages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1B2328)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Groups, contentDescription = null, tint = ChatProTeal, modifier = Modifier.size(32.dp))
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Bienvenido al Canal de la Comunidad",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Los mensajes, audios y fotos aquí son públicos para todos los miembros.",
                                color = ChatProTextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            } else {
                items(communityMessages, key = { it.id }) { msg ->
                    val isMe = msg.senderUsername == currentAccount?.username
                    val senderUser = allUsers.firstOrNull { it.username.equals(msg.senderUsername, ignoreCase = true) }

                    CommunityMessageRow(
                        message = msg,
                        senderDisplayName = senderUser?.displayName ?: msg.senderUsername,
                        senderAvatarUrl = senderUser?.avatarUrl,
                        isSenderAdmin = senderUser?.isAdmin == true || msg.senderUsername.equals("Eliel_21", ignoreCase = true),
                        isMe = isMe,
                        onImageClick = { fullImagePreviewUrl = it }
                    )
                }
            }
        }

        // Image Attachment Preview before send
        if (selectedPhotoFile != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E1E))
                    .padding(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = selectedPhotoFile,
                        contentDescription = "Foto adjunta",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Foto lista para enviar",
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { selectedPhotoFile = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancelar", tint = Color.Gray)
                    }
                }
            }
        }

        // Voice Recording in Progress Bar
        AnimatedVisibility(visible = isRecording) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2C1919))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val pulseTransition = rememberInfiniteTransition(label = "pulse")
                val alpha by pulseTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(500), repeatMode = RepeatMode.Reverse),
                    label = "alpha"
                )
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(ChatProErrorRed.copy(alpha = alpha))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Grabando nota de voz: %02d:%02d".format(recordingSeconds / 60, recordingSeconds % 60),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
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
                            viewModel.sendCommunityAudioMessage(file, duration)
                        }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .background(ChatProTeal, CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar", tint = Color.Black, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Bottom Input Bar
        Surface(
            color = ChatProDarkSurface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Attach Photo button
                IconButton(
                    onClick = { photoPickerLauncher.launch("image/*") },
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Adjuntar Foto",
                        tint = ChatProTeal
                    )
                }

                // Text field
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text("Escribe un mensaje para todos…", color = ChatProTextSecondary, fontSize = 13.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("community_message_input"),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1A1F24),
                        unfocusedContainerColor = Color(0xFF1A1F24),
                        focusedBorderColor = ChatProTeal,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Microphone button (Voice Note) or Send Button
                if (messageText.isBlank() && selectedPhotoFile == null) {
                    IconButton(
                        onClick = { audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF222B32))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Grabar audio",
                            tint = ChatProCyan
                        )
                    }
                } else {
                    IconButton(
                        onClick = {
                            val textToSend = messageText.trim()
                            val photoToSend = selectedPhotoFile
                            messageText = ""
                            selectedPhotoFile = null
                            // Se mantiene el teclado activo para poder seguir chateando de forma continua

                            if (photoToSend != null) {
                                viewModel.sendCommunityPhotoMessage(photoToSend, textToSend.ifBlank { "Foto" })
                            } else if (textToSend.isNotBlank()) {
                                viewModel.sendCommunityTextMessage(textToSend)
                            }
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(ChatProTeal)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Enviar",
                            tint = Color.Black
                        )
                    }
                }
            }
        }
    }

    // Full Screen Image Dialog
    if (fullImagePreviewUrl != null) {
        Dialog(onDismissRequest = { fullImagePreviewUrl = null }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .clickable { fullImagePreviewUrl = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = fullImagePreviewUrl,
                    contentDescription = "Foto ampliada",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun CommunityMessageRow(
    message: ChatMessage,
    senderDisplayName: String,
    senderAvatarUrl: String?,
    isSenderAdmin: Boolean,
    isMe: Boolean,
    onImageClick: (String) -> Unit
) {
    val currentlyPlayingId by AudioPlayerHelper.currentlyPlayingId.collectAsState()
    val isPlayingThis = currentlyPlayingId == message.id

    val timeFormatted = remember(message.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        // Incoming message: Author Avatar
        if (!isMe) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(getAvatarColor(message.senderUsername)),
                contentAlignment = Alignment.Center
            ) {
                if (!senderAvatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = senderAvatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = senderDisplayName.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Message Bubble
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isMe) Color(0xFF00564D) else Color(0xFF1F262D)
            ),
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                // Header (Only for incoming messages in community)
                if (!isMe) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 3.dp)
                    ) {
                        Text(
                            text = senderDisplayName,
                            color = ChatProCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        if (isSenderAdmin) {
                            Text(
                                text = "ADMIN",
                                color = Color.Black,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(ChatProTeal, RoundedCornerShape(3.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        } else {
                            Text(
                                text = "@${message.senderUsername}",
                                color = ChatProTextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // Message Content (Photo / Audio / Text)
                when (message.type) {
                    MessageType.PHOTO -> {
                        AsyncImage(
                            model = message.mediaUrl,
                            contentDescription = "Foto recibida",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onImageClick(message.mediaUrl) }
                        )
                        if (message.text.isNotBlank() && message.text != "Foto") {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = message.text, color = Color.White, fontSize = 14.sp)
                        }
                    }

                    MessageType.AUDIO -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isMe) ChatProTeal else Color(0xFF2E3842))
                                    .clickable {
                                        AudioPlayerHelper.playAudio(message.id, message.mediaUrl)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlayingThis) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = if (isMe) Color.Black else ChatProTeal,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Canvas(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(20.dp)
                            ) {
                                val barWidth = 3.dp.toPx()
                                val spacing = 2.dp.toPx()
                                val heights = listOf(0.4f, 0.8f, 0.5f, 1f, 0.7f, 0.3f, 0.9f, 0.6f, 0.4f, 0.8f)
                                var curX = 0f
                                heights.forEach { f ->
                                    val h = size.height * f
                                    val top = (size.height - h) / 2f
                                    drawRoundRect(
                                        color = if (isPlayingThis) ChatProCyan else Color.White.copy(alpha = 0.8f),
                                        topLeft = Offset(curX, top),
                                        size = Size(barWidth, h),
                                        cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                                    )
                                    curX += barWidth + spacing
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${message.audioDurationSeconds}s",
                                color = ChatProCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    else -> {
                        Text(
                            text = message.text,
                            color = Color.White,
                            fontSize = 14.sp,
                            lineHeight = 19.sp
                        )
                    }
                }

                // Time tag and status
                Row(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeFormatted,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 10.sp
                    )
                    if (isMe) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = when (message.status) {
                                MessageStatus.READ -> Icons.Default.DoneAll
                                MessageStatus.SENT -> Icons.Default.DoneAll
                                else -> Icons.Default.Check
                            },
                            contentDescription = null,
                            tint = if (message.status == MessageStatus.READ) ChatProCyan else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}
