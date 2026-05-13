package com.pabl3st.rutapp.data.local

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SyncStatusTest {

    @Test
    fun `SyncStatus constants have correct values`() {
        assertThat(SyncStatus.PENDING).isEqualTo("pending")
        assertThat(SyncStatus.SYNCED).isEqualTo("synced")
        assertThat(SyncStatus.ERROR).isEqualTo("error")
    }

    @Test
    fun `all SyncStatus values are non-empty`() {
        listOf(SyncStatus.PENDING, SyncStatus.SYNCED, SyncStatus.ERROR).forEach {
            assertThat(it).isNotEmpty()
        }
    }
}
