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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.ChatProAdminBadge
import com.example.ui.theme.ChatProCardDark
import com.example.ui.theme.ChatProCyan
import com.example.ui.theme.ChatProDarkBg
import com.example.ui.theme.ChatProDarkSurface
import com.example.ui.theme.ChatProErrorRed
import com.example.ui.theme.ChatProOnlineGreen
import com.example.ui.theme.ChatProTeal
import com.example.ui.theme.ChatProTextPrimary
import com.example.ui.theme.ChatProTextSecondary

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onOpenAdmin: () -> Unit,
    onAddAccount: () -> Unit,
    onEditProfile: () -> Unit
) {
    val currentAccount by viewModel.currentAccount.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val currentUser = allUsers.firstOrNull { it.username == currentAccount?.username }

    var showAccountsDialog by remember { mutableStateOf(false) }
    var biometricEnabled by remember { mutableStateOf(true) }
    var ephemeralEnabled by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatProDarkBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Profile Header with 80dp avatar, 22sp bold name, gray @username
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ChatProCardDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(getAvatarColor(currentAccount?.username ?: "me")),
                    contentAlignment = Alignment.Center
                ) {
                    val avatarUrl = currentUser?.avatarUrl ?: currentAccount?.avatarUrl
                    if (!avatarUrl.isNullOrBlank()) {
                        coil.compose.AsyncImage(
                            model = avatarUrl,
                            contentDescription = "Avatar",
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = currentAccount?.displayName?.take(1)?.uppercase() ?: "U",
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = currentUser?.displayName ?: currentAccount?.displayName ?: "Usuario",
                    color = ChatProTextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "@${currentAccount?.username ?: "usuario"}",
                    color = ChatProTextSecondary,
                    fontSize = 14.sp
                )

                if (!currentUser?.bio.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentUser!!.bio,
                        color = ChatProTextSecondary,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Botón Editar Perfil
                Button(
                    onClick = onEditProfile,
                    colors = ButtonDefaults.buttonColors(containerColor = ChatProTeal),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("edit_profile_button")
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Editar Perfil",
                        color = Color.Black,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (currentAccount?.isAdmin == true) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF004D40), RoundedCornerShape(12.dp))
                            .border(1.dp, ChatProTeal, RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "ADMINISTRADOR DEL SISTEMA",
                            color = ChatProCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Admin Panel Shortcut (Only for Eliel_21 or admin accounts)
        if (currentAccount?.isAdmin == true) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF142926)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, ChatProTeal, RoundedCornerShape(16.dp))
                    .clickable { onOpenAdmin() }
                    .testTag("admin_panel_entry_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = "Panel de Administración",
                        tint = ChatProAdminBadge,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Panel de Administración",
                            color = ChatProTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Gestión de usuarios, almacenamiento y métricas",
                            color = ChatProCyan,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Multi-Account Switcher Section (Telegram style)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ChatProCardDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cuentas vinculadas (${accounts.size}/3)",
                        color = ChatProTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = onAddAccount,
                        colors = ButtonDefaults.buttonColors(containerColor = ChatProTeal),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("add_account_button")
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Añadir", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                accounts.forEach { acc ->
                    val isActive = acc.username == currentAccount?.username
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.switchAccount(acc.username) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(getAvatarColor(acc.username)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(acc.displayName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(acc.displayName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("@${acc.username}", color = ChatProTextSecondary, fontSize = 12.sp)
                        }
                        if (isActive) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Activa", tint = ChatProOnlineGreen, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Security & Preferences
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ChatProCardDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Seguridad y Privacidad", color = ChatProCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = ChatProTeal)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Cifrado AES-256", color = ChatProTextPrimary, fontSize = 14.sp)
                        Text("Activo en chats y almacenamiento", color = ChatProTextSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = true,
                        onCheckedChange = null,
                        colors = SwitchDefaults.colors(checkedThumbColor = ChatProCyan, checkedTrackColor = ChatProTeal)
                    )
                }

                HorizontalDivider(color = Color(0xFF2C2C2C), modifier = Modifier.padding(vertical = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = ChatProTeal)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Bloqueo Biométrico", color = ChatProTextPrimary, fontSize = 14.sp)
                        Text("Proteger inicio con huella", color = ChatProTextSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = biometricEnabled,
                        onCheckedChange = { biometricEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = ChatProCyan, checkedTrackColor = ChatProTeal)
                    )
                }

                HorizontalDivider(color = Color(0xFF2C2C2C), modifier = Modifier.padding(vertical = 12.dp))

                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "Acerca de ChatPro",
                    subtitle = "Versión 2.0.4 - Zero Rastro",
                    onClick = {}
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Logout and Delete Account (in red as requested)
        Button(
            onClick = { viewModel.logout() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C1E1E)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ChatProErrorRed, RoundedCornerShape(14.dp))
                .testTag("logout_button")
        ) {
            Icon(Icons.Default.Logout, contentDescription = null, tint = ChatProErrorRed)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Cerrar Sesión", color = ChatProErrorRed, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = ChatProTeal)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, color = ChatProTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = ChatProTextSecondary, fontSize = 12.sp)
        }
    }
}
