package de.meinbudget.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {
    @Test
    fun parsesGermanAndPlainFormats() {
        assertEquals(4500L, parseCents("45"))
        assertEquals(4550L, parseCents("45,5"))
        assertEquals(-1299L, parseCents("-12,99"))
        assertEquals(20000L, parseCents("+200"))
        assertEquals(123456L, parseCents("1.234,56 €"))
        assertEquals(1250L, parseCents("12.50"))
    }

    @Test
    fun rejectsInvalidInput() {
        assertNull(parseCents(""))
        assertNull(parseCents("abc"))
        assertNull(parseCents("1e5"))
        assertNull(parseCents("12,5x"))
    }

    @Test
    fun rejectsAmountsAboveLimit() {
        assertEquals(100_000_000_000L, parseCents("1000000000"))
        assertNull(parseCents("1000000000,01"))
        assertNull(parseCents("99999999999999999999"))
    }
}
