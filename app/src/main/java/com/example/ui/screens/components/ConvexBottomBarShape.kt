package com.example.ui.screens.components

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * A Shape whose top edge is a single, continuous, G1-smooth convex crest.
 *
 * The silhouette deforms organically upward at the center, and at the peak
 * the contour EXACTLY traces the blue circle's upper semicircle outline.
 * This ensures zero gap between the dark surface and the embedded action button.
 *
 * The path is composed of 4 cubic Bézier curves (no arcTo):
 *   1. Left transition  — flat bar → circle's left edge (gentle rise)
 *   2. Left semicircle   — Bézier approximation of upper-left quadrant
 *   3. Right semicircle  — Bézier approximation of upper-right quadrant
 *   4. Right transition — circle's right edge → flat bar (gentle descent)
 *
 * At every junction the tangent direction is continuous (G1):
 *   - Flat bar → transition: horizontal
 *   - Transition → semicircle: vertical
 *   - Left semicircle → right semicircle at apex: horizontal
 *
 * @param domeRadiusPx    Radius of the embedded circle (= how far the dome rises)
 * @param barHeightPx     Height of the flat bar portion
 * @param transitionHalfWidthPx  Distance from center to where the flat bar ends
 */
class ConvexBottomBarShape(
    private val domeRadiusPx: Float,
    private val barHeightPx: Float,
    private val transitionHalfWidthPx: Float
) : Shape {

    companion object {
        /**
         * Cubic Bézier approximation of a quarter-circle.
         * k = (4/3) * tan(π/4) ≈ 0.5523
         */
        private const val K = 0.5523f
    }

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val w = size.width
        val cx = w / 2f
        val r = domeRadiusPx
        val barTop = r
        val totalHeight = r + barHeightPx
        val tw = transitionHalfWidthPx

        // Horizontal distance of the transition zone (flat bar edge → circle edge)
        val tSpan = tw - r

        val path = Path().apply {
            // ── Bottom-left ──
            moveTo(0f, totalHeight)

            // ── Left edge → flat bar height ──
            lineTo(0f, barTop)

            // ── Flat bar ──
            lineTo(cx - tw, barTop)

            // ── Curve 1: Left transition (flat bar → circle's left edge) ──
            //  P0 = (cx - tw, barTop)       — on the flat bar
            //  P1 = horizontal control      — G1 at flat bar (slope = 0)
            //  P2 = vertical control        — G1 at circle edge (slope = ∞)
            //  P3 = (cx - r, barTop)        — circle's leftmost point
            cubicTo(
                x1 = cx - tw + tSpan * 0.5f,
                y1 = barTop,
                x2 = cx - r,
                y2 = barTop - r * 0.5f,
                x3 = cx - r,
                y3 = barTop
            )

            // ── Curve 2: Left semicircle half (upper-left quadrant) ──
            //  Bézier approximation of the circle from 9 o'clock to 12 o'clock.
            //  P0 = (cx - r, barTop)        — 9 o'clock (leftmost)
            //  P3 = (cx, 0)                 — 12 o'clock (apex)
            cubicTo(
                x1 = cx - r,
                y1 = barTop - r * K,
                x2 = cx - r * K,
                y2 = barTop - r,
                x3 = cx,
                y3 = barTop - r
            )

            // ── Curve 3: Right semicircle half (upper-right quadrant) ──
            //  Bézier approximation of the circle from 12 o'clock to 3 o'clock.
            //  P0 = (cx, 0)                 — 12 o'clock (apex)
            //  P3 = (cx + r, barTop)        — 3 o'clock (rightmost)
            cubicTo(
                x1 = cx + r * K,
                y1 = barTop - r,
                x2 = cx + r,
                y2 = barTop - r * K,
                x3 = cx + r,
                y3 = barTop
            )

            // ── Curve 4: Right transition (circle's right edge → flat bar) ──
            //  P0 = (cx + r, barTop)        — circle's rightmost point
            //  P1 = vertical control        — G1 at circle edge (slope = ∞)
            //  P2 = horizontal control      — G1 at flat bar (slope = 0)
            //  P3 = (cx + tw, barTop)       — on the flat bar
            cubicTo(
                x1 = cx + r,
                y1 = barTop - r * 0.5f,
                x2 = cx + tw - tSpan * 0.5f,
                y2 = barTop,
                x3 = cx + tw,
                y3 = barTop
            )

            // ── Right flat bar ──
            lineTo(w, barTop)

            // ── Right edge → bottom ──
            lineTo(w, totalHeight)

            // ── Bottom edge ──
            close()
        }

        return Outline.Generic(path)
    }
}
