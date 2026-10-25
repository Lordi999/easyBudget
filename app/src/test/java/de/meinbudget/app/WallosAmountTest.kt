package de.meinbudget.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class WallosAmountTest {
    private fun parse(raw: String) = WallosClient.parseAmount(raw)

    @Test
    fun parsesNumbersAndFormattedStrings() {
        assertEquals(BigDecimal("2045.32"), parse("2,045.32"))   // so liefert Wallos es tatsächlich
        assertEquals(BigDecimal("2045.32"), parse("2.045,32"))
        assertEquals(BigDecimal("2045.32"), parse("2045.32"))
        assertEquals(BigDecimal("12.5"), parse("12.5"))
        assertEquals(BigDecimal("1234567"), parse("1,234,567"))
        assertEquals(BigDecimal("0"), parse("0"))
    }

    @Test
    fun rejectsGarbage() {
        assertNull(parse(""))
        assertNull(parse("€2,045.32"))
        assertNull(parse("abc"))
    }
}
