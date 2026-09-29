package com.example.ui

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.ChatMessage
import com.example.data.model.MessageStatus
import com.example.data.model.MessageType
import com.example.data.model.User
import com.example.ui.theme.ChatProBubbleReceiver
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatConversationScreen(
    viewModel: MainViewModel,
    peer: User,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    BackHandler {
        AudioPlayerHelper.stopAudio()
        onBack()
    }

    DisposableEffect(Unit) {
        onDispose {
            AudioPlayerHelper.stopAudio()
        }
    }

    val allMessages by viewModel.allMessages.collectAsState()
    val currentAccount by viewModel.currentAccount.collectAsState()
    val myUsername = currentAccount?.username ?: ""

    // Filter messages for this conversation and sort descending for reverseLayout
    val conversationMessages = allMessages.filter {
        (it.chatId == peer.username || (it.senderUsername == peer.username && it.recipientUsername == myUsername) ||
         (it.senderUsername == myUsername && it.recipientUsername == peer.username))
    }.sortedByDescending { it.timestamp }

    var textInput by remember { mutableStateOf("") }
    var selectedMessageForReaction by remember { mutableStateOf<ChatMessage?>(null) }

    // Real voice recorder state
    val recorderHelper = remember { VoiceRecorderHelper(context) }
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
            val file = recorderHelper.startRecording()
            if (file != null) {
                isRecording = true
            }
        }
    }

    // Real photo picker launcher
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
            if (compressed != null) {
                viewModel.sendPhoto(compressed)
            }
        }
    }

    val reactionEmojis = listOf("❤️", "👍", "😂", "🔥", "😮", "😢", "🙏", "👏")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatProDarkBg)
    ) {
        // TopAppBar with avatar, peer name, and online state
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .border(
                                width = if (peer.isOnline) 2.dp else 0.dp,
                                color = if (peer.isOnline) ChatProOnlineGreen else Color.Transparent,
                                shape = CircleShape
                            )
                            .padding(if (peer.isOnline) 1.5.dp else 0.dp)
                            .clip(CircleShape)
                            .background(getAvatarColor(peer.username)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = peer.displayName.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = peer.displayName,
                            color = ChatProTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (peer.isOnline) "en línea" else "últ. vez hace poco",
                            color = if (peer.isOnline) ChatProOnlineGreen else ChatProTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = {
                    AudioPlayerHelper.stopAudio()
                    onBack()
                }, modifier = Modifier.testTag("chat_back_button")) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = ChatProDarkSurface)
        )

        // Chat messages with reverseLayout = true
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            reverseLayout = true,
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(conversationMessages, key = { it.id }) { msg ->
                val isMe = msg.senderUsername == myUsername
                ChatMessageBubble(
                    message = msg,
                    isMe = isMe,
                    onLongClick = {
                        selectedMessageForReaction = msg
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        // Pill Input Bar or Audio Recording Bar
        Surface(
            color = ChatProDarkSurface,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
        ) {
            if (isRecording) {
                // Live voice recording control bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pulsing Red Recording Indicator
                    val infiniteTransition = rememberInfiniteTransition(label = "PulseRec")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(tween(500), repeatMode = RepeatMode.Reverse),
                        label = "AlphaPulse"
                    )

                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(ChatProErrorRed.copy(alpha = alpha))
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    val mins = recordingSeconds / 60
                    val secs = recordingSeconds % 60
                    Text(
                        text = "Grabando nota de voz %02d:%02d".format(mins, secs),
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )

                    // Cancel button
                    IconButton(onClick = {
                        recorderHelper.stopRecording()
                        isRecording = false
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancelar", tint = Color.Gray)
                    }

                    // Stop & Send button
                    IconButton(
                        onClick = {
                            val (file, duration) = recorderHelper.stopRecording()
                            isRecording = false
                            if (file != null && file.exists() && file.length() > 0) {
                                viewModel.sendVoiceNote(file, duration)
                            }
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(ChatProTeal)
                    ) {
                        Icon(Icons.Default.Done, contentDescription = "Enviar audio", tint = Color.Black)
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Real Attach photo button
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch("image/*")
                        },
                        modifier = Modifier.testTag("chat_attach_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Adjuntar Foto",
                            tint = ChatProTeal
                        )
                    }

                    // Text Input
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("Escribe un mensaje…", color = ChatProTextSecondary, fontSize = 14.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_message_input"),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ChatProBubbleReceiver,
                            unfocusedContainerColor = ChatProBubbleReceiver,
                            focusedBorderColor = ChatProTeal,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Animated Mic or Send Button
                    AnimatedContent(
                        targetState = textInput.isNotBlank(),
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "MicSendTransition"
                    ) { hasText ->
                        if (hasText) {
                            IconButton(
                                onClick = {
                                    viewModel.sendMessage(textInput.trim())
                                    textInput = ""
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(ChatProTeal)
                                    .testTag("chat_send_button")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Enviar",
                                    tint = Color.Black
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(ChatProTeal)
                                    .testTag("chat_voice_button")
                            ) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = "Grabar nota de voz",
                                    tint = Color.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Reaction Picker Dialog (8 emojis)
    if (selectedMessageForReaction != null) {
        Dialog(onDismissRequest = { selectedMessageForReaction = null }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = ChatProDarkSurface,
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    reactionEmojis.forEach { emoji ->
                        Text(
                            text = emoji,
                            fontSize = 28.sp,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    selectedMessageForReaction?.let {
                                        viewModel.reactToMessage(it.id, emoji)
                                    }
                                    selectedMessageForReaction = null
                                }
                                .padding(4.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    isMe: Boolean,
    onLongClick: () -> Unit
) {
    val bubbleShape = if (isMe) {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 20.dp)
    }

    val bubbleBg = if (isMe) ChatProTeal else ChatProBubbleReceiver
    val textColor = if (isMe) Color.White else Color.White

    val timeFormatted = remember(message.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
    }

    val currentlyPlayingId by AudioPlayerHelper.currentlyPlayingId.collectAsState()
    val isPlayingThisAudio = currentlyPlayingId == message.id

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(bubbleShape)
                .background(bubbleBg)
                .combinedClickable(
                    onClick = {},
                    onLongClick = onLongClick
                )
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Column {
                if (!isMe) {
                    Text(
                        text = "@${message.senderUsername}",
                        color = ChatProCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
                when (message.type) {
                    MessageType.PHOTO -> {
                        // Real Photo Display with AsyncImage
                        AsyncImage(
                            model = message.mediaUrl,
                            contentDescription = "Foto recibida",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (message.text.isNotBlank() && message.text != "Foto") {
                            Text(
                                text = message.text,
                                color = textColor,
                                fontSize = 14.sp
                            )
                        }
                    }

                    MessageType.AUDIO -> {
                        // Real Audio Player with Play / Pause / Waveform
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isMe) Color(0xFF004D40) else ChatProTeal)
                                    .clickable {
                                        if (message.mediaUrl.isNotBlank()) {
                                            AudioPlayerHelper.playAudio(message.id, message.mediaUrl)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlayingThisAudio) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlayingThisAudio) "Detener" else "Reproducir",
                                    tint = if (isMe) Color.White else Color.Black,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Interactive Audio Waveform Canvas
                            Canvas(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(24.dp)
                            ) {
                                val barWidth = 3.dp.toPx()
                                val spacing = 2.dp.toPx()
                                val barHeights = listOf(0.3f, 0.6f, 0.9f, 0.5f, 0.3f, 0.8f, 0.7f, 1.0f, 0.4f, 0.7f, 0.3f)
                                var currentX = 0f
                                barHeights.forEach { fraction ->
                                    val h = size.height * fraction
                                    val top = (size.height - h) / 2f
                                    drawRoundRect(
                                        color = if (isPlayingThisAudio) ChatProCyan else if (isMe) Color.White.copy(alpha = 0.9f) else ChatProCyan,
                                        topLeft = Offset(currentX, top),
                                        size = Size(barWidth, h),
                                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                                    )
                                    currentX += barWidth + spacing
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${message.audioDurationSeconds}s",
                                color = textColor.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    else -> {
                        Text(
                            text = message.text,
                            color = textColor,
                            fontSize = 15.sp,
                            lineHeight = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Time and Status indicator row
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeFormatted,
                        color = if (isMe) Color.White.copy(alpha = 0.8f) else ChatProTextSecondary,
                        fontSize = 11.sp
                    )

                    if (isMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        when (message.status) {
                            MessageStatus.SENDING -> {
                                Icon(
                                    Icons.Default.AccessTime,
                                    contentDescription = "Enviando",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            MessageStatus.SENT -> {
                                Icon(
                                    Icons.Default.Done,
                                    contentDescription = "Enviado",
                                    tint = ChatProCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            MessageStatus.READ -> {
                                Icon(
                                    Icons.Default.DoneAll,
                                    contentDescription = "Leído",
                                    tint = ChatProCyan,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Reaction Badges below the bubble
        if (message.reactions.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .padding(top = 2.dp, start = if (isMe) 0.dp else 8.dp, end = if (isMe) 8.dp else 0.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                message.reactions.values.distinct().forEach { emoji ->
                    val count = message.reactions.values.count { it == emoji }
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF222222), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF333333), RoundedCornerShape(12.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (count > 1) "$emoji $count" else emoji,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
