package lib.atomofiron.insets

import androidx.core.graphics.Insets
import lib.atomofiron.insets.ExtendedWindowInsets.Type
import org.junit.Assert.*
import org.junit.Test

class UnitTests {

    object CustomType : Type() {
        val testType1 = define("testType1")
        val testType2 = define("testType2")
        val testType3 = define("testType3")

        val negative = TypeSet("negative", seed = -1)
        val positive = TypeSet("positive", seed = 1)

        inline operator fun invoke(block: CustomType.() -> TypeSet): TypeSet = CustomType.block()
    }

    // associate your custom type with ExtendedWindowInsets
    private inline operator fun ExtendedWindowInsets.invoke(block: CustomType.() -> TypeSet): Insets = get(CustomType.block())

    @Test
    fun ext_window_insets() {
        val zero = Insets.of(0, 0, 0, 0)
        val ascent = Insets.of(100, 200, 300, 400)
        val descent = Insets.of(400, 300, 200, 100)
        val united = Insets.max(ascent, descent)
        val windowInsets = ExtendedWindowInsets.Builder()
            .set(CustomType.testType1, zero)
            .set(CustomType.testType2, ascent)
            .set(CustomType.testType3, descent)
            .build()
        val actual1 = CustomType.from(windowInsets) { testType1 }
        val actual2 = windowInsets(CustomType) { testType2 }
        val actual3 = windowInsets { testType3 }
        val actual4 = windowInsets { testType1 + testType2 + testType3 }
        assertEquals(actual1, zero)
        assertEquals(actual2, ascent)
        assertEquals(actual3, descent)
        assertEquals(actual4, united)
    }

    @Test
    fun insets_value() {
        val input = Insets.of(100, 200, 300, 400)
        val actual = input.toValues()
        assertEquals(actual.toInsets(), input)
    }

    @Test
    fun operations() {
        val one = CustomType { testType2 }
        val two = CustomType { testType1 + testType3 }
        val three = CustomType { testType1 + testType2 + testType3 }
        assertEquals(three - one, CustomType { testType1 + testType3 })
        assertEquals(three - two, CustomType { testType2 })
        assertEquals(three - three, TypeSet.Empty)
        assertEquals(one + two, three)
        assertEquals(three + three, three)
        assertEquals(one * two, TypeSet.Empty)
        assertEquals(one * three, one)
    }

    @Test
    fun negative() {
        val first = Insets.of(100, 0, 100, 0)
        val second = Insets.of(0, 100, 0, 100)
        val both = Insets.max(first, second)
        val windowInsets = ExtendedWindowInsets.Builder()
            .set(CustomType.negative, first)
            .set(CustomType.positive, second)
            .build()
        assertEquals(first, windowInsets[CustomType.negative])
        assertEquals(second, windowInsets[CustomType.positive])
        assertEquals(both, windowInsets[CustomType { negative + positive }])
        assertEquals(Insets.NONE, windowInsets[CustomType { negative * positive }])
    }

    @Test
    fun all() {
        val first = Insets.of(100, 0, 100, 0)
        val second = Insets.of(0, 100, 0, 100)
        val both = Insets.max(first, second)
        val windowInsets = ExtendedWindowInsets.Builder()
            .set(CustomType.testType1, first)
            .set(CustomType.testType2, second)
            .build()
        assertEquals(both, windowInsets.all())
    }

    @Test
    fun empty() {
        val first = Insets.of(100, 0, 100, 0)
        val second = Insets.of(0, 100, 0, 100)
        val windowInsets = ExtendedWindowInsets.Builder()
            .set(CustomType.testType1, first)
            .set(CustomType.testType2, second)
            .build()
        assertEquals(Insets.NONE, windowInsets[TypeSet.Empty])
        assertEquals(Insets.NONE, windowInsets[TypeSet.Empty * CustomType.testType1])
        assertEquals(Insets.NONE, windowInsets[TypeSet.Empty - CustomType.testType1])
        assertEquals(first, windowInsets[TypeSet.Empty + CustomType.testType1])
    }
}

