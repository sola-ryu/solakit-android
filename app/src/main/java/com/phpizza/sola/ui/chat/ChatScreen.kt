package com.phpizza.sola.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phpizza.sola.ui.chat.components.ChatDrawerContent
import com.phpizza.sola.ui.chat.components.ChatInputBar
import com.phpizza.sola.ui.chat.components.ChatMessageItem
import com.phpizza.sola.ui.chat.components.SuggestionChips
import com.phpizza.sola.ui.theme.SolaTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var topMenuExpanded by remember { mutableStateOf(false) }

    // Synchronize drawer state with ViewModel state
    LaunchedEffect(uiState.isDrawerOpen) {
        if (uiState.isDrawerOpen && drawerState.isClosed) {
            drawerState.open()
        } else if (!uiState.isDrawerOpen && drawerState.isOpen) {
            drawerState.close()
        }
    }

    // Auto-scroll to bottom on new message
    LaunchedEffect(uiState.currentSessionId, uiState.messages.size, uiState.messages.lastOrNull()?.content?.length) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ChatDrawerContent(
                sessions = uiState.sessions,
                currentSessionId = uiState.currentSessionId,
                onSessionSelect = { sessionId ->
                    viewModel.selectSession(sessionId)
                    coroutineScope.launch { drawerState.close() }
                },
                onNewChat = {
                    viewModel.createNewSession()
                    coroutineScope.launch { drawerState.close() }
                },
                onRenameSession = { session ->
                    viewModel.startRenameSession(session)
                },
                onDeleteSession = { sessionId ->
                    viewModel.deleteSession(sessionId)
                }
            )
        },
        modifier = modifier
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = uiState.activeSession?.title ?: "Sola AI Chat",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                if (drawerState.isClosed) drawerState.open() else drawerState.close()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open Session Drawer"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.createNewSession() }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Chat Session"
                            )
                        }

                        Box {
                            IconButton(onClick = { topMenuExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Session Options"
                                )
                            }

                            DropdownMenu(
                                expanded = topMenuExpanded,
                                onDismissRequest = { topMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Rename Chat") },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    onClick = {
                                        topMenuExpanded = false
                                        uiState.activeSession?.let { viewModel.startRenameSession(it) }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Clear History") },
                                    leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    onClick = {
                                        topMenuExpanded = false
                                        viewModel.clearCurrentSessionHistory()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete Chat", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) },
                                    onClick = {
                                        topMenuExpanded = false
                                        uiState.currentSessionId?.let { viewModel.deleteSession(it) }
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                ChatInputBar(
                    text = uiState.inputText,
                    onTextChanged = viewModel::onInputTextChanged,
                    onSendMessage = { viewModel.sendMessage() },
                    onStopGeneration = viewModel::stopGeneration,
                    isGenerating = uiState.isGenerating
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (uiState.isLoadingMessages) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (uiState.messageLoadError != null) {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(uiState.messageLoadError.orEmpty(), color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = viewModel::retryMessages) { Text("Retry") }
                    }
                } else if (uiState.messages.isEmpty()) {
                    // Show Starter Suggestions when session is empty
                    SuggestionChips(
                        onSuggestionClick = { prompt ->
                            viewModel.sendMessage(prompt)
                        },
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    // Conversation Message List
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.messages, key = { it.id }) { message ->
                            ChatMessageItem(message = message)
                        }
                    }
                }

                // Generating Overlay / Status Banner
                AnimatedVisibility(
                    visible = uiState.isGenerating && uiState.messages.none { it.isStreaming },
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = MaterialTheme.shapes.medium
                            )
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI is thinking...",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }

    // Rename Session Dialog
    uiState.renamingSession?.let { session ->
        var renameText by remember(session) { mutableStateOf(session.title) }

        AlertDialog(
            onDismissRequest = { viewModel.cancelRenameSession() },
            title = { Text("Rename Chat Session") },
            text = {
                Column {
                    OutlinedTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        singleLine = true,
                        label = { Text("Session Title") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmRenameSession(renameText) },
                    enabled = renameText.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelRenameSession() }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ChatScreenPreview() {
    SolaTheme {
        ChatScreen()
    }
}
