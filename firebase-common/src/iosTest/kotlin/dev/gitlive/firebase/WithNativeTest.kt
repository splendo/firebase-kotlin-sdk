package dev.gitlive.firebase

import kotlin.test.Test
import kotlin.test.assertEquals

class WithNativeTest {

    data class IOSClass(val value: String)
    class CommonAccessor(override val native: IOSClass) : WithNative<IOSClass>

    @Test
    fun testWithNative() {
        val iosClass = IOSClass("Test")
        val commonAccessor = CommonAccessor(iosClass)
        assertEquals(iosClass, commonAccessor.ios)
    }
}
