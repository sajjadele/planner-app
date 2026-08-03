package com.example.ui.screens.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.mirror.MirrorInsight
import com.example.ui.components.VisionText
import com.example.ui.theme.*

@Composable
fun MirrorTestDialog(
    onDismiss: () -> Unit,
    viewModel: DebugMirrorViewModel = viewModel()
) {
    val result by viewModel.lastResult.collectAsState()
    val status by viewModel.status.collectAsState()
    val scenario by viewModel.lastScenario.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (LocalIsDarkTheme.current) DarkSurfaceVariant else Color.White
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Mirror Testing",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (LocalIsDarkTheme.current) DarkTextPrimary else TextPrimary
                )
                VisionText(
                    text = "ابزار توسعه‌دهنده — فقط در نسخه DEBUG",
                    fontSize = 11.sp,
                    color = if (LocalIsDarkTheme.current) DarkTextTertiary else TextTertiary,
                    modifier = Modifier.padding(top = 2.dp).fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                val buttons = listOf(
                    "شبیه‌سازی ۷ روز سابقه" to { viewModel.simulateHistory() },
                    "سناریو Boulder" to { viewModel.createBoulderScenario() },
                    "سناریو Goal Attention" to { viewModel.createGoalAttentionScenario() },
                    "سناریو Initiator/Finisher" to { viewModel.createInitiatorFinisherScenario() },
                    "سناریو Consistency Decay" to { viewModel.createConsistencyDecayScenario() }
                )
                buttons.forEach { (label, action) ->
                    Button(
                        onClick = action,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        VisionText(label, color = Color.White, fontWeight = FontWeight.Medium)
                    }
                }

                Button(
                    onClick = { viewModel.clearTestData() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    VisionText("پاکسازی داده‌های تست Mirror", color = Color.White, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(12.dp))

                status?.let {
                    VisionText(
                        text = it,
                        fontSize = 11.sp,
                        color = if (LocalIsDarkTheme.current) DarkTextTertiary else TextTertiary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (result.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            scenario?.let {
                                VisionText(
                                    text = "Scenario: $it",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                            VisionText(
                                text = "Mirror detected (${result.size}):",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            result.forEach { insight: MirrorInsight ->
                                VisionText(
                                    text = "• ${insight.title}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                VisionText(
                                    text = insight.message,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                                    lineHeight = 16.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    VisionText("بستن", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
