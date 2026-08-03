package com.example.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.onboarding.OnboardingStep
import com.example.core.onboarding.OnboardingViewModel

private val STEP_ORDER = listOf(
    OnboardingStep.Welcome,
    OnboardingStep.Goal,
    OnboardingStep.Task,
    OnboardingStep.Future
)

@Composable
fun OnboardingHost(
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = viewModel(),
    onFinish: (Int) -> Unit
) {
    val step by viewModel.step.collectAsState()
    val goalTitle by viewModel.goalTitle.collectAsState()
    val taskTitle by viewModel.taskTitle.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val reduceMotion = rememberReduceMotion()

    // Force RTL for the whole onboarding surface (Persian-first).
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // safeDrawing lifts the CTA above the system navigation bar / IME on
            // all screen form factors (edge-to-edge is enabled in MainActivity).
            AnimatedContent(
                targetState = step,
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
                transitionSpec = {
                    // Onboarding is rare/first-time -> directional delight is welcome.
                    // Strong ease-out, sub-300ms, reduced-motion aware (instant when set).
                    val duration = if (reduceMotion) 0 else OnboardingMotion.STEP_DURATION
                    val easing = OnboardingMotion.EaseOut
                    val forward =
                        STEP_ORDER.indexOf(targetState) > STEP_ORDER.indexOf(initialState)
                    if (forward) {
                        (fadeIn(tween(duration, easing = easing)) +
                            slideInHorizontally(tween(duration, easing = easing)) { it / 2 })
                            .togetherWith(
                                fadeOut(tween(duration, easing = easing)) +
                                    slideOutHorizontally(tween(duration, easing = easing)) { -it / 2 }
                            )
                    } else {
                        (fadeIn(tween(duration, easing = easing)) +
                            slideInHorizontally(tween(duration, easing = easing)) { -it / 2 })
                            .togetherWith(
                                fadeOut(tween(duration, easing = easing)) +
                                    slideOutHorizontally(tween(duration, easing = easing)) { it / 2 }
                            )
                    }
                },
                label = "OnboardingSteps"
            ) { currentStep ->
                when (currentStep) {
                    OnboardingStep.Welcome -> OnboardingWelcomeScreen(
                        onNext = viewModel::next,
                        modifier = Modifier.fillMaxSize()
                    )
                    OnboardingStep.Goal -> OnboardingGoalScreen(
                        goalTitle = goalTitle,
                        onGoalTitleChange = viewModel::onGoalTitleChange,
                        onNext = viewModel::next,
                        onBack = viewModel::back,
                        modifier = Modifier.fillMaxSize()
                    )
                    OnboardingStep.Task -> OnboardingTaskScreen(
                        goalTitle = goalTitle,
                        taskTitle = taskTitle,
                        onTaskTitleChange = viewModel::onTaskTitleChange,
                        onBack = viewModel::back,
                        onSubmit = { viewModel.next() },
                        isSubmitting = isSubmitting,
                        modifier = Modifier.fillMaxSize()
                    )
                    OnboardingStep.Future -> OnboardingFutureScreen(
                        onBack = viewModel::back,
                        onFinish = { viewModel.finish(onFinish) },
                        isSubmitting = isSubmitting,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}