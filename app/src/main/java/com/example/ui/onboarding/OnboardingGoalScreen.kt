package com.example.ui.onboarding

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.RTL
import kotlinx.coroutines.delay

private val GOAL_IDEAS = listOf(
    "یادگیری زبان انگلیسی", "ورزش منظم", "مطالعه هفتگی",
    "پروژه کاری", "مدیتیشن روزانه", "پس‌انداز مالی"
)

/**
 * Step 2 — Goal. Establishes that the product starts from a Goal.
 * No task language here; only "what do you want to reach?"
 */
@Composable
fun OnboardingGoalScreen(
    goalTitle: String,
    onGoalTitleChange: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
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
        Spacer(Modifier.height(20.dp))

        Text(
            text = "همه‌چیز با یک هدف شروع می‌شود.",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            lineHeight = 30.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "به چه چیزی می‌خواهی برسی؟",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = goalTitle,
            onValueChange = onGoalTitleChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            placeholder = { Text("${RTL}مثلاً: یادگیری زبان انگلیسی") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                cursorColor = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = "ایده‌ها:",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(GOAL_IDEAS) { idea ->
                AssistChip(
                    onClick = { onGoalTitleChange(idea) },
                    label = { Text(idea, color = MaterialTheme.colorScheme.onSurface) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        }
        Spacer(Modifier.weight(1f))
        val interactionSource = remember { MutableInteractionSource() }
        Button(
            onClick = onNext,
            enabled = goalTitle.isNotBlank(),
            interactionSource = interactionSource,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .pressScale(interactionSource),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(
                "ادامه",
                color = MaterialTheme.colorScheme.surface,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}
