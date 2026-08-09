package com.dietcoach.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.dietcoach.app.data.model.ChatMessageEntity
import com.dietcoach.app.ui.AppUiState

@Composable
fun ChatScreen(
    state: AppUiState,
    chat: List<ChatMessageEntity>,
    streamingAssistant: String?,
    onSend: (String) -> Unit,
    onClear: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val streaming = streamingAssistant != null

    LaunchedEffect(chat.size, streamingAssistant?.length) {
        val last = chat.size + if (streaming) 1 else 0
        if (last > 0) {
            listState.animateScrollToItem(last - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("AI 助手 · Qwen-Max", style = MaterialTheme.typography.headlineMedium)
        Text(
            "流式回答 · 公式支持 LaTeX（\$...\$ / \$\$...\$\$）。涉及训练说「帮我记录」可入库。${state.effectiveWeightHint()}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = onClear, enabled = !streaming) { Text("清空对话") }

        if (state.busy && streamingAssistant.isNullOrEmpty()) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (chat.isEmpty() && !streaming) {
                item {
                    Text(
                        "试试：写出 BMR 的 Mifflin 公式并用 LaTeX 展示 / 中午汉堡鸡翅帮我记录",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(chat, key = { it.id }) { msg ->
                ChatBubble(
                    text = msg.content,
                    mine = msg.role == "user",
                    streaming = false
                )
            }
            if (streaming) {
                item(key = "streaming") {
                    ChatBubble(
                        text = streamingAssistant.orEmpty().ifEmpty { "…" },
                        mine = false,
                        streaming = true
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                label = { Text("输入问题") },
                minLines = 1,
                maxLines = 4,
                enabled = !streaming
            )
            Button(
                onClick = {
                    val text = input.trim()
                    if (text.isNotEmpty()) {
                        onSend(text)
                        input = ""
                    }
                },
                enabled = !state.busy && input.isNotBlank()
            ) { Text(if (streaming) "生成中" else "发送") }
        }
    }
}

@Composable
private fun ChatBubble(
    text: String,
    mine: Boolean,
    streaming: Boolean
) {
    val bg = if (mine) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (mine) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(14.dp))
                .background(bg)
                .padding(12.dp)
        ) {
            if (mine) {
                Text(text = text, color = fg, style = MaterialTheme.typography.bodyLarge)
            } else {
                ChatMessageBody(
                    text = text,
                    streaming = streaming,
                    textColor = fg
                )
            }
        }
    }
}
