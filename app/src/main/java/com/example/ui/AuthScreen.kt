package com.example.ui

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.ChatProCyan
import com.example.ui.theme.ChatProErrorRed
import com.example.ui.theme.ChatProOnlineGreen
import com.example.ui.theme.ChatProTeal
import com.example.ui.theme.ChatProTextPrimary
import com.example.ui.theme.ChatProTextSecondary
import com.example.util.ImageHelper
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@Composable
fun AuthScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    var isRegisterMode by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var selectedAvatarFile by remember { mutableStateOf<File?>(null) }
    var showPhotoChoiceDialog by remember { mutableStateOf(false) }
    var showPermissionRationale by remember { mutableStateOf(false) }
    var rationaleMessage by remember { mutableStateOf("") }

    val usernameCheckState by viewModel.usernameCheckState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Photo pickers
    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val compressed = ImageHelper.compressAndSaveImage(
                context = context,
                sourceUri = uri,
                fileName = "avatar_${System.currentTimeMillis()}.jpg",
                maxDimension = 800
            )
            selectedAvatarFile = compressed
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            try {
                val file = File(context.cacheDir, "cam_avatar_${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
                }
                selectedAvatarFile = file
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
    ) { isGranted ->
        galleryPickerLauncher.launch("image/*")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF121212), Color(0xFF1E1E1E))
                )
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            if (!isRegisterMode) {
                // Logo circular 100dp con degradado #00BFA5 a #1DE9B6
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(ChatProTeal, ChatProCyan)
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "ChatPro Logo",
                        tint = Color.Black,
                        modifier = Modifier.size(52.dp)
                    )
                }
            } else {
                // FUNCIONALIDAD 1: Avatar circular de 100dp en la parte superior del formulario con icono de cámara
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .border(
                            width = 2.5.dp,
                            brush = Brush.linearGradient(listOf(ChatProTeal, ChatProCyan)),
                            shape = CircleShape
                        )
                        .clip(CircleShape)
                        .background(
                            if (selectedAvatarFile == null) getAvatarColor(username.ifBlank { "new_user" })
                            else Color.Black
                        )
                        .clickable { showPhotoChoiceDialog = true }
                        .testTag("register_avatar_picker"),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedAvatarFile != null) {
                        AsyncImage(
                            model = selectedAvatarFile,
                            contentDescription = "Foto seleccionada",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (displayName.isNotBlank()) {
                        Text(
                            text = displayName.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Seleccionar foto",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Camera overlay badge
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
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Título ChatPro 32sp Bold blanco
            Text(
                text = "ChatPro",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = ChatProTextPrimary
            )
            Text(
                text = if (isRegisterMode) "Completa tu perfil para unirte a ChatPro" else "Mensajería rápida, privada y segura",
                fontSize = 14.sp,
                color = ChatProTextSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Animated content between Login and Register
            AnimatedContent(
                targetState = isRegisterMode,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "AuthModeTransition"
            ) { registering ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (registering) {
                        // Display Name
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            label = { Text("Nombre completo") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = ChatProTeal)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_fullname_input"),
                            shape = RoundedCornerShape(50),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ChatProTeal,
                                unfocusedBorderColor = Color(0xFF333333),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Username field with real-time live validation
                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            if (registering) {
                                viewModel.onUsernameInputChanged(it)
                            }
                        },
                        label = { Text("@usuario") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = ChatProTeal)
                        },
                        trailingIcon = {
                            if (registering) {
                                when (usernameCheckState) {
                                    is UsernameCheckState.Checking -> {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = ChatProTeal,
                                            strokeWidth = 2.dp
                                        )
                                    }
                                    is UsernameCheckState.Available -> {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Disponible",
                                            tint = ChatProOnlineGreen
                                        )
                                    }
                                    is UsernameCheckState.Taken -> {
                                        Icon(
                                            Icons.Default.Error,
                                            contentDescription = "Ocupado",
                                            tint = ChatProErrorRed
                                        )
                                    }
                                    else -> {}
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_username_input"),
                        shape = RoundedCornerShape(50),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (registering && usernameCheckState is UsernameCheckState.Taken) ChatProErrorRed else ChatProTeal,
                            unfocusedBorderColor = Color(0xFF333333),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )

                    // Real-time username feedback message
                    if (registering) {
                        AnimatedVisibility(visible = usernameCheckState !is UsernameCheckState.Idle) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                when (usernameCheckState) {
                                    is UsernameCheckState.Checking -> {
                                        Text(
                                            text = "Verificando disponibilidad…",
                                            color = ChatProTextSecondary,
                                            fontSize = 12.sp
                                        )
                                    }
                                    is UsernameCheckState.Available -> {
                                        Text(
                                            text = "✓ Nombre de usuario disponible",
                                            color = ChatProOnlineGreen,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    is UsernameCheckState.Taken -> {
                                        Text(
                                            text = "✗ Este nombre de usuario ya está en uso, por favor prueba con otro",
                                            color = ChatProErrorRed,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    else -> {}
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Password field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = ChatProTeal)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password visibility",
                                    tint = ChatProTextSecondary
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_password_input"),
                        shape = RoundedCornerShape(50),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ChatProTeal,
                            unfocusedBorderColor = Color(0xFF333333),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Botón 56dp con degradado horizontal, esquinas de píldora
                    val isRegisterDisabled = registering && (usernameCheckState is UsernameCheckState.Taken || usernameCheckState is UsernameCheckState.Checking || username.length < 3)

                    Button(
                        onClick = {
                            if (registering) {
                                viewModel.register(
                                    username = username,
                                    displayName = displayName,
                                    pass = password,
                                    avatarFile = selectedAvatarFile,
                                    onSuccess = {},
                                    onError = { err ->
                                        scope.launch { snackbarHostState.showSnackbar(err) }
                                    }
                                )
                            } else {
                                viewModel.login(
                                    username = username,
                                    pass = password,
                                    onSuccess = {},
                                    onError = { err ->
                                        scope.launch { snackbarHostState.showSnackbar(err) }
                                    }
                                )
                            }
                        },
                        enabled = !isRegisterDisabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("auth_submit_button"),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = Color(0xFF333333)
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (isRegisterDisabled) {
                                        Brush.horizontalGradient(listOf(Color(0xFF444444), Color(0xFF444444)))
                                    } else {
                                        Brush.horizontalGradient(listOf(ChatProTeal, ChatProCyan))
                                    },
                                    shape = RoundedCornerShape(50)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (registering) "Registrarse en ChatPro" else "Entrar a ChatPro",
                                color = if (isRegisterDisabled) Color.Gray else Color.Black,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Toggle login / register
            Row(
                modifier = Modifier
                    .clickable {
                        isRegisterMode = !isRegisterMode
                        if (!isRegisterMode) {
                            viewModel.onUsernameInputChanged("")
                        }
                    }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isRegisterMode) "¿Ya tienes cuenta? " else "¿No tienes cuenta? ",
                    color = ChatProTextSecondary,
                    fontSize = 14.sp
                )
                Text(
                    text = if (isRegisterMode) "Inicia sesión" else "Regístrate",
                    color = ChatProCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Photo source selection Dialog (Camera / Gallery)
        if (showPhotoChoiceDialog) {
            AlertDialog(
                onDismissRequest = { showPhotoChoiceDialog = false },
                title = { Text("Foto de Perfil", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "ChatPro necesita acceso a tus fotos o cámara para configurar tu avatar de perfil.",
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

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
