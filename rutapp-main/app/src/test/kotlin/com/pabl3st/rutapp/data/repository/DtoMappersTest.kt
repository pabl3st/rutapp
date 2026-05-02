package com.pabl3st.rutapp.data.repository

import com.google.common.truth.Truth.assertThat
import com.pabl3st.rutapp.util.TestFixtures
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Tests para DtoMappers.kt — conversiones DTO → Entity.
 * EXTENSIÓN: añadir test cuando se añada campo nuevo en DtoMappers.kt
 */
@RunWith(JUnit4::class)
class DtoMappersTest {

    @Test
    fun `RouteDto toEntity mapea todos los campos`() {
        val dto = TestFixtures.routeDto(id = 42, uid = "uid-42", name = "Mi Ruta",
            dateAssigned = "2026-06-15", status = "active", notes = "Notas",
            updatedAt = "2026-06-15T10:00:00Z")

        val entity = dto.toEntity(userId = 5, accountId = 3)

        assertThat(entity.uid).isEqualTo("uid-42")
        assertThat(entity.serverId).isEqualTo(42)
        assertThat(entity.userId).isEqualTo(5)
        assertThat(entity.accountId).isEqualTo(3)
        assertThat(entity.name).isEqualTo("Mi Ruta")
        assertThat(entity.dateAssigned).isEqualTo("2026-06-15")
        assertThat(entity.status).isEqualTo("active")
        assertThat(entity.notes).isEqualTo("Notas")
        assertThat(entity.syncStatus).isEqualTo("synced")
        assertThat(entity.syncedAt).isEqualTo("2026-06-15T10:00:00Z")
    }

    @Test
    fun `RouteDto toEntity con notes null`() {
        val entity = TestFixtures.routeDto(notes = null).toEntity(1, 1)
        assertThat(entity.notes).isNull()
    }

    @Test
    fun `RouteDto toEntity con deletedAt`() {
        val entity = TestFixtures.routeDto(deletedAt = "2026-05-10T12:00:00Z").toEntity(1, 1)
        assertThat(entity.deletedAt).isEqualTo("2026-05-10T12:00:00Z")
    }

    @Test
    fun `RouteDto toEntity syncStatus siempre es synced`() {
        val entity = TestFixtures.routeDto().toEntity(1, 1)
        assertThat(entity.syncStatus).isEqualTo("synced")
    }

    @Test
    fun `RouteDto toEntity preserva todos los status posibles`() {
        listOf("pending", "active", "done", "cancelled").forEach { status ->
            val entity = TestFixtures.routeDto(status = status).toEntity(1, 1)
            assertThat(entity.status).isEqualTo(status)
        }
    }

    @Test
    fun `StopDto toEntity mapea todos los campos`() {
        val dto = TestFixtures.stopDto(id = 7, uid = "stop-007", routeUid = "route-001",
            name = "Cliente XYZ", address = "Calle Mayor 1", lat = 39.4699, lng = -0.3763,
            orderIndex = 3, status = "pending", updatedAt = "2026-05-01T15:00:00Z")

        val entity = dto.toEntity(accountId = 2)!!

        assertThat(entity.uid).isEqualTo("stop-007")
        assertThat(entity.serverId).isEqualTo(7)
        assertThat(entity.routeUid).isEqualTo("route-001")
        assertThat(entity.accountId).isEqualTo(2)
        assertThat(entity.name).isEqualTo("Cliente XYZ")
        assertThat(entity.address).isEqualTo("Calle Mayor 1")
        assertThat(entity.lat).isWithin(0.0001).of(39.4699)
        assertThat(entity.lng).isWithin(0.0001).of(-0.3763)
        assertThat(entity.orderIndex).isEqualTo(3)
        assertThat(entity.syncStatus).isEqualTo("synced")
        assertThat(entity.syncedAt).isEqualTo("2026-05-01T15:00:00Z")
    }

    @Test
    fun `StopDto toEntity retorna null cuando routeUid es null`() {
        val entity = TestFixtures.stopDto(routeUid = null).toEntity(2)
        assertThat(entity).isNull()
    }

    @Test
    fun `StopDto toEntity con coordenadas nulas`() {
        val entity = TestFixtures.stopDto(lat = null, lng = null).toEntity(2)!!
        assertThat(entity.lat).isNull()
        assertThat(entity.lng).isNull()
    }

    @Test
    fun `StopDto toEntity preserva visitedAt y status done`() {
        val entity = TestFixtures.stopDto(visitedAt = "2026-05-01T10:30:00Z", status = "done").toEntity(2)!!
        assertThat(entity.visitedAt).isEqualTo("2026-05-01T10:30:00Z")
        assertThat(entity.status).isEqualTo("done")
    }

    @Test
    fun `StopDto toEntity preserva todos los status posibles`() {
        listOf("pending", "visiting", "done", "skipped").forEach { status ->
            val entity = TestFixtures.stopDto(status = status).toEntity(2)!!
            assertThat(entity.status).isEqualTo(status)
        }
    }

    @Test
    fun `StopDto toEntity syncStatus siempre es synced`() {
        val entity = TestFixtures.stopDto().toEntity(2)!!
        assertThat(entity.syncStatus).isEqualTo("synced")
    }
}
