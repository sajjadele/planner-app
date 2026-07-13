package com.example.ui.onboarding

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.RTL
import kotlinx.coroutines.delay

@Composable
fun OnboardingTaskScreen(
    goalTitle: String,
    taskTitle: String,
    linkedGoalTitle: String?,
    onTaskTitleChange: (String) -> Unit,
    onSelectGoal: (String?) -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
    isSubmitting: Boolean,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(300); focusRequester.requestFocus() }

    val isLinked = linkedGoalTitle != null
    var showDropdown by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))

        // ── Floating Goal card ──
        OnboardingGoalCard(title = goalTitle, linked = isLinked)

        Spacer(Modifier.height(2.dp))
        ConnectorLine(linked = isLinked, modifier = Modifier.height(24.dp))
        Spacer(Modifier.height(2.dp))

        // ── Connector node — opens the real production DropdownMenu picker ──
        Box(contentAlignment = Alignment.Center) {
            ConnectorNode(
                linked = isLinked,
                onTap = { showDropdown = true }
            )
            DropdownMenu(
                expanded = showDropdown,
                onDismissRequest = { showDropdown = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🎯 ", fontSize = 14.sp)
                            Text(goalTitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    onClick = { onSelectGoal(goalTitle); showDropdown = false }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            "بدون هدف",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    onClick = { onSelectGoal(null); showDropdown = false }
                )
            }
        }

        Spacer(Modifier.height(2.dp))
        ConnectorLine(linked = isLinked, modifier = Modifier.height(24.dp))
        Spacer(Modifier.height(12.dp))

        // ── Task input ──
        Text(
            text = "حالا یک قدم کوچک برای امروز بردار:",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = taskTitle,
            onValueChange = onTaskTitleChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            placeholder = { Text("${RTL}مثلاً: ۱۵ دقیقه قدم زدن") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                cursorColor = MaterialTheme.colorScheme.primary
            )
        )

        Spacer(Modifier.weight(1f))

        // CTA disabled until the explicit linking action is completed.
        val interactionSource = remember { MutableInteractionSource() }
        Button(
            onClick = onSubmit,
            enabled = taskTitle.isNotBlank() && isLinked && !isSubmitting,
            interactionSource = interactionSource,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .pressScale(interactionSource),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    "هدفم را بساز",
                    color = MaterialTheme.colorScheme.surface,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** Floating goal card. Elevates + tints when the task is linked to it. */
@Composable
private fun OnboardingGoalCard(title: String, linked: Boolean, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (linked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
        tonalElevation = if (linked) 6.dp else 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🎯", fontSize = 22.sp)
            Spacer(Modifier.width(10.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

/** The delightful connector node. Pulses (glow) until linked, then becomes "✓ متصل شد". */
@Composable
private fun ConnectorNode(linked: Boolean, onTap: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val reduceMotion = rememberReduceMotion()

    val transition = rememberInfiniteTransition(label = "nodeGlow")
    val pulse by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable<Float>(
            tween(900, easing = OnboardingMotion.EaseOut),
            RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val glowAlpha = if (linked || reduceMotion) 0f else pulse

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f * glowAlpha),
            modifier = Modifier.matchParentSize()
        ) {}
        Surface(
            onClick = onTap,
            interactionSource = interactionSource,
            shape = RoundedCornerShape(22.dp),
            color = if (linked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = if (linked) 1f else 0.55f)
            ),
            modifier = Modifier.pressScale(interactionSource)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 11.dp)
            ) {
                Text(
                    text = if (linked) "✓ متصل شد" else "🔗 متصل کن به هدف",
                    color = if (linked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

/** Vertical connector that "draws" (scaleY 0→1) once the link is made. */
@Composable
private fun ConnectorLine(linked: Boolean, modifier: Modifier = Modifier) {
    val reduceMotion = rememberReduceMotion()
    val progress by animateFloatAsState(
        targetValue = if (linked) 1f else 0f,
        animationSpec = tween(
            if (reduceMotion) 0 else 220,
            easing = OnboardingMotion.EaseOut
        ),
        label = "connectLine"
    )
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .graphicsLayer {
                    scaleY = progress
                    transformOrigin = TransformOrigin(0.5f, 0f)
                }
                .background(MaterialTheme.colorScheme.primary)
        ) {}
    }
}
