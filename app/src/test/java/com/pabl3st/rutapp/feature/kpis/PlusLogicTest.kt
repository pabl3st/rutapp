package com.pabl3st.rutapp.feature.kpis

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Tests para la lógica Plus/PlusLL del sector telco.
 * Lógica OR: activaciones >= 5 OR primerBono >= 50€
 */
class PlusLogicTest {

    // Simula la función de cálculo aislada (sin Room)
    private fun isPlus(activaciones: Double, primerBono: Double): Boolean =
        activaciones >= 5.0 || primerBono >= 50.0

    @Test
    fun `plus activaciones exactamente 5`() {
        assertThat(isPlus(activaciones = 5.0, primerBono = 0.0)).isTrue()
    }

    @Test
    fun `plus primerBono exactamente 50`() {
        assertThat(isPlus(activaciones = 0.0, primerBono = 50.0)).isTrue()
    }

    @Test
    fun `plus activaciones 4 y bono 49 = no plus`() {
        assertThat(isPlus(activaciones = 4.0, primerBono = 49.99)).isFalse()
    }

    @Test
    fun `plus OR logic - activaciones cumple pero bono no`() {
        assertThat(isPlus(activaciones = 6.0, primerBono = 10.0)).isTrue()
    }

    @Test
    fun `plus OR logic - bono cumple pero activaciones no`() {
        assertThat(isPlus(activaciones = 2.0, primerBono = 75.0)).isTrue()
    }

    @Test
    fun `plus ambos cumplen`() {
        assertThat(isPlus(activaciones = 7.0, primerBono = 80.0)).isTrue()
    }

    @Test
    fun `plus cero activaciones y cero bono = no plus`() {
        assertThat(isPlus(activaciones = 0.0, primerBono = 0.0)).isFalse()
    }

    @Test
    fun `plus activaciones justo bajo umbral`() {
        assertThat(isPlus(activaciones = 4.9, primerBono = 0.0)).isFalse()
    }

    @Test
    fun `plus bono justo bajo umbral`() {
        assertThat(isPlus(activaciones = 0.0, primerBono = 49.99)).isFalse()
    }
}
