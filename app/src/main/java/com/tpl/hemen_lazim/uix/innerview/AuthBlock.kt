package com.tpl.hemen_lazim.uix.innerview

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.tpl.hemen_lazim.R
import com.tpl.hemen_lazim.model.AuthUiState
import com.tpl.hemen_lazim.ui.theme.Spacing

/**
 * Giriş / kayıt formu: üstte mod seçimi, alanlar ve gönder. Hatalar ilgili alanın altında;
 * şifre kuralı kayıt modunda baştan görünür (AuthValidator ile aynı kural).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthBlock(
    ui: AuthUiState,
    onModeChange: (isLogin: Boolean) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var passwordVisible by remember { mutableStateOf(false) }
    val enabled = !ui.isLoading

    Column(
        modifier = modifier.widthIn(max = 420.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.m)
    ) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = ui.isLogin,
                onClick = { onModeChange(true) },
                enabled = enabled,
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) { Text(stringResource(R.string.auth_login)) }
            SegmentedButton(
                selected = !ui.isLogin,
                onClick = { onModeChange(false) },
                enabled = enabled,
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) { Text(stringResource(R.string.auth_register)) }
        }

        OutlinedTextField(
            value = ui.username,
            onValueChange = onUsernameChange,
            label = { Text(stringResource(R.string.auth_username)) },
            singleLine = true,
            enabled = enabled,
            isError = ui.usernameError != null,
            supportingText = ui.usernameError?.let { error -> { Text(stringResource(error)) } },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            modifier = Modifier.fillMaxWidth()
        )

        if (!ui.isLogin) {
            OutlinedTextField(
                value = ui.email,
                onValueChange = onEmailChange,
                label = { Text(stringResource(R.string.auth_email)) },
                singleLine = true,
                enabled = enabled,
                isError = ui.emailError != null,
                supportingText = ui.emailError?.let { error -> { Text(stringResource(error)) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                modifier = Modifier.fillMaxWidth()
            )
        }

        OutlinedTextField(
            value = ui.password,
            onValueChange = onPasswordChange,
            label = { Text(stringResource(R.string.auth_password)) },
            singleLine = true,
            enabled = enabled,
            isError = ui.passwordError != null,
            supportingText = when {
                ui.passwordError != null -> {
                    { Text(stringResource(ui.passwordError)) }
                }
                // Kuralı hata almadan önce göster: eski ipucu özel karakter istiyordu, kural istemiyor
                !ui.isLogin -> {
                    { Text(stringResource(R.string.auth_password_rule)) }
                }
                else -> null
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = stringResource(
                            if (passwordVisible) R.string.auth_hide_password else R.string.auth_show_password
                        )
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                if (ui.canSubmit) onSubmit()
            }),
            modifier = Modifier.fillMaxWidth()
        )

        if (ui.formError != null) {
            Text(
                text = stringResource(ui.formError),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            onClick = onSubmit,
            enabled = ui.canSubmit && !ui.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
        ) {
            if (ui.isLoading) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(Spacing.s))
            }
            Text(stringResource(if (ui.isLogin) R.string.auth_login else R.string.auth_register))
        }
    }
}
