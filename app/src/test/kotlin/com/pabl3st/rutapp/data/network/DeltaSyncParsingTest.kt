package com.pabl3st.rutapp.data.network

import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parsea una respuesta REAL de `delta_sync` capturada de producción
 * (app/src/test/resources/delta_sync_real.json).
 *
 * Por qué existe: al pasar Moshi a adaptadores KSP se eliminó el fallback
 * reflexivo, que toleraba en silencio los tipos que MySQL/PDO devuelve.
 * Se verificó que todos los DTOs tuvieran @JsonClass, pero NO que sus tipos
 * casaran con el JSON real — y la primera sincronización falló con
 * "Expected a boolean but was NUMBER at path $.stops[0].pdv_open",
 * dejando al cliente sin rutas.
 *
 * Validar la forma del DTO no basta: hay que validar el contenido real.
 * Si alguien cambia un tipo de StopDto o el backend cambia una columna,
 * este test cae antes que el usuario.
 */
class DeltaSyncParsingTest {

    private val moshi: Moshi = Moshi.Builder()
        .add(LenientAdapters)
        .build()

    private fun payload(): String =
        javaClass.classLoader!!.getResourceAsStream("delta_sync_real.json")!!
            .bufferedReader().use { it.readText() }

    @Test
    fun `parsea la respuesta real de delta_sync sin excepciones`() {
        val adapter = moshi.adapter(DeltaSyncResponse::class.java)
        val resp = adapter.fromJson(payload())

        assertNotNull("la respuesta no debe ser null", resp)
        assertTrue("ok debe ser true", resp!!.ok)
        assertTrue("debe traer rutas", (resp.routes?.size ?: 0) > 0)
        assertTrue("debe traer paradas", (resp.stops?.size ?: 0) > 0)
    }

    @Test
    fun `pdv_open llega como 0-1 y se convierte a Boolean`() {
        val resp = moshi.adapter(DeltaSyncResponse::class.java).fromJson(payload())!!
        val stop = resp.stops!!.first()
        // En el JSON real pdv_open es el número 1, no true
        assertEquals(true, stop.pdvOpen)
        assertEquals(false, stop.pdvInactive)
    }

    @Test
    fun `lat y lng llegan como String decimal y se convierten a Double`() {
        val resp = moshi.adapter(DeltaSyncResponse::class.java).fromJson(payload())!!
        val stop = resp.stops!!.first()
        assertNotNull("lat debe parsearse", stop.lat)
        assertNotNull("lng debe parsearse", stop.lng)
        assertTrue("lat en rango de España", stop.lat!! in 27.0..44.0)
        assertTrue("lng en rango de España", stop.lng!! in -19.0..5.0)
    }

    @Test
    fun `visit_frequency numerico no rompe el parseo`() {
        val resp = moshi.adapter(DeltaSyncResponse::class.java).fromJson(payload())!!
        // Viene como INT en MySQL; el DTO lo declara String?
        resp.stops!!.forEach { assertNotNull(it.uid) }
    }

    @Test
    fun `los adaptadores laxos aceptan ambas formas del mismo valor`() {
        val a = moshi.adapter(DeltaSyncResponse::class.java)
        val base = """{"ok":true,"routes":[],"stops":[%s],"server_time":"2026-08-09T00:00:00Z"}"""
        val comoNumero = """{"uid":"u1","route_uid":"r1","name":"N","status":"pending",
            "order_index":0,"lat":"39.5","lng":"-0.3","pdv_open":1,"pdv_inactive":0,
            "visit_frequency":54,"created_at":"2026-01-01 00:00:00","updated_at":"2026-01-01 00:00:00"}"""
        val comoNativo = """{"uid":"u2","route_uid":"r1","name":"N","status":"pending",
            "order_index":0,"lat":39.5,"lng":-0.3,"pdv_open":true,"pdv_inactive":false,
            "visit_frequency":"54","created_at":"2026-01-01 00:00:00","updated_at":"2026-01-01 00:00:00"}"""

        val r1 = a.fromJson(base.format(comoNumero))!!.stops!!.first()
        val r2 = a.fromJson(base.format(comoNativo))!!.stops!!.first()

        assertEquals("ambas formas dan el mismo Boolean", r1.pdvOpen, r2.pdvOpen)
        assertEquals("ambas formas dan el mismo Double", r1.lat, r2.lat)
        assertEquals("ambas formas dan el mismo Double", r1.lng, r2.lng)
    }
}
