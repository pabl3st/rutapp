package com.pabl3st.rutapp.data.repository

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SyncResultTest {

    @Test
    fun `SyncResult has all expected variants`() {
        // Ensures no variant was accidentally removed
        val results: List<SyncResult> = listOf(
            SyncResult.Success,
            SyncResult.NoAuth,
            SyncResult.Unauthorized,
            SyncResult.UploadError,
            SyncResult.DownloadError,
        )
        assertThat(results).hasSize(5)
    }

    @Test
    fun `SyncResult Success is distinct from NoAuth`() {
        assertThat(SyncResult.Success).isNotEqualTo(SyncResult.NoAuth)
    }

    @Test
    fun `SyncResult Unauthorized is distinct from NoAuth`() {
        // NoAuth = no token; Unauthorized = token exists but server rejects it (401)
        assertThat(SyncResult.Unauthorized).isNotEqualTo(SyncResult.NoAuth)
    }
}
