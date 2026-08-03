package com.example.ui.onboarding

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.RTL
import kotlinx.coroutines.delay

/**
 * Step 3 — Goal → Task. Shows the relationship visually: the task always belongs
 * to the created goal. No picker, no "بدون هدف" — the connection is drawn, not selected.
 */
@Composable
fun OnboardingTaskScreen(
    goalTitle: String,
    taskTitle: String,
    onTaskTitleChange: (String) -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
    isSubmitting: Boolean,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(300); focusRequester.requestFocus() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(12.dp))
        OnboardingBackButton(onBack = onBack)
        Spacer(Modifier.height(16.dp))

        Text(
            text = "هر هدف، از چند قدمِ کوچک تشکیل می‌شود.",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))

        // ── Floating Goal card (always matched to the entered goal) ──
        OnboardingGoalCard(title = goalTitle)

        // ── Animated connector that draws (scaleY 0→1) on entry ──
        Spacer(Modifier.height(4.dp))
        ConnectorLine(modifier = Modifier.height(28.dp))
        Spacer(Modifier.height(4.dp))

        // ── Node: visual statement that the task belongs to the goal ──
        ConnectorNode(modifier = Modifier)

        Spacer(Modifier.height(4.dp))
        ConnectorLine(modifier = Modifier.height(10.dp))
        Spacer(Modifier.height(20.dp))

        // ── Task input ──
        Text(
            text = "اولین قدمِ تو چیست؟",
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
            placeholder = { Text("${RTL}مثلاً: ۱۵ دقیقه مطالعه") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                cursorColor = MaterialTheme.colorScheme.primary
            )
        )

        Spacer(Modifier.weight(1f))

        Text(
            text = "هدف، مسیر را مشخص می‌کند؛\nهر قدمِ تو را جلوتر می‌برد.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))

        val interactionSource = remember { MutableInteractionSource() }
        Button(
            onClick = onSubmit,
            enabled = taskTitle.isNotBlank() && !isSubmitting,
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

/** Floating goal card — tinted to signal it is the anchor of the task below. */
@Composable
private fun OnboardingGoalCard(title: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
        tonalElevation = 4.dp,
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

/** Static "connected" node — communicates the binding, no selection needed. */
@Composable
private fun ConnectorNode(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "گام‌های این هدف",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

/** Vertical connector that "draws" (scaleY 0→1) once on entry. */
@Composable
private fun ConnectorLine(modifier: Modifier = Modifier) {
    val reduceMotion = rememberReduceMotion()
    var drawn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        drawn = true
    }
    val progress by animateFloatAsState(
        targetValue = if (drawn) 1f else 0f,
        animationSpec = tween(
            if (reduceMotion) 0 else OnboardingMotion.CONNECT_DRAW_MS,
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