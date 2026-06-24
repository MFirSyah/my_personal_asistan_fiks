package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassmorphicBackground
import com.example.ui.components.VividViolet
import com.example.ui.components.VividIndigo
import com.example.ui.components.VividRose
import com.example.viewmodel.FinanceActivityViewModel

@Composable
fun AuthScreen(
    viewModel: FinanceActivityViewModel,
    onBypass: () -> Unit
) {
    val appLang by viewModel.appLanguage.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegistering by remember { mutableStateOf(false) }
    var usernameInput by remember { mutableStateOf("") }
    var aiNameInput by remember { mutableStateOf("") }

    LaunchedEffect(isRegistering) {
        if (isRegistering) {
            if (usernameInput.isBlank()) usernameInput = "Owner"
            if (aiNameInput.isBlank()) aiNameInput = "FinAct AI"
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GlassmorphicBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .navigationBarsPadding()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Hero Title with display typography
            Text(
                text = "FinAct AI",
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                letterSpacing = 2.sp
            )
            
            Text(
                text = Localization.get("app_subtitle", appLang),
                fontSize = 14.sp,
                fontWeight = FontWeight.Light,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
            )

            // Glassmorphic Auth Form
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isRegistering) Localization.get("auth_register", appLang) else Localization.get("auth_login", appLang),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    if (isRegistering) {
                        // User Name Input
                        OutlinedTextField(
                            value = usernameInput,
                            onValueChange = { usernameInput = it },
                            label = { Text(if (appLang == "id") "Nama Anda" else "Your Name", color = Color.White.copy(alpha = 0.6f)) },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color.White) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VividViolet,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // AI Name Input
                        OutlinedTextField(
                            value = usernameInput.let { aiNameInput },
                            onValueChange = { aiNameInput = it },
                            label = { Text(if (appLang == "id") "Nama AI Asisten" else "AI Assistant Name", color = Color.White.copy(alpha = 0.6f)) },
                            leadingIcon = { Icon(Icons.Default.Face, contentDescription = null, tint = Color.White) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VividViolet,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Email Input
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(Localization.get("auth_email", appLang), color = Color.White.copy(alpha = 0.6f)) },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color.White) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VividViolet,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Input
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(Localization.get("auth_password", appLang), color = Color.White.copy(alpha = 0.6f)) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VividViolet,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Main Action Button (Gradient Primary)
                    Button(
                        onClick = {
                            if (isRegistering) {
                                if (usernameInput.isNotBlank()) {
                                    viewModel.updateUserName(usernameInput)
                                }
                                if (aiNameInput.isNotBlank()) {
                                    viewModel.updateAiName(aiNameInput)
                                }
                                viewModel.handleRegister(email, password)
                            } else {
                                viewModel.handleLogin(email, password)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(listOf(VividViolet, VividIndigo))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isRegistering) Localization.get("auth_sign_up", appLang) else Localization.get("auth_log_in", appLang),
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Switch Mode Text
                    TextButton(onClick = { isRegistering = !isRegistering }) {
                        Text(
                            text = if (isRegistering) Localization.get("auth_already_have_account", appLang) else Localization.get("auth_new_user", appLang),
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Google Sign-In Simulation Button
            OutlinedButton(
                onClick = {
                    // Simulates Firebase Auth Google Sign-in flow
                    onBypass()
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(horizontal = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = Localization.get("auth_sign_in_google", appLang),
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Demo Sandbox Bypass Button
            TextButton(onClick = { onBypass() }) {
                Text(
                    text = Localization.get("auth_offline_sandbox", appLang),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
