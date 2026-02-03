/*
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package lib.atomofiron.insets

import android.view.View
import android.view.WindowInsets
import androidx.core.graphics.Insets
import androidx.core.view.DisplayCutoutCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.max


//                        /system-\
// 00000000000000000000000101010101
//            seeds: 30...987654321
internal const val LEGACY_LIMIT = Int.SIZE_BITS - 2
internal val LEGACY_RANGE = 1..LEGACY_LIMIT

class ExtendedWindowInsets internal constructor(
    internal val insets: Map<Int, InsetsValue>,
    private val hidden: TypeSet = TypeSet.Empty,
    val displayCutout: DisplayCutoutCompat?,
) {
    @Suppress("FunctionName")
    companion object {

        val Empty = ExtendedWindowInsets()

        fun Builder() = ExtendedBuilder()

        internal fun Builder(windowInsets: WindowInsetsCompat? = null): ExtendedBuilder {
            return ExtendedBuilder(windowInsets.getValues(), windowInsets.getHidden(), windowInsets?.displayCutout)
        }

        fun Builder(windowInsets: ExtendedWindowInsets? = null): ExtendedBuilder {
            return windowInsets?.run { ExtendedBuilder(insets, hidden, displayCutout) } ?: Builder()
        }
    }

    abstract class Type {
        companion object {
            val statusBars: TypeSet = WindowInsetsCompat.Type.statusBars().toTypeSet("statusBars")
            val navigationBars: TypeSet = WindowInsetsCompat.Type.navigationBars().toTypeSet("navigationBars")
            val captionBar: TypeSet = WindowInsetsCompat.Type.captionBar().toTypeSet("captionBar")
            val systemBars: TypeSet = statusBars + navigationBars + captionBar
            val displayCutout: TypeSet = WindowInsetsCompat.Type.displayCutout().toTypeSet("displayCutout")
            val barsWithCutout: TypeSet = systemBars + displayCutout
            val tappableElement: TypeSet = WindowInsetsCompat.Type.tappableElement().toTypeSet("tappableElement")
            val systemGestures: TypeSet = WindowInsetsCompat.Type.systemGestures().toTypeSet("systemGestures")
            val mandatorySystemGestures: TypeSet = WindowInsetsCompat.Type.mandatorySystemGestures().toTypeSet("mandatorySystemGestures")
            val ime: TypeSet = WindowInsetsCompat.Type.ime().toTypeSet("ime")

            internal val types = linkedSetOf(TypeSet.Empty, statusBars, navigationBars, captionBar, displayCutout, tappableElement, systemGestures, mandatorySystemGestures, ime)

            inline operator fun invoke(block: Companion.() -> TypeSet): TypeSet = this.block()
        }

        val statusBars = Companion.statusBars
        val navigationBars = Companion.navigationBars
        val captionBar = Companion.captionBar
        val systemBars = Companion.systemBars
        val displayCutout = Companion.displayCutout
        val barsWithCutout = Companion.barsWithCutout
        val tappableElement = Companion.tappableElement
        val systemGestures = Companion.systemGestures
        val mandatorySystemGestures = Companion.mandatorySystemGestures
        val ime = Companion.ime

        fun define(name: String, animated: Boolean = false) = TypeSet(name, animated = animated).also { types.add(it) }
    }

    constructor(windowInsets: WindowInsets?, view: View? = null)
            : this(windowInsets?.let { WindowInsetsCompat.toWindowInsetsCompat(it, view) })

    constructor(windowInsets: WindowInsetsCompat? = null)
            : this(windowInsets.getValues().toMap(), windowInsets.getHidden(), displayCutout = windowInsets?.displayCutout)

    @Deprecated("Compatibility with API of WindowInsets", replaceWith = ReplaceWith("get(type)"))
    fun getInsets(type: Int): Insets {
        var typeMask = 0
        (type.toTypeSet() - hidden).forEach {
            typeMask = typeMask or it.toLegacyType()
        }
        return when (typeMask) {
            0 -> Insets.NONE
            else -> getInsetsIgnoringVisibility(typeMask)
        }
    }

    @Deprecated("Compatibility with API of WindowInsets", replaceWith = ReplaceWith("getIgnoringVisibility(types)"))
    fun getInsetsIgnoringVisibility(type: Int): Insets {
        val values = fourZeros()
        var cursor = 1
        var seed = TypeSet.FIRST_SEED
        while (cursor in 1..type) {
            if ((cursor and type) != 0) {
                values.max(seed)
            }
            cursor = cursor.shl(1)
            seed++
        }
        return Insets.of(values[0], values[1], values[2], values[3])
    }

    fun getIgnoringVisibility(types: TypeSet): Insets {
        if (types.isEmpty()) {
            return Insets.NONE
        }
        val values = fourZeros()
        var next: TypeSet? = types
        while (next != null) {
            values.max(next.seed)
            next = next.next
        }
        return Insets.of(values[0], values[1], values[2], values[3])
    }

    operator fun get(types: TypeSet): Insets = getIgnoringVisibility(types - hidden)

    internal fun all(): Insets = insets.values.max()

    fun isEmpty(): Boolean = !isNotEmpty()

    fun isNotEmpty(): Boolean = insets.any { !it.value.isEmpty }

    fun isEmpty(types: TypeSet): Boolean = get(types).isEmpty()

    fun isNotEmpty(types: TypeSet): Boolean = !isEmpty(types)

    fun isVisible(types: TypeSet): Boolean = !hidden.contains(types)

    override fun equals(other: Any?): Boolean = when {
        other === this -> true
        other !is ExtendedWindowInsets -> false
        other.hidden != hidden -> false
        other.displayCutout != displayCutout -> false
        else -> other.insets == insets
    }

    override fun hashCode(): Int {
        var result = (displayCutout?.hashCode() ?: 0)
        result = 31 * result + insets.hashCode()
        result = 31 * result + hidden.hashCode()
        return result
    }

    override fun toString(): String {
        val insets = "insets=" + insets
            .map { (type, value) -> "${type.getTypeName()}$value" }
            .joinToString(separator = ",")
            .takeIf { it.isNotEmpty() }
        val hidden = if (hidden.isEmpty()) null else "hidden=$hidden"
        val list = listOfNotNull(insets, hidden).joinToString(separator = ", ")
            .takeIf { it.isNotEmpty() }
            ?: "empty"
        return "$simpleName($list)"
    }

    private fun IntArray.max(seed: Int) {
        max(insets[seed] ?: return)
    }
}

private val array = IntArray(4)

private fun fourZeros() = array.apply {
    for (i in 0..3) set(i, 0)
}

private fun Collection<InsetsValue>.max(): Insets {
    val sum = fourZeros()
    for (value in this) {
        sum.max(value)
    }
    return Insets.of(sum[0], sum[1], sum[2], sum[3])
}

private fun IntArray.max(value: InsetsValue) {
    if (value.isNotEmpty) {
        set(0, max(get(0), value.left))
        set(1, max(get(1), value.top))
        set(2, max(get(2), value.right))
        set(3, max(get(3), value.bottom))
    }
}
