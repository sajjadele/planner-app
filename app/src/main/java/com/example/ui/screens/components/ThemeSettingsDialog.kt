package com.example.ui.screens.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.core.preferences.ThemeMode
import com.example.ui.theme.*
import com.example.BuildConfig

@Composable
fun ThemeSettingsDialog(
    currentTheme: ThemeMode,
    onSelectTheme: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    var showMirrorTest by remember { mutableStateOf(false) }
    if (BuildConfig.DEBUG && showMirrorTest) {
        MirrorTestDialog(onDismiss = { showMirrorTest = false })
    }
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (LocalIsDarkTheme.current) DarkSurfaceVariant else Color.White
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .then(
                    if (!LocalIsDarkTheme.current) {
                        Modifier.border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(24.dp))
                    } else Modifier
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // Icon header
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = AccentPurple,
                    modifier = Modifier.size(32.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "تنظیمات پوسته",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (LocalIsDarkTheme.current) DarkTextPrimary else TextPrimary
                )

                Text(
                    text = "حالت نمایش برنامه را انتخاب کنید",
                    fontSize = 12.sp,
                    color = if (LocalIsDarkTheme.current) DarkTextTertiary else TextTertiary,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Theme options
                ThemeOption.entries.forEach { option ->
                    val isSelected = when (option) {
                        ThemeOption.LIGHT -> currentTheme == ThemeMode.LIGHT
                        ThemeOption.DARK -> currentTheme == ThemeMode.DARK
                        ThemeOption.SYSTEM -> currentTheme == ThemeMode.SYSTEM
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                onSelectTheme(
                                    when (option) {
                                        ThemeOption.LIGHT -> ThemeMode.LIGHT
                                        ThemeOption.DARK -> ThemeMode.DARK
                                        ThemeOption.SYSTEM -> ThemeMode.SYSTEM
                                    }
                                )
                            }
                            .background(
                                if (isSelected)
                                    if (LocalIsDarkTheme.current) DarkAccentPurple.copy(alpha = 0.15f)
                                    else AccentPurple.copy(alpha = 0.08f)
                                else Color.Transparent,
                                RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Radio indicator
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .then(
                                    if (isSelected) {
                                        Modifier
                                            .clip(RoundedCornerShape(11.dp))
                                            .background(
                                                if (LocalIsDarkTheme.current) DarkAccentPurple else AccentPurple,
                                                RoundedCornerShape(11.dp)
                                            )
                                    } else {
                                        Modifier
                                            .clip(RoundedCornerShape(11.dp))
                                            .border(
                                                BorderStroke(
                                                    2.dp,
                                                    if (LocalIsDarkTheme.current) DarkTextTertiary else TextTertiary
                                                ),
                                                RoundedCornerShape(11.dp)
                                            )
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.White)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = option.title,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (LocalIsDarkTheme.current) DarkTextPrimary else TextPrimary
                            )
                            Text(
                                text = option.subtitle,
                                fontSize = 11.sp,
                                color = if (LocalIsDarkTheme.current) DarkTextTertiary else TextTertiary,
                                modifier = Modifier.padding(top = 1.dp)
                            )
                        }

                        if (isSelected) {
                            Text(
                                text = "✓",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (LocalIsDarkTheme.current) DarkAccentPurple else AccentPurple
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Developer-only: Mirror Testing (DEBUG builds only)
                if (BuildConfig.DEBUG) {
                    Button(
                        onClick = { showMirrorTest = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6D28D9)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ابزارهای توسعه‌دهنده", color = Color.White, fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Close button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("تایید", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private enum class ThemeOption(val title: String, val subtitle: String) {
    LIGHT("حالت روشن", "پس‌زمینه روشن با رنگ‌های بنفش"),
    DARK("حالت تاریک", "پس‌زمینه تیره مناسب شب"),
    SYSTEM("هماهنگ با سیستم", "بر اساس تنظیمات دستگاه شما")
}
