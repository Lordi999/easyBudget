package de.meinbudget.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BudgetBackupTest {
    private val october = Month(
        year = 2026, month = 10, openingCents = -55900, incomeCents = 247500, fixedCents = 202400,
        entries = listOf(Entry(1, "Steam-Spiel", -4500), Entry(2, "MacBook Verkauf", 20000)),
    )

    @Test
    fun roundTrip() {
        val text = BudgetBackup.encode(listOf(october), "2026-10-26")
        val parsed = BudgetBackup.decode(text)!!
        assertEquals(listOf(october), parsed.months)
        assertEquals(0, parsed.invalid)
    }

    @Test
    fun acceptsPlainBudgetJsonList() {
        val text = """[{"year":2026,"month":10,"openingCents":0,"incomeCents":100,"fixedCents":0}]"""
        assertEquals(1, BudgetBackup.decode(text)!!.months.size)
    }

    @Test
    fun rejectsForeignFiles() {
        assertNull(BudgetBackup.decode("not json"))
        assertNull(BudgetBackup.decode("""{"format":"something-else","months":[]}"""))
        assertNull(BudgetBackup.decode("42"))
    }

    @Test
    fun dropsInvalidMonthsAndFixesDuplicateIds() {
        val broken = october.copy(month = 13)
        val duplicates = october.copy(month = 11, entries = listOf(Entry(1, "a", -1), Entry(1, "b", -2)))
        val parsed = BudgetBackup.sanitize(listOf(broken, duplicates))
        assertEquals(1, parsed.invalid)
        assertEquals(listOf(1L, 2L), parsed.months.single().entries.map(Entry::id))
    }
}
