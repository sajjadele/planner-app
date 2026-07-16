package com.example.domain.mirror

object MirrorEngine {

    fun render(signal: MirrorSignal): MirrorInsight = when (signal.type) {
        MirrorSignalType.BOULDER -> MirrorInsight(
            title = "تسک مسدودشده",
            message = "این فعالیت چندبار جابجا شده. شاید کم‌تر و کوچک‌تر Split بشه.",
            relatedTaskId = signal.taskId
        )
        MirrorSignalType.GOAL_ATTENTION -> MirrorInsight(
            title = "هدف مورد نیاز توجه",
            message = "این هدف اخیراً حرکت کمی داشته. یک قدم کوچک امروز می‌تزینه کمک کنه.",
            relatedGoalId = signal.goalId
        )
        MirrorSignalType.INITIATOR_FINISHER -> MirrorInsight(
            title = "تعادل شروع و پایان",
            message = "اخیراً تسک زیاد ساخته شده و کمتر تمام شده. تمرکز روی تکمیل کارهای در حال انجام می‌تونه مؤثرتر باشه.",
            relatedGoalId = signal.goalId
        )
        MirrorSignalType.CONSISTENCY_DECAY -> MirrorInsight(
            title = "کاهش انتظام فعالیت",
            message = "فعالیت با هفته گذشته کمتر شده. تمام شدن حداقل یک تسک کوچک می‌تونه ریتم را بازگردونه.",
            relatedGoalId = signal.goalId
        )
    }
}
