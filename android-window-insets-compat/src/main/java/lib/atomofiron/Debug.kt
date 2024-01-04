package lib.atomofiron

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import android.view.View.MeasureSpec

@SuppressLint("ResourceType")
fun View.id(): String {
    return when {
        id <= 0 -> "${javaClass.simpleName}($id)"
        else -> resources.getResourceEntryName(id)
    }
}

fun Any?.poop(s: String) {
    Log.e("atomofiron", "[${this?.javaClass?.simpleName}] $s")
}

fun Int.mode(): String = when (MeasureSpec.getMode(this)) {
    MeasureSpec.UNSPECIFIED -> "UNSPECIFIED"
    MeasureSpec.EXACTLY -> "EXACTLY"
    MeasureSpec.AT_MOST -> "AT_MOST"
    else -> "UNKNOWN"
}

fun Int.bits(
    prefix: String = "0x",
    zero: Char = '0',
    one: Char = '1',
    full: Boolean = false,
    oneDigitAtLeast: Boolean = true,
): String = toULong().bits(prefix, zero, one, 32, full, oneDigitAtLeast)

fun ULong.bits(
    prefix: String = "0x",
    zero: Char = '0',
    one: Char = '1',
    full: Boolean = false,
    oneDigitAtLeast: Boolean = true,
): String = bits(prefix, zero, one, 64, full, oneDigitAtLeast)

fun ULong.bits(
    prefix: String,
    zero: Char,
    one: Char,
    size: Int,
    full: Boolean,
    oneDigitAtLeast: Boolean,
): String {
    val builder = StringBuilder(size + prefix.length).append(prefix)
    var cursor = 1uL.shl(size.dec())
    var flag = full
    for (i in 0..<size) {
        val bit = (this and cursor) != 0uL
        flag = flag || bit
        if (flag) {
            builder.append(if (bit) one else zero)
        }
        cursor = cursor.shr(1)
    }
    if (oneDigitAtLeast && builder.length == prefix.length) {
        builder.append(zero)
    }
    return builder.toString()
}