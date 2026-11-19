package dev.anmitali.kotoba.input

import dev.anmitali.kotoba.layout.CharKey
import dev.anmitali.kotoba.layout.Layer
import dev.anmitali.kotoba.layout.LayerKind
import org.junit.Assert.assertEquals
import org.junit.Test

class KeyOutputTest {

    private val key = CharKey("e", "E", 1f, hold = listOf("é", "è"), holdShifted = listOf("É", "È"))

    private fun layer(shiftable: Boolean) = Layer(LayerKind.Keys, shiftable, false, emptyList(), null)

    @Test
    fun outputFollowsShiftOnShiftableLayer() {
        assertEquals("e", outputOf(key, layer(true), ShiftState.Off))
        assertEquals("E", outputOf(key, layer(true), ShiftState.Once))
        assertEquals("E", outputOf(key, layer(true), ShiftState.Caps))
    }

    @Test
    fun outputIgnoresShiftOnFixedLayer() {
        assertEquals("e", outputOf(key, layer(false), ShiftState.Caps))
    }

    @Test
    fun alternativesFollowShift() {
        assertEquals(listOf("é", "è"), alternativesOf(key, layer(true), ShiftState.Off))
        assertEquals(listOf("É", "È"), alternativesOf(key, layer(true), ShiftState.Once))
        assertEquals(listOf("é", "è"), alternativesOf(key, layer(false), ShiftState.Caps))
    }
}
