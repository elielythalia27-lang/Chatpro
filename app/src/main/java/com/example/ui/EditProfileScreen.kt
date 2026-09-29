package com.example.ui

import android.Manifest
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.ChatProCardDark
import com.example.ui.theme.ChatProCyan
import com.example.ui.theme.ChatProDarkBg
import com.example.ui.theme.ChatProDarkSurface
import com.example.ui.theme.ChatProTeal
import com.example.ui.theme.ChatProTextPrimary
import com.example.ui.theme.ChatProTextSecondary
import com.example.util.ImageHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val currentAccount by viewModel.currentAccount.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()

    val currentUser = allUsers.firstOrNull { it.username == currentAccount?.username }

    var displayName by remember(currentUser) { mutableStateOf(currentUser?.displayName ?: currentAccount?.displayName ?: "") }
    var bio by remember(currentUser) { mutableStateOf(currentUser?.bio ?: "¡Hola! Uso ChatPro.") }
    var newAvatarFile by remember { mutableStateOf<File?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    var showPhotoChoiceDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val compressed = ImageHelper.compressAndSaveImage(
                context = context,
                sourceUri = uri,
                fileName = "edit_avatar_${System.currentTimeMillis()}.jpg",
                maxDimension = 800
            )
            newAvatarFile = compressed
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            try {
                val file = File(context.cacheDir, "cam_edit_avatar_${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
                }
                newAvatarFile = file
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(null)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("ChatPro necesita acceso a la cámara para tomar tu foto de perfil")
            }
        }
    }

    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        galleryPickerLauncher.launch("image/*")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Editar Perfil",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("edit_profile_back")) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ChatProDarkSurface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = ChatProDarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 1. Avatar circular de 100dp con icono de cámara para cambiar la foto
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .border(
                        width = 2.5.dp,
                        brush = Brush.linearGradient(listOf(ChatProTeal, ChatProCyan)),
                        shape = CircleShape
                    )
                    .clip(CircleShape)
                    .background(getAvatarColor(currentAccount?.username ?: "me"))
                    .clickable { showPhotoChoiceDialog = true }
                    .testTag("edit_profile_avatar"),
                contentAlignment = Alignment.Center
            ) {
                if (newAvatarFile != null) {
                    AsyncImage(
                        model = newAvatarFile,
                        contentDescription = "Nuevo avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (!currentUser?.avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = currentUser?.avatarUrl,
                        contentDescription = "Avatar actual",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = displayName.take(1).uppercase().ifBlank { "U" },
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Camera Badge overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(30.dp)
                        .background(ChatProTeal, CircleShape)
                        .border(1.5.dp, Color.Black, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "Cambiar foto",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            TextButton(
                onClick = { showPhotoChoiceDialog = true },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Cambiar foto de perfil", color = ChatProCyan, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Campo de texto para el nombre completo
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Nombre completo") },
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null, tint = ChatProTeal)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_name_input"),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = ChatProCardDark,
                    unfocusedContainerColor = ChatProCardDark,
                    focusedBorderColor = ChatProTeal,
                    unfocusedBorderColor = Color(0xFF333333),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Campo de texto para el @usuario (solo lectura, no editable)
            OutlinedTextField(
                value = "@${currentAccount?.username ?: ""}",
                onValueChange = {},
                readOnly = true,
                label = { Text("@usuario (no editable)") },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_username_readonly"),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF181818),
                    unfocusedContainerColor = Color(0xFF181818),
                    focusedBorderColor = Color(0xFF2C2C2C),
                    unfocusedBorderColor = Color(0xFF2C2C2C),
                    focusedTextColor = Color.Gray,
                    unfocusedTextColor = Color.Gray
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Campo de texto para la biografía (opcional)
            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Biografía") },
                placeholder = { Text("Cuéntale a tus amigos algo sobre ti…", color = ChatProTextSecondary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .testTag("edit_bio_input"),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = ChatProCardDark,
                    unfocusedContainerColor = ChatProCardDark,
                    focusedBorderColor = ChatProTeal,
                    unfocusedBorderColor = Color(0xFF333333),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 5. Botón de Guardar Cambios
            Button(
                onClick = {
                    if (displayName.isBlank()) {
                        scope.launch { snackbarHostState.showSnackbar("El nombre no puede estar vacío") }
                        return@Button
                    }
                    isSaving = true
                    viewModel.updateProfile(
                        displayName = displayName.trim(),
                        bio = bio.trim(),
                        avatarFile = newAvatarFile,
                        onSuccess = { msg ->
                            isSaving = false
                            scope.launch {
                                snackbarHostState.showSnackbar("Perfil actualizado correctamente")
                                delay(600)
                                onBack()
                            }
                        },
                        onError = { err ->
                            isSaving = false
                            scope.launch { snackbarHostState.showSnackbar(err) }
                        }
                    )
                },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_profile_button"),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = ChatProTeal)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Guardar Cambios",
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Photo source selection Dialog (Camera / Gallery)
        if (showPhotoChoiceDialog) {
            AlertDialog(
                onDismissRequest = { showPhotoChoiceDialog = false },
                title = { Text("Cambiar Foto de Perfil", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "Selecciona de dónde deseas tomar la nueva imagen de tu avatar.",
                        color = ChatProTextSecondary
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showPhotoChoiceDialog = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            mediaPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                        } else {
                            galleryPickerLauncher.launch("image/*")
                        }
                    }) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = ChatProCyan)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Galería", color = ChatProCyan)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showPhotoChoiceDialog = false
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = ChatProTeal)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cámara", color = ChatProTeal)
                    }
                },
                containerColor = Color(0xFF1E1E1E),
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}
