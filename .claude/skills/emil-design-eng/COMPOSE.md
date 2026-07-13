# Compose Mapping — emil-design-eng (Jetpack Compose)

This project is **Jetpack Compose / Android**, not web/CSS. Below maps the skill's
principles to Compose APIs. Read the parent `SKILL.md` for the rationale; use this
for the *implementation*.

## Easing
| Skill says | Compose |
| --- | --- |
| `ease-out` / strong `cubic-bezier(0.23,1,0.32,1)` | `CubicBezierEasing(0.23f, 1f, 0.32f, 1f)` |
| `ease-in-out` `cubic-bezier(0.77,0,0.175,1)` | `CubicBezierEasing(0.77f, 0f, 0.175f, 1f)` |
| `ease` (hover/color) | `FastOutSlowInEasing` or `LinearEasing` |
| never `ease-in` on UI | avoid `FastOutLinearInEasing` for enter; the app already uses `FastOutSlowInEasing` for tab transitions — fine |

Define shared tokens once, e.g. `object Motion { val EaseOut = CubicBezierEasing(0.23f,1f,0.32f,1f); const val STEP = 240 }`.

## Duration (ms)
Press feedback 100–160 · popovers 125–200 · dropdowns 150–250 · modal/drawer 200–500.
**Rule: UI transitions < 300ms.** Use `tween(240, easing = Motion.EaseOut)`.

## Buttons must feel responsive (scale 0.97 on press)
Material3 `Button`/`TextButton` accept an `interactionSource`. Drive a subtle scale:
```kotlin
val interactionSource = remember { MutableInteractionSource() }
val pressed by interactionSource.collectIsPressedAsState()
val scale by animateFloatAsState(if (pressed) 0.97f else 1f, tween(120))
Button(onClick, interactionSource = interactionSource,
       modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }) { ... }
```

## Never animate from scale(0)
Enter from `scale(0.95)` + `alpha 0`. With `AnimatedVisibility`/`AnimatedContent`,
use `scaleIn(initialScale = 0.95) + fadeIn()` instead of `scaleIn(0)`.

## Interruptibility
- Prefer `Modifier.animate*AsState` / `Crossfade` / `AnimatedContent` (retargetable) over `Animatable` jumps.
- For gesture/spring motion use `spring(stiffness, damping)` (e.g. `spring(380f, 38f)`); keep `bounce` subtle (0.1–0.3).
- `AnimatedContent` is interruptible by default — good for screen swaps.

## GPU-only properties
Animate `graphicsLayer { translationX/scaleX/alpha/rotation* }` and `alpha` only.
Never animate `height`/`width`/`padding` for motion — for appearance changes use
`AnimatedVisibility` with `expandVertically`/`shrinkVertically` (clips, doesn't reflow layout each frame).

## Reduced motion (Android proxy)
Compose has no `prefers-reduced-motion` flag. Use the system "Remove animations"
accessibility setting:
```kotlin
fun rememberReduceMotion(): Boolean {
  val c = LocalContext.current
  return remember { runCatching {
    Settings.Global.getFloat(c.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
  }.getOrDefault(false) }
}
```
When `true`, pass `tween(0)` (instant) to every `animationSpec`, or skip movement and keep alpha only.

## Cohesion
This app's existing tab transitions use `FastOutSlowInEasing` fade. Keep onboarding
motion in the same fast-out family (use `CubicBezierEasing(0.23,1,0.32,1)`) so the
whole product feels cohesive. Don't bounce.
