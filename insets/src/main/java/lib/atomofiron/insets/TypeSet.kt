/*
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package lib.atomofiron.insets

import java.util.Objects
import java.util.concurrent.atomic.AtomicInteger

// WindowInsetsCompat.Type.SIZE = 9
private var nextSeed = AtomicInteger(1000 - 7)

data class TypeSet internal constructor(
    val name: String,
    internal val seed: Int = nextSeed.incrementAndGet(),
    internal val next: TypeSet? = null,
    val animated: Boolean = next?.animated ?: false,
) : Set<TypeSet> {
    companion object {
        internal const val FIRST_SEED = 1
        internal val All = TypeSet("all", -1)
        val Empty = TypeSet("empty", 0)
    }

    override val size: Int = when {
        this === Empty -> 0
        this === All -> -1
        next == null -> 1
        else -> next.size.inc()
    }

    override fun isEmpty(): Boolean = this === Empty

    override operator fun contains(element: TypeSet): Boolean  = when {
        this === All -> true
        element === All -> false
        else -> contains(element.seed)
    }

    operator fun contains(seed: Int): Boolean = when {
        seed == this.seed -> true
        next == null -> false
        else -> next.contains(seed)
    }

    override fun containsAll(elements: Collection<TypeSet>): Boolean {
        when {
            elements.isEmpty() -> return true
            isEmpty() -> return !elements.any { !it.isEmpty() }
            elements is TypeSet -> return !elements.any { !contains(it) }
            else -> for (element in elements) {
                if (!containsAll(element)) return false
            }
        }
        return true
    }

    override fun iterator() = object : Iterator<TypeSet> {
        private var next: TypeSet? = this@TypeSet
            get() = field?.takeIf { it !== Empty }
        override fun hasNext(): Boolean = next != null
        override fun next(): TypeSet = next
            ?.also { next = it.next }
            ?.single()
            ?: throw NoSuchElementException()
    }

    // without sorting
    operator fun plus(other: TypeSet): TypeSet {
        when {
            isEmpty() -> return other
            other.isEmpty() -> return this
            this === All -> return All
            other === All -> return All
        }
        var head = this
        var next: TypeSet? = other
        while (next != null) {
            if (next !in head) {
                head = next.copy(next = head)
            }
            next = next.next
        }
        return head
    }

    operator fun minus(other: TypeSet): TypeSet = when {
        isEmpty() -> Empty
        other.isEmpty() -> this
        other === All -> Empty
        // todo? TypeSet(inverted = true, next = other)
        this === All -> throw UnsupportedOperationException("All minus ${other.joinToString { it.name }}")
        else -> operation(other, contains = false)
    }

    operator fun times(other: TypeSet): TypeSet = when {
        isEmpty() -> Empty
        other.isEmpty() -> Empty
        this === All -> other
        other === All -> this
        else -> operation(other, contains = true)
    }

    // this and other should not be empty
    private fun operation(other: TypeSet, contains: Boolean): TypeSet {
        var head: TypeSet? = null
        for (it in this) {
            if ((it in other) == contains) {
                head = it.copy(next = head)
            }
        }
        return head ?: Empty
    }

    private fun single(): TypeSet = if (size == 1) this else copy(next = null)

    override fun toString(): String = next?.takeIf { it.isNotEmpty() }?.let { ("$it,$name") } ?: name

    override fun equals(other: Any?): Boolean = when {
        other === this -> true
        other !is TypeSet -> false
        !containsAll(other) -> false
        !other.containsAll(this) -> false
        else -> true
    }

    override fun hashCode(): Int = Objects.hash(seed, next)
}
