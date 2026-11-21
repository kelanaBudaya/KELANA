package id.kelana.core.data.auth

import org.junit.Assert.assertEquals
import org.junit.Test

class SupabaseUrlTest {
    @Test
    fun projectRootIsKeptAsIs() {
        assertEquals("https://abc.supabase.co", normalizeSupabaseUrl("https://abc.supabase.co"))
    }

    @Test
    fun trailingSlashIsRemoved() {
        assertEquals("https://abc.supabase.co", normalizeSupabaseUrl(" https://abc.supabase.co/ "))
    }

    @Test
    fun restEndpointFromTheDashboardIsReducedToTheRoot() {
        assertEquals("https://abc.supabase.co", normalizeSupabaseUrl("https://abc.supabase.co/rest/v1/"))
        assertEquals("https://abc.supabase.co", normalizeSupabaseUrl("https://abc.supabase.co/rest/v1"))
    }

    @Test
    fun emptyStaysEmpty() {
        assertEquals("", normalizeSupabaseUrl(""))
    }
}
