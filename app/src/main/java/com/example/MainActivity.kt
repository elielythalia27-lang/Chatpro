package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AdminDashboardScreen
import com.example.ui.AuthScreen
import com.example.ui.ChatConversationScreen
import com.example.ui.HomeScreen
import com.example.ui.MainViewModel
import com.example.ui.NavigationScreen
import com.example.ui.StatusViewerScreen
import com.example.ui.theme.ChatProDarkBg
import com.example.ui.theme.ChatProTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ChatProTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ChatProDarkBg
                ) {
                    ChatProMainApp()
                }
            }
        }
    }
}

@Composable
fun ChatProMainApp(viewModel: MainViewModel = viewModel()) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val activeChatPeer by viewModel.activeChatPeer.collectAsState()
    val activeStatus by viewModel.activeStatus.collectAsState()

    Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
        when (screen) {
            NavigationScreen.AUTH -> {
                AuthScreen(viewModel = viewModel)
            }
            NavigationScreen.HOME -> {
                HomeScreen(
                    viewModel = viewModel,
                    onOpenAdmin = { viewModel.openAdminPanel() },
                    onAddAccount = { viewModel.navigateTo(NavigationScreen.AUTH) },
                    onEditProfile = { viewModel.openEditProfile() }
                )
            }
            NavigationScreen.EDIT_PROFILE -> {
                com.example.ui.EditProfileScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateTo(NavigationScreen.HOME) }
                )
            }
            NavigationScreen.CHAT_CONVERSATION -> {
                activeChatPeer?.let { peer ->
                    ChatConversationScreen(
                        viewModel = viewModel,
                        peer = peer,
                        onBack = { viewModel.navigateTo(NavigationScreen.HOME) }
                    )
                } ?: viewModel.navigateTo(NavigationScreen.HOME)
            }
            NavigationScreen.STATUS_VIEWER -> {
                activeStatus?.let { status ->
                    StatusViewerScreen(
                        status = status,
                        onClose = { viewModel.navigateTo(NavigationScreen.HOME) },
                        onReply = { replyText ->
                            // Optional reply behavior
                            viewModel.navigateTo(NavigationScreen.HOME)
                        }
                    )
                } ?: viewModel.navigateTo(NavigationScreen.HOME)
            }
            NavigationScreen.ADMIN_PANEL -> {
                AdminDashboardScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateTo(NavigationScreen.HOME) }
                )
            }
        }
    }
}
