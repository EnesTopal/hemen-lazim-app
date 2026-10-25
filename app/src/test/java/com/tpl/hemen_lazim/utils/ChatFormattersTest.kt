package com.tpl.hemen_lazim.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatFormattersTest {

    @Test
    fun initialsUseTurkishCapitals() {
        assertEquals("AY", initialsOf("ayşe yılmaz"))
        assertEquals("İ", initialsOf("ilker"))
        assertEquals("C", initialsOf("  cem  "))
        assertEquals("?", initialsOf(""))
    }
}
