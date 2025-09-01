package com.tpl.hemen_lazim.uix.innerview

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tpl.hemen_lazim.uix.viewmodel.AuthViewModel


@Composable
fun AuthBlock(
    isLogin: Boolean,
    username: String,
    password: String,
    email: String,
    isLoading: Boolean,
    onToggleMode: () -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier.widthIn(max = 360.dp).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isLogin) {
                Button(onClick = {}, enabled = !isLoading) { Text("Login") }
                OutlinedButton(onClick = { if (!isLoading) onToggleMode() }, enabled = !isLoading) { Text("Register") }
            } else {
                OutlinedButton(onClick = { if (!isLoading) onToggleMode() }, enabled = !isLoading) { Text("Login") }
                Button(onClick = {}, enabled = !isLoading) { Text("Register") }
            }
        }


        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = { Text("Username") },
            singleLine = true
        )


        if (!isLogin) {
            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text("Email") },
                singleLine = true
            )
        }

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            singleLine = true
        )

        Button(
            onClick = onSubmit,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isLogin) "Login" else "Register")
        }
    }
}



/*
@Composable
fun AuthBlock(
    isLogin: Boolean,
    username: String,
    password: String,
    isLoading: Boolean,
    onToggleMode: () -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (isLogin) "Didn't register?" else "Already registered?")
            TextButton(onClick = onToggleMode) {
                Text(if (isLogin) "Register" else "Login")
            }
        }

        TextField(
            value = username,
            onValueChange = onUsernameChange,
            label = { Text("Username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        TextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = onSubmit,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isLogin) "Login" else "Register")
        }
    }
}
        */