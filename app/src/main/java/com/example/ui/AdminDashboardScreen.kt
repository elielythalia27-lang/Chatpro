package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ChatProAdminBadge
import com.example.ui.theme.ChatProCardDark
import com.example.ui.theme.ChatProCyan
import com.example.ui.theme.ChatProDarkBg
import com.example.ui.theme.ChatProDarkSurface
import com.example.ui.theme.ChatProErrorRed
import com.example.ui.theme.ChatProTeal
import com.example.ui.theme.ChatProTextPrimary
import com.example.ui.theme.ChatProTextSecondary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val adminStats by viewModel.adminStats.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val systemLogs by viewModel.systemLogs.collectAsState()
    val globalConfig by viewModel.globalConfig.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Dashboard", "Usuarios", "Almacenamiento", "Logs", "Difusión")

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var broadcastText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatProDarkBg)
    ) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = ChatProAdminBadge,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Panel de Administración",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.testTag("admin_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = ChatProDarkSurface)
        )

        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = ChatProDarkSurface,
            contentColor = ChatProCyan,
            edgePadding = 12.dp
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTabIndex == index) ChatProCyan else ChatProTextSecondary
                        )
                    }
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (selectedTabIndex) {
                0 -> AdminOverviewTab(adminStats)
                1 -> AdminUsersTab(allUsers, viewModel) { msg ->
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                }
                2 -> AdminStorageTab(adminStats, viewModel) { msg ->
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                }
                3 -> AdminLogsTab(systemLogs)
                4 -> AdminBroadcastTab(
                    broadcastText = broadcastText,
                    onTextChange = { broadcastText = it },
                    onSend = {
                        if (broadcastText.isNotBlank()) {
                            viewModel.adminSendBroadcast(broadcastText.trim())
                            broadcastText = ""
                            scope.launch { snackbarHostState.showSnackbar("Mensaje del sistema difundido") }
                        }
                    }
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
fun AdminOverviewTab(stats: com.example.data.model.AdminStats) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Métricas Generales en Tiempo Real", color = ChatProCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AdminMetricCard(
                    title = "Almacenamiento Total",
                    value = "%.1f MB".format(stats.totalStorageBytes / (1024f * 1024f)),
                    modifier = Modifier.weight(1f)
                )
                AdminMetricCard(
                    title = "Usuarios Totales",
                    value = "${stats.totalUsers}",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AdminMetricCard(
                    title = "Activos (24h)",
                    value = "${stats.activeUsers24h}",
                    modifier = Modifier.weight(1f)
                )
                AdminMetricCard(
                    title = "Mensajes Hoy",
                    value = "${stats.messagesToday}",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ChatProCardDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Actividad de los últimos 7 días", color = ChatProTextPrimary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))

                    // 7-day activity bar chart
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val maxCount = stats.weeklyActivity.maxOfOrNull { it.second } ?: 1
                        stats.weeklyActivity.forEach { (day, count) ->
                            val heightFraction = count.toFloat() / maxCount
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(22.dp)
                                        .height((100 * heightFraction).dp)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(ChatProTeal)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(day, color = ChatProTextSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminMetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ChatProCardDark),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = ChatProTextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, color = ChatProTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AdminUsersTab(
    users: List<com.example.data.model.User>,
    viewModel: MainViewModel,
    showMessage: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = users.filter {
        it.displayName.contains(query, ignoreCase = true) || it.username.contains(query, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Buscar usuario para gestionar…", color = ChatProTextSecondary) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = ChatProCardDark,
                unfocusedContainerColor = ChatProCardDark,
                focusedBorderColor = ChatProTeal,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered, key = { it.username }) { user ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = ChatProCardDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(getAvatarColor(user.username)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(user.displayName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(user.displayName, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(user.cleanUsername, color = ChatProCyan, fontSize = 12.sp)
                        }

                        // Suspend / Reactivate
                        IconButton(onClick = {
                            viewModel.adminSuspendUser(user.username, !user.isSuspended)
                            showMessage("Estado de ${user.username} modificado")
                        }) {
                            Icon(
                                if (user.isSuspended) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = "Suspender",
                                tint = if (user.isSuspended) ChatProErrorRed else ChatProTeal
                            )
                        }

                        // Delete
                        if (!user.isAdmin) {
                            IconButton(onClick = {
                                viewModel.adminDeleteUser(user.username)
                                showMessage("Usuario ${user.username} eliminado")
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = ChatProErrorRed)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminStorageTab(
    stats: com.example.data.model.AdminStats,
    viewModel: MainViewModel,
    showMessage: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ChatProCardDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Desglose por carpetas internas", color = ChatProTextPrimary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                stats.storageByFolder.forEach { (folder, bytes) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(folder.replaceFirstChar { it.uppercase() }, color = ChatProTextSecondary)
                        Text("%.2f MB".format(bytes / (1024f * 1024f)), color = ChatProCyan, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Button(
            onClick = {
                viewModel.adminPurgeStorage()
                showMessage("Limpieza de caché y archivos antiguos completada")
            },
            colors = ButtonDefaults.buttonColors(containerColor = ChatProTeal),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Icon(Icons.Default.CleaningServices, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Ejecutar Limpieza Manual", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AdminLogsTab(logs: List<com.example.data.model.SystemLog>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(logs, key = { it.id }) { log ->
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = ChatProCardDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(if (log.isError) ChatProErrorRed else ChatProTeal, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(log.tag, color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(log.message, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text(time, color = ChatProTextSecondary, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun AdminBroadcastTab(
    broadcastText: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Emisión de Notificación Global del Sistema", color = ChatProCyan, fontWeight = FontWeight.Bold)
        Text(
            "Este mensaje se emitirá inmediatamente a todos los usuarios conectados.",
            color = ChatProTextSecondary,
            fontSize = 13.sp
        )

        OutlinedTextField(
            value = broadcastText,
            onValueChange = onTextChange,
            placeholder = { Text("Escribe el comunicado oficial…", color = ChatProTextSecondary) },
            modifier = Modifier.fillMaxWidth().height(140.dp),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = ChatProCardDark,
                unfocusedContainerColor = ChatProCardDark,
                focusedBorderColor = ChatProTeal,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Button(
            onClick = onSend,
            colors = ButtonDefaults.buttonColors(containerColor = ChatProAdminBadge),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Icon(Icons.Default.Campaign, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Emitir a Todos los Usuarios", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}
