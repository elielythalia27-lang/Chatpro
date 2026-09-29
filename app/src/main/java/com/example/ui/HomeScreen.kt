package com.example.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ChatProCyan
import com.example.ui.theme.ChatProDarkBg
import com.example.ui.theme.ChatProDarkSurface
import com.example.ui.theme.ChatProOnlineGreen
import com.example.ui.theme.ChatProTeal
import com.example.ui.theme.ChatProTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onOpenAdmin: () -> Unit,
    onAddAccount: () -> Unit,
    onEditProfile: () -> Unit
) {
    val activeTab by viewModel.activeTab.collectAsState()
    val isConnected by viewModel.isMoodleConnected.collectAsState()

    Scaffold(
        topBar = {
            // Only show main top bar on tabs other than CHATS (CommunityChatScreen has its own header)
            if (activeTab != HomeTab.CHATS) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "ChatPro",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isConnected) ChatProOnlineGreen else Color(0xFFFFB300))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isConnected) "Conectado" else "Conectando…",
                                fontSize = 11.sp,
                                color = if (isConnected) ChatProOnlineGreen else Color(0xFFFFB300),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = ChatProDarkSurface)
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = ChatProDarkSurface,
                contentColor = ChatProCyan
            ) {
                // Novedades (Feed de la Comunidad)
                NavigationBarItem(
                    selected = activeTab == HomeTab.FEED,
                    onClick = { viewModel.setHomeTab(HomeTab.FEED) },
                    icon = {
                        Icon(
                            if (activeTab == HomeTab.FEED) Icons.Filled.Campaign else Icons.Outlined.Campaign,
                            contentDescription = "Novedades"
                        )
                    },
                    label = { Text("Novedades", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        indicatorColor = ChatProTeal,
                        selectedTextColor = ChatProCyan,
                        unselectedIconColor = ChatProTextSecondary,
                        unselectedTextColor = ChatProTextSecondary
                    ),
                    modifier = Modifier.testTag("tab_feed")
                )

                // Chat General (Canal Público Oficial)
                NavigationBarItem(
                    selected = activeTab == HomeTab.CHATS,
                    onClick = { viewModel.setHomeTab(HomeTab.CHATS) },
                    icon = {
                        Icon(
                            if (activeTab == HomeTab.CHATS) Icons.Filled.Forum else Icons.Outlined.Forum,
                            contentDescription = "Chat General"
                        )
                    },
                    label = { Text("Chat General", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        indicatorColor = ChatProTeal,
                        selectedTextColor = ChatProCyan,
                        unselectedIconColor = ChatProTextSecondary,
                        unselectedTextColor = ChatProTextSecondary
                    ),
                    modifier = Modifier.testTag("tab_chats")
                )

                // Miembros de la Comunidad
                NavigationBarItem(
                    selected = activeTab == HomeTab.CONTACTS,
                    onClick = { viewModel.setHomeTab(HomeTab.CONTACTS) },
                    icon = {
                        Icon(
                            if (activeTab == HomeTab.CONTACTS) Icons.Filled.Groups else Icons.Outlined.Groups,
                            contentDescription = "Miembros"
                        )
                    },
                    label = { Text("Miembros", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        indicatorColor = ChatProTeal,
                        selectedTextColor = ChatProCyan,
                        unselectedIconColor = ChatProTextSecondary,
                        unselectedTextColor = ChatProTextSecondary
                    ),
                    modifier = Modifier.testTag("tab_contacts")
                )

                // Mi Cuenta / Ajustes
                NavigationBarItem(
                    selected = activeTab == HomeTab.SETTINGS,
                    onClick = { viewModel.setHomeTab(HomeTab.SETTINGS) },
                    icon = {
                        Icon(
                            if (activeTab == HomeTab.SETTINGS) Icons.Filled.AccountCircle else Icons.Outlined.AccountCircle,
                            contentDescription = "Mi Cuenta"
                        )
                    },
                    label = { Text("Mi Cuenta", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        indicatorColor = ChatProTeal,
                        selectedTextColor = ChatProCyan,
                        unselectedIconColor = ChatProTextSecondary,
                        unselectedTextColor = ChatProTextSecondary
                    ),
                    modifier = Modifier.testTag("tab_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ChatProDarkBg)
                .padding(innerPadding)
        ) {
            Crossfade(targetState = activeTab, label = "TabCrossfade") { tab ->
                when (tab) {
                    HomeTab.FEED -> FeedScreen(
                        viewModel = viewModel,
                        onOpenProfile = onEditProfile
                    )
                    HomeTab.CHATS -> CommunityChatScreen(
                        viewModel = viewModel
                    )
                    HomeTab.CONTACTS -> ContactsDirectoryScreen(
                        viewModel = viewModel
                    )
                    HomeTab.STATUS -> StatusScreen(
                        viewModel = viewModel,
                        onStatusClick = { status -> viewModel.openStatusViewer(status) }
                    )
                    HomeTab.SETTINGS -> SettingsScreen(
                        viewModel = viewModel,
                        onOpenAdmin = onOpenAdmin,
                        onAddAccount = onAddAccount,
                        onEditProfile = onEditProfile
                    )
                }
            }
        }
    }
}
