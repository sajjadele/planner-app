package com.example.ui.screens.components

import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.LayoutDirection

class ConvexBottomBarShape(
    private val fabRadiusPx: Float,
    private val barHeightPx: Float,
    private val transitionHalfWidthPx: Float
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): Outline {
        return Outline.Generic(createFullBarPath(size))
    }

    // این متد برای رنگ‌آمیزی کل بدنه نوار استفاده می‌شود و مسیر را کاملاً می‌بندد
    private fun createFullBarPath(size: Size): Path {
        val w = size.width
        val hMax = size.height
        return crestPath(size).apply {
            // مسیر را از لبه‌های راست و پایین دور می‌زنیم تا یک شکل بسته برای رنگ‌آمیزی ایجاد شود
            lineTo(w, hMax)
            lineTo(0f, hMax)
            close()
        }
    }

    // این متد فقط خط بالایی نوار را برای کشیدن Highlight (لبه روشن) ترسیم می‌کند
    fun crestPath(size: Size): Path {
        val w = size.width
        val cx = w / 2f
        
        val r = fabRadiusPx
        
        // ── اصلاحات اصلی برای بلند و نرم شدن کوه ──
        val barTop = 50f   // خط صاف نوار را پایین‌تر می‌آوریم تا فضا باز شود
        val h = 48f        // عمق و ارتفاع غوص (کوه) را تقریباً دو برابر می‌کنیم تا کاملاً بالا بیاید
        val tw = r * 2.8f  // دامنه کوه را باز هم عریض‌تر می‌کنیم تا شیب صعود و فرود فوق‌العاده نرم و بی‌نقص شود

        return Path().apply {
            moveTo(0f, barTop)
            lineTo(cx - tw, barTop)

            // صعود فوق‌العاده ملایم به سمت قله
            cubicTo(
                x1 = cx - tw + (tw - r) * 0.5f, y1 = barTop,
                x2 = cx - r * 1.3f, y2 = barTop - h * 0.2f,
                x3 = cx - r, y3 = barTop - h * 0.6f
            )

            // دور زدن کاملاً کروی و نرم سقف قله (بالای دکمه)
            cubicTo(
                x1 = cx - r * 0.5f, y1 = barTop - h,
                x2 = cx - r * 0.3f, y2 = barTop - h,
                x3 = cx, y3 = barTop - h
            )

            // حرکت به سمت پایین از سمت راست قله
            cubicTo(
                x1 = cx + r * 0.3f, y1 = barTop - h,
                x2 = cx + r * 0.5f, y2 = barTop - h,
                x3 = cx + r, y3 = barTop - h * 0.6f
            )

            // فرود ملایم روی خط افقی نوار
            cubicTo(
                x1 = cx + r * 1.3f, y1 = barTop - h * 0.2f,
                x2 = cx + tw - (tw - r) * 0.5f, y2 = barTop,
                x3 = cx + tw, y3 = barTop
            )

            lineTo(w, barTop)
        }
    }
}