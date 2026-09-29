package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.UserStatus
import com.example.ui.theme.ChatProCardDark
import com.example.ui.theme.ChatProCyan
import com.example.ui.theme.ChatProDarkBg
import com.example.ui.theme.ChatProTeal
import com.example.ui.theme.ChatProTextPrimary
import com.example.ui.theme.ChatProTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatusScreen(
    viewModel: MainViewModel,
    onStatusClick: (UserStatus) -> Unit
) {
    val statuses by viewModel.activeStatuses.collectAsState()
    val currentAccount by viewModel.currentAccount.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    val myStatus = statuses.firstOrNull { it.authorUsername == currentAccount?.username }
    val otherStatuses = statuses.filter { it.authorUsername != currentAccount?.username }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatProDarkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Estados Públicos (24 Horas)",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = ChatProTextPrimary
            )
            Text(
                text = "Visibles para todos los usuarios de ChatPro",
                fontSize = 13.sp,
                color = ChatProTextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Horizontal Carousel of Stories
            LazyRow(
                contentPadding = PaddingValues(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // "Mi Estado" Item
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                if (myStatus != null) onStatusClick(myStatus)
                                else showCreateDialog = true
                            }
                            .testTag("my_status_item")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .border(
                                    width = 2.5.dp,
                                    brush = Brush.linearGradient(listOf(ChatProTeal, ChatProCyan)),
                                    shape = CircleShape
                                )
                                .padding(3.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2C2C2C)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (myStatus != null) {
                                Text(
                                    text = myStatus.authorDisplayName.take(1).uppercase(),
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Publicar Estado",
                                    tint = ChatProCyan,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (myStatus != null) "Mi Estado" else "Añadir",
                            color = ChatProTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Others' active statuses
                items(otherStatuses, key = { it.id }) { status ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onStatusClick(status) }
                            .testTag("status_item_${status.authorUsername}")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .border(
                                    width = 2.5.dp,
                                    brush = Brush.linearGradient(listOf(ChatProTeal, ChatProCyan)),
                                    shape = CircleShape
                                )
                                .padding(3.dp)
                                .clip(CircleShape)
                                .background(getAvatarColor(status.authorUsername)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = status.authorDisplayName.take(1).uppercase(),
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = status.authorDisplayName.split(" ").firstOrNull() ?: status.authorUsername,
                            color = ChatProTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Recientes",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = ChatProTextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Recent list
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(statuses, key = { it.id }) { status ->
                    val isMyStatus = status.authorUsername == currentAccount?.username
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = ChatProCardDark),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onStatusClick(status) }
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(getAvatarColor(status.authorUsername)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = status.authorDisplayName.take(1).uppercase(),
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = status.authorDisplayName,
                                    color = ChatProTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = status.text,
                                    color = ChatProTextSecondary,
                                    fontSize = 13.sp,
                                    maxLines = 1
                                )
                            }
                            if (isMyStatus) {
                                IconButton(onClick = { viewModel.deleteStatus(status.id) }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Eliminar",
                                        tint = Color(0xFFFF5252)
                                    )
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        FloatingActionButton(
            onClick = { showCreateDialog = true },
            containerColor = ChatProTeal,
            contentColor = Color.Black,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("create_status_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Nuevo Estado")
        }
    }

    if (showCreateDialog) {
        CreateStatusDialog(
            onDismiss = { showCreateDialog = false },
            onPost = { text ->
                viewModel.postStatus(text)
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun CreateStatusDialog(
    onDismiss: () -> Unit,
    onPost: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1E1E1E),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Publicar Estado",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ChatProTextPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("¿Qué estás pensando hoy?", color = ChatProTextSecondary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF2C2C2C),
                        unfocusedContainerColor = Color(0xFF2C2C2C),
                        focusedBorderColor = ChatProTeal,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Text("Cancelar", color = ChatProTextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { if (text.isNotBlank()) onPost(text.trim()) },
                        colors = ButtonDefaults.buttonColors(containerColor = ChatProTeal),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Publicar", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusViewerScreen(
    status: UserStatus,
    onClose: () -> Unit,
    onReply: (String) -> Unit
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(status.id) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 6000, easing = LinearEasing)
        )
        onClose()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Segmented story progress bar
            LinearProgressIndicator(
                progress = { progress.value },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = ChatProCyan,
                trackColor = Color(0x55FFFFFF)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Author Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(getAvatarColor(status.authorUsername)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = status.authorDisplayName.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = status.authorDisplayName,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(status.createdAt)),
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                }

                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                }
            }

            // Status Central Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = status.text,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp)
                )
            }

            // Viewers count at bottom
            Row(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Visibility,
                    contentDescription = null,
                    tint = ChatProCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Visto por ${status.viewersCount} usuarios",
                    color = Color.LightGray,
                    fontSize = 13.sp
                )
            }
        }
    }
}
