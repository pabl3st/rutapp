package com.pabl3st.rutapp.data.local

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RutasTypeConvertersTest {

    private val converters = RutasTypeConverters()

    // ── scheduledDates ────────────────────────────────────────

    @Test
    fun `scheduledDatesToString returns null for null input`() {
        assertThat(converters.scheduledDatesToString(null)).isNull()
    }

    @Test
    fun `scheduledDatesToString returns null for empty list`() {
        assertThat(converters.scheduledDatesToString(emptyList())).isNull()
    }

    @Test
    fun `scheduledDatesToString serializes list correctly`() {
        val result = converters.scheduledDatesToString(listOf("2026-05-12", "2026-05-21"))
        assertThat(result).isEqualTo("2026-05-12,2026-05-21")
    }

    @Test
    fun `stringToScheduledDates returns null for null input`() {
        assertThat(converters.stringToScheduledDates(null)).isNull()
    }

    @Test
    fun `stringToScheduledDates returns null for blank input`() {
        assertThat(converters.stringToScheduledDates("   ")).isNull()
    }

    @Test
    fun `stringToScheduledDates parses comma-separated dates`() {
        val result = converters.stringToScheduledDates("2026-05-12,2026-05-21")
        assertThat(result).containsExactly("2026-05-12", "2026-05-21").inOrder()
    }

    @Test
    fun `round-trip serialization preserves list`() {
        val original = listOf("2026-05-01", "2026-05-15", "2026-05-28")
        val serialized = converters.scheduledDatesToString(original)
        val deserialized = converters.stringToScheduledDates(serialized)
        assertThat(deserialized).isEqualTo(original)
    }

    @Test
    fun `stringToScheduledDates handles spaces around commas`() {
        val result = converters.stringToScheduledDates("2026-05-12 , 2026-05-21")
        assertThat(result).containsExactly("2026-05-12", "2026-05-21").inOrder()
    }
}
