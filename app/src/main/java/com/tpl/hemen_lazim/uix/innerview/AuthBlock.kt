package com.tpl.hemen_lazim.uix.innerview

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthBlock(
    isLogin: Boolean,
    username: String,
    password: String,
    email: String,
    isLoading: Boolean,
    // yeni parametreler:
    emailError: String?,
    passwordError: String?,
    formError: String?,
    canSubmit: Boolean,
    onToggleMode: () -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .widthIn(max = 360.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Üst toggle (stil aynı, sadece no-op aktif)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isLogin) {
                Button(onClick = {}, enabled = !isLoading) { Text("Login") }
                OutlinedButton(onClick = { if (!isLoading) onToggleMode() }, enabled = !isLoading) { Text("Register") }
            } else {
                OutlinedButton(onClick = { if (!isLoading) onToggleMode() }, enabled = !isLoading) { Text("Login") }
                Button(onClick = {}, enabled = !isLoading) { Text("Register") }
            }
        }

        // Username (görsel hata istemiyorsun diye sabit bıraktım)
        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = { Text("Username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )

        if (!isLogin) {
            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text("Email") },
                singleLine = true,
                isError = emailError != null, // sadece stil
                trailingIcon = {
                    if (emailError != null) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Hata",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
        }

        // Password: görsel hata + visibility toggle (metin yok, layout kaymaz)
        var visible by remember { mutableStateOf(false) }

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            singleLine = true,
            isError = passwordError != null,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (passwordError != null) {
                        val tooltipState = rememberTooltipState(isPersistent = false)
                        val scope = rememberCoroutineScope()

                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                            state = tooltipState,
                            tooltip = {
                                PlainTooltip {
                                    Column {
                                        Text("Şifre gereksinimleri:", style = MaterialTheme.typography.labelLarge)
                                        Spacer(Modifier.height(4.dp))
                                        Text("• En az 8 karakter")
                                        Text("• En az 1 sayı + 1 özel karakter")
                                    }
                                }
                            }
                        ) {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        if (tooltipState.isVisible) tooltipState.dismiss() else tooltipState.show()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = "Şifre kuralları",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        Spacer(Modifier.width(8.dp))
                    }

                    IconButton(onClick = { visible = !visible }) {
                        Icon(
                            imageVector = if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (visible) "Gizle" else "Göster"
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )


        Spacer(Modifier.weight(1f)) // alanlar kaymasın

        // Alttaki sabit "hata bandı": tek satır, sabit yükseklik; klavye üstünde kalır
        FormErrorBar(message = formError)

        // Submit
        Button(
            onClick = onSubmit,
            enabled = canSubmit && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .imePadding() // klavye üstünde
        ) {
            Text(if (isLogin) "Login" else "Register")
        }
    }
}

/** Sabit yükseklikli, tek satırlık hata çubuğu — boşken alpha=0 ile görünmez. */
@Composable
fun FormErrorBar(message: String?) {
    val hasError = !message.isNullOrBlank()
    val alpha = if (hasError) 1f else 0f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .imePadding()
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = message ?: "",
            modifier = Modifier.alpha(alpha),
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFD32F2F)
//            color = MaterialTheme.colorScheme.error
        )
    }
}
