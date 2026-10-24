package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SubscriptionTier
import com.example.ui.theme.BrandAccent
import com.example.ui.theme.BrandPrimary
import com.example.ui.viewmodel.AuthMode

@Composable
fun AuthScreen(
    authMode: AuthMode,
    onSetAuthMode: (AuthMode) -> Unit,
    onLogin: (email: String, pass: String, onError: (String) -> Unit) -> Unit,
    onRegister: (email: String, pass: String, name: String, plan: SubscriptionTier, onError: (String) -> Unit) -> Unit,
    onResetPassword: (email: String, newPass: String, onComplete: (Boolean) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var selectedPlan by remember { mutableStateOf(SubscriptionTier.PRO) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Brand Header
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(BrandPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = "TaskFlow Logo",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "TaskFlow",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = when (authMode) {
                        AuthMode.LOGIN -> "Welcome back! Sign in to access your projects."
                        AuthMode.REGISTER -> "Create your TaskFlow account and start organizing."
                        AuthMode.FORGOT_PASSWORD -> "Reset your password to regain account access."
                        else -> "Production Task Management SaaS"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                when (authMode) {
                    AuthMode.LOGIN -> {
                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                errorMessage = null
                            },
                            label = { Text("Email Address") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_email_input")
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                errorMessage = null
                            },
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_password_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "Forgot Password?",
                                style = MaterialTheme.typography.labelMedium.copy(color = BrandPrimary),
                                modifier = Modifier
                                    .clickable { onSetAuthMode(AuthMode.FORGOT_PASSWORD) }
                                    .padding(vertical = 4.dp)
                            )
                        }

                        Button(
                            onClick = {
                                if (email.isBlank() || password.isBlank()) {
                                    errorMessage = "Please enter both email and password."
                                } else {
                                    onLogin(email, password) { err -> errorMessage = err }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("login_submit_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                        ) {
                            Text("Sign In")
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                        // Demo Quick Login
                        OutlinedButton(
                            onClick = {
                                onLogin("alex.founder@taskflow.io", "password123") { err -> errorMessage = err }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("demo_login_button")
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = BrandAccent)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("1-Click Demo Login (Alex Vance - Pro)")
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("Don't have an account? ", fontSize = 13.sp)
                            Text(
                                text = "Sign up",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandPrimary,
                                modifier = Modifier.clickable {
                                    errorMessage = null
                                    onSetAuthMode(AuthMode.REGISTER)
                                }
                            )
                        }
                    }

                    AuthMode.REGISTER -> {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Full Name") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_name_input")
                        )

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_email_input")
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_password_input")
                        )

                        // Plan Selector on registration
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Choose Initial Subscription Plan", style = MaterialTheme.typography.labelMedium)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SubscriptionTier.values().forEach { tier ->
                                    FilterChip(
                                        selected = selectedPlan == tier,
                                        onClick = { selectedPlan = tier },
                                        label = { Text("${tier.title} ($${tier.monthlyPrice.toInt()}/m)", fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                if (name.isBlank() || email.isBlank() || password.isBlank()) {
                                    errorMessage = "Please fill in all fields."
                                } else {
                                    onRegister(email, password, name, selectedPlan) { err -> errorMessage = err }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("register_submit_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                        ) {
                            Text("Create Account & Start")
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("Already have an account? ", fontSize = 13.sp)
                            Text(
                                text = "Sign in",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandPrimary,
                                modifier = Modifier.clickable {
                                    errorMessage = null
                                    onSetAuthMode(AuthMode.LOGIN)
                                }
                            )
                        }
                    }

                    AuthMode.FORGOT_PASSWORD -> {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Account Email") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("New Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                if (email.isBlank() || password.isBlank()) {
                                    errorMessage = "Enter your email and new password."
                                } else {
                                    onResetPassword(email, password) { success ->
                                        if (!success) {
                                            errorMessage = "No account found with this email."
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                        ) {
                            Text("Reset Password & Sign In")
                        }

                        TextButton(
                            onClick = {
                                errorMessage = null
                                onSetAuthMode(AuthMode.LOGIN)
                            }
                        ) {
                            Text("Back to Sign In")
                        }
                    }

                    else -> {}
                }
            }
        }
    }
}
