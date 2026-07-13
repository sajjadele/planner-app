# Compose Mapping — review-animations (Jetpack Compose)

Companion to `SKILL.md` + `STANDARDS.md`. Maps the web-oriented review standards to
**Jetpack Compose / Android** so findings can cite concrete Compose fixes (file:line).

## The Ten Standards → Compose checks
| # | Standard | Compose red flag | Fix |
| --- | --- | --- | --- |
| 1 | Justified motion | `AnimatedVisibility`/transition on a 100+/day action (e.g. tab hot-swap, checkbox) | delete the animation |
| 2 | Frequency | animating something seen constantly | `tween(0)` or remove |
| 3 | Responsive easing | `FastOutLinearInEasing` on enter, or no easing arg | `CubicBezierEasing(0.23f,1f,0.32f,1f)` |
| 4 | Sub-300ms | `tween(400)`+ on a UI element | `tween(200–240)` |
| 5 | Origin/physical | enter from `scaleIn(0)` | `scaleIn(0.95) + fadeIn()` |
| 6 | Interruptible | non-retargetable jumps via `Animatable.snapTo` on rapid toggles | `animate*AsState` / `spring` |
| 7 | GPU-only | animating `height`/`width`/`padding` for motion | `graphicsLayer` transform / alpha, or `expandVertically` |
| 8 | A11y reduced-motion | no `rememberReduceMotion()` gate on movement | instant `tween(0)` when reduced |
| 9 | Asymmetric timing | equal enter/exit duration on press-release | slower press, snappy release |
| 10 | Cohesion | bounce/overshoot where app is crisp | match fast-out family |

## Escalation triggers → Compose equivalents
- `transition: all` → animating unbounded `Modifier` params; be explicit (transform/alpha only).
- `scale(0)` / pure-fade → `scaleIn(0)` / `fadeIn()` with no initial scale → `scaleIn(0.95)+fadeIn`.
- `ease-in` on UI → `FastOutLinearInEasing` for an enter → switch to ease-out curve.
- keyboard/high-freq animation → any motion on checkbox/tab/keyboard action → remove.
- `transform-origin: center` popover → for Compose popups use `Alignment` anchored to trigger; a centered `Dialog` is exempt.
- keyframes on rapid triggers → `Animatable` restart loops on toggles → `animate*AsState`.
- layout-prop animation → `Modifier.height` tweened → use `AnimatedVisibility` clip or `graphicsLayer`.
- missing reduced-motion → movement without `rememberReduceMotion()` gate.
- symmetric press timing → `Button` press scale same speed both ways → snap release.
- everything-at-once → no stagger on a list of entering items → 30–80ms `delay` between items.

## Verdict vocabulary (same as SKILL.md)
- **Block** when: feel-breaking regression, motion on high-freq/keyboard action,
  `scale(0)`/`ease-in` on UI, or non-GPU motion with an easy GPU fix.
- **Approve** when: no feel-breaking regressions, nothing that should be deleted,
  durations/easing in bounds, interruptibility handled, reduced-motion respected.

## Android reduced-motion helper (cite this in findings)
```kotlin
fun rememberReduceMotion(): Boolean {
  val c = LocalContext.current
  return remember { runCatching {
    Settings.Global.getFloat(c.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
  }.getOrDefault(false) }
}
```
Use `if (reduce) tween(0) else tween(240, easing = CubicBezierEasing(0.23f,1f,0.32f,1f))`.
