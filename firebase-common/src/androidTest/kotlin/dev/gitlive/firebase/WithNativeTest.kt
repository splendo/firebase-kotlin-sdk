package dev.gitlive.firebase

import org.junit.Test
import kotlin.test.assertEquals

class WithNativeTest {

    data class AndroidClass(val value: String)
    class CommonAccessor(override val native: AndroidClass) : WithNative<AndroidClass>

    @Test
    fun testWithNative() {
        val androidClass = AndroidClass("Test")
        val commonAccessor = CommonAccessor(androidClass)
        assertEquals(androidClass, commonAccessor.android)
    }
}
