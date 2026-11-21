package id.kelana.feature.akun.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NonceTest {
    @Test
    fun sha256Hex_matchesKnownVectors() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", Nonce.sha256Hex("abc"))
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", Nonce.sha256Hex(""))
    }

    @Test
    fun generateRaw_is64LowercaseHexCharsAndDiffersEachTime() {
        val a = Nonce.generateRaw()
        val b = Nonce.generateRaw()
        assertEquals(64, a.length)
        assertTrue(a.all { it in '0'..'9' || it in 'a'..'f' })
        assertNotEquals(a, b)
    }
}
