package com.wordlight.game.ui.common

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

enum class HapticType { TICK, CONFIRM, REJECT, SUCCESS }

/** View-based haptics: no VIBRATE permission needed and it respects the system haptics setting. */
fun View.performHaptic(type: HapticType) {
    val modern = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    val constant = when (type) {
        HapticType.TICK -> HapticFeedbackConstants.CLOCK_TICK
        HapticType.CONFIRM -> if (modern) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY
        HapticType.REJECT -> if (modern) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS
        HapticType.SUCCESS -> if (modern) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS
    }
    performHapticFeedback(constant)
}

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
