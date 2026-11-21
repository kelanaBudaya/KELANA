package id.kelana.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidationTest {
    @Test
    fun acceptsNormalEmails() {
        assertTrue(AuthValidation.isValidEmail("a@b.co"))
        assertTrue(AuthValidation.isValidEmail("  nama.baru+tag@mail.example.id "))
    }

    @Test
    fun rejectsBadEmails() {
        listOf("", "abc", "a@b", "@b.com", "a@.com", "a b@c.com", "a@@b.com", "a..b@c.com", "a@b..com", "a@b.c")
            .forEach { assertFalse(it, AuthValidation.isValidEmail(it)) }
    }

    @Test
    fun passwordNeedsEightCharsLettersAndDigits() {
        assertTrue(AuthValidation.isValidPassword("abcdefg1"))
        assertTrue(AuthValidation.isValidPassword("1234567A"))
        assertFalse(AuthValidation.isValidPassword("abc1"))
        assertFalse(AuthValidation.isValidPassword("abcdefgh"))
        assertFalse(AuthValidation.isValidPassword("12345678"))
        assertFalse(AuthValidation.isValidPassword("!!!!!!!!1"))
        assertFalse(AuthValidation.isValidPassword(""))
    }
}
