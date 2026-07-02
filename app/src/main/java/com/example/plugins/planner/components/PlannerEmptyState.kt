package com.example.plugins.planner.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PlannerEmptyState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "📝",
            fontSize = 48.sp
        )

        Spacer(modifier = androidx.compose.ui.Modifier.height(12.dp))

        Text(
            text = "برنامه‌ای برای امروز ثبت نشده است",
            color = Color(0xFF938F99),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = androidx.compose.ui.Modifier.height(4.dp))

        Text(
            text = "برای شروع، دکمه + را بزنید",
            color = Color(0xFF938F99),
            fontSize = 12.sp
        )
    }
}
