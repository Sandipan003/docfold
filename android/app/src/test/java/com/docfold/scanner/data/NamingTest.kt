package com.docfold.scanner.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NamingTest {
    @Test fun takesTheFirstLineThatHasWords() = assertEquals("Rental agreement", nameFromText("12\n--\n  Rental   agreement:\nBetween the owner"))
    @Test fun keepsNamesShort() = assertTrue(nameFromText("word ".repeat(40))!!.length <= 60)
    @Test fun findsNothingInNumbersAlone() = assertNull(nameFromText("12 34\n56"))
    @Test fun findsNothingInBlankText() = assertNull(nameFromText(""))
}
