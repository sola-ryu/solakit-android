package com.phpizza.sola.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phpizza.sola.ui.chat.ChatScreen

@Composable
fun LoginScreen(viewModel: LoginViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    if (state.isSuccess) {
        ChatScreen()
        return
    }

    Box(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            com.phpizza.sola.ui.theme.HighContrastSetting()
            Text("Welcome to Sola", style = MaterialTheme.typography.headlineLarge)
            Text("Connect to your Solakit server to start chatting.",
                style = MaterialTheme.typography.bodyLarge)
            OutlinedTextField(
                value = state.serverUrl,
                onValueChange = viewModel::onServerUrlChanged,
                label = { Text("Endpoint URL") },
                placeholder = { Text("http://127.0.0.1:8382") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                singleLine = true,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.apiToken,
                onValueChange = viewModel::onApiTokenChanged,
                label = { Text("API token") },
                visualTransformation = if (state.isTokenVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    TextButton(onClick = viewModel::toggleTokenVisibility) {
                        Text(if (state.isTokenVisible) "Hide" else "Show")
                    }
                },
                singleLine = true,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = state.rememberConnection,
                    onCheckedChange = viewModel::onRememberConnectionChanged,
                    enabled = !state.isLoading
                )
                Text("Remember connection")
            }
            state.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium)
            }
            Button(
                onClick = viewModel::connect,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (state.isLoading) "Connecting…" else "Connect")
            }
        }
    }
}
