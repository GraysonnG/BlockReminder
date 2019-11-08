package blockreminder.utils

import com.megacrit.cardcrawl.core.Settings

fun Int.scale(): Float = this * Settings.scale
fun Float.scale(): Float = this * Settings.scale