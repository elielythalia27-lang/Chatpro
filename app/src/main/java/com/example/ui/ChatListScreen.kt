package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.User
import com.example.ui.theme.ChatProCardDark
import com.example.ui.theme.ChatProCyan
import com.example.ui.theme.ChatProDarkBg
import com.example.ui.theme.ChatProOnlineGreen
import com.example.ui.theme.ChatProTeal
import com.example.ui.theme.ChatProTextPrimary
import com.example.ui.theme.ChatProTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatListScreen(
    viewModel: MainViewModel,
    onOpenChat: (User) -> Unit,
    onNewChatClick: () -> Unit
) {
    val users by viewModel.allUsers.collectAsState()
    val allMessages by viewModel.allMessages.collectAsState()
    val currentAccount by viewModel.currentAccount.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    val communityUser = users.firstOrNull { it.username == "comunidad_general" }
        ?: User(
            username = "comunidad_general",
            displayName = "Comunidad ChatPro 🌐",
            bio = "Chat público para todos los usuarios",
            isOnline = true
        )

    // Filter peers (excluding self and community general)
    val peers = users.filter { it.username != currentAccount?.username && it.username != "comunidad_general" }
        .filter {
            it.displayName.contains(searchQuery, ignoreCase = true) ||
            it.username.contains(searchQuery, ignoreCase = true)
        }

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
            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar conversaciones…", color = ChatProTextSecondary) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Buscar", tint = ChatProTeal)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("chat_search_input"),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = ChatProCardDark,
                    unfocusedContainerColor = ChatProCardDark,
                    focusedBorderColor = ChatProTeal,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                // Pinned Official Community Group Chat
                item {
                    val lastCommunityMsg = allMessages.firstOrNull {
                        it.chatId == "comunidad_general" || it.recipientUsername == "comunidad_general"
                    }

                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF162522)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ChatProTeal.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenChat(communityUser) }
                            .testTag("conversation_card_comunidad_general")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(ChatProTeal),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "🌐",
                                    fontSize = 24.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Comunidad ChatPro 🌐",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "CHAT PÚBLICO",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black,
                                        modifier = Modifier
                                            .background(ChatProTeal, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                val displayMsg = if (lastCommunityMsg != null) {
                                    "${lastCommunityMsg.senderUsername}: ${lastCommunityMsg.text}"
                                } else {
                                    "Chat grupal público para todos los usuarios."
                                }

                                Text(
                                    text = displayMsg,
                                    fontSize = 13.sp,
                                    color = ChatProCyan,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "Mensajes Directos",
                        color = ChatProTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )
                }

                if (peers.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = ChatProCardDark),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No tienes chats directos aún",
                                    color = ChatProTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Usa el chat de la comunidad arriba o toca '+' para escribir a un contacto",
                                    color = ChatProTextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                } else {
                    items(peers, key = { it.username }) { peer ->
                        val lastMessage = allMessages.firstOrNull {
                            (it.chatId == peer.username || it.recipientUsername == peer.username)
                        }

                        ChatConversationItem(
                            peer = peer,
                            lastMessageText = lastMessage?.text ?: peer.bio,
                            lastMessageTime = lastMessage?.timestamp ?: peer.lastSeen,
                            onClick = { onOpenChat(peer) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        // Floating Action Button for starting new chat
        FloatingActionButton(
            onClick = onNewChatClick,
            containerColor = ChatProTeal,
            contentColor = Color.Black,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("new_chat_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Nuevo Chat", modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
fun ChatConversationItem(
    peer: User,
    lastMessageText: String,
    lastMessageTime: Long,
    onClick: () -> Unit
) {
    val timeFormatted = remember(lastMessageTime) {
        if (lastMessageTime > 0) {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            sdf.format(Date(lastMessageTime))
        } else ""
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ChatProCardDark),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("conversation_card_${peer.username}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
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
                if (peer.avatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = peer.avatarUrl,
                        contentDescription = "Avatar de ${peer.displayName}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = peer.displayName.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = peer.displayName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = ChatProTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (timeFormatted.isNotBlank()) {
                        Text(
                            text = timeFormatted,
                            fontSize = 12.sp,
                            color = ChatProTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = lastMessageText,
                    fontSize = 13.sp,
                    color = ChatProTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
