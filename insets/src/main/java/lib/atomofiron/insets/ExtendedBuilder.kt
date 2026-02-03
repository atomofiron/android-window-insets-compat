/*
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package lib.atomofiron.insets

import androidx.core.graphics.Insets
import androidx.core.view.DisplayCutoutCompat

class ExtendedBuilder internal constructor(
    values: Map<Int, InsetsValue>? = null,
    private var hidden: TypeSet = TypeSet.Empty,
    private val displayCutout: DisplayCutoutCompat? = null,
) {
    private val values: MutableMap<Int, InsetsValue> = values?.toMutableMap() ?: mutableMapOf()

    init {
        logEntries("init")
    }

    @Deprecated("Compatibility with API of WindowInsets.Builder", replaceWith = ReplaceWith("set(type, insets)"))
    fun setInsets(type: Int, insets: Insets): ExtendedBuilder {
        for (seed in LEGACY_RANGE) {
            val cursor = seed.toTypeMask()
            when {
                cursor > type -> break
                (cursor and type) != 0 -> values[seed] = insets.toValues()
            }
        }
        return this
    }

    fun consume(typeMask: Int): ExtendedBuilder = consume(typeMask.toTypeSet(), MAX_INSETS)

    operator fun get(types: TypeSet): Insets {
        var value = InsetsValue()
        types.forEach {
            value = value.max(values[it.seed])
        }
        return value.toInsets()
    }

    operator fun set(types: TypeSet, insets: Insets): ExtendedBuilder {
        val debugValues = debug { values.toMap() }
        val insetsValue = insets.toValues()
        types.forEach {
            values[it.seed] = insetsValue
        }
        debugValues?.let { logChanges("set", from = it, to = values, insets, types) }
        return this
    }

    fun max(types: TypeSet, insets: Insets): ExtendedBuilder {
        val debugValues = debug { values.toMap() }
        val insetsValue = insets.toValues()
        types.forEach {
            values[it.seed] = insetsValue max values[it.seed]
        }
        debugValues?.let { logChanges("max", from = it, to = values, insets, types) }
        return this
    }

    fun add(types: TypeSet, insets: Insets): ExtendedBuilder {
        val debugValues = debug { values.toMap() }
        val insetsValue = insets.toValues()
        types.forEach {
            values[it.seed] = insetsValue + values[it.seed]
        }
        debugValues?.let { logChanges("add", from = it, to = values, insets, types) }
        return this
    }

    fun consume(types: TypeSet): ExtendedBuilder = consume(types, MAX_INSETS)

    fun consume(insets: Insets): ExtendedBuilder {
        for ((seed, value) in values.entries.toList()) {
            values[seed] = value.consume(insets)
        }
        return this
    }

    fun consume(types: TypeSet, insets: Insets = MAX_INSETS): ExtendedBuilder {
        val debugValues = debug { values.toMap() }
        when {
            insets.isEmpty() -> return this.also { logd { "consume empty" } }
            types.isEmpty() -> return this.also { logd { "consume nothing" } }
            else -> for (type in types) {
                values[type.seed] = values[type.seed]?.consume(insets) ?: continue
            }
        }
        debugValues?.let { logChanges("consume", from = it, to = values, insets, types) }
        return this
    }

    fun setVisible(types: TypeSet): ExtendedBuilder {
        hidden -= types
        return this
    }

    fun setInvisible(types: TypeSet): ExtendedBuilder {
        hidden += types
        return this
    }

    fun setVisibility(types: TypeSet, visible: Boolean): ExtendedBuilder {
        return if (visible) setVisible(types) else setInvisible(types)
    }

    fun build(): ExtendedWindowInsets {
        logEntries("build")
        return ExtendedWindowInsets(values.toMap(), hidden, displayCutout)
    }

    private fun logEntries(prefix: String) {
        logd {
            val entries = values.entries
                .filter { !it.value.isEmpty }
                .joinToString(separator = " ") {
                    "${it.key.getTypeName()}${it.value}"
                }
            "$prefix $entries"
        }
    }
}