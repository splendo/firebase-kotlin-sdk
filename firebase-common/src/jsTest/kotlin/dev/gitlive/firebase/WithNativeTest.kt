package dev.gitlive.firebase

import kotlin.test.Test
import kotlin.test.assertEquals

class WithNativeTest {

    data class JSClass(val value: String)
    data class JSClassAccessor(override val js: JSClass) : JsAccessor<JSClass>
    class CommonAccessor<T>(override val native: T) : WithNative<T>

    @Test
    fun testWithNative() {
        val jsClass = JSClass("Test")
        val commonAccessor = CommonAccessor(jsClass)
        assertEquals(jsClass, commonAccessor.native)
        assertEquals(jsClass, commonAccessor.js)
    }

    @Test
    fun testWithNativeJSAccessor() {
        val jsClass = JSClass("Test")
        val accessor = JSClassAccessor(jsClass)
        val commonAccessor = CommonAccessor(accessor)
        assertEquals(accessor, commonAccessor.native)
        assertEquals(jsClass, commonAccessor.js)
    }
}
