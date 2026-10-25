package com.tpl.hemen_lazim.network

import com.tpl.hemen_lazim.R
import com.tpl.hemen_lazim.ui.messageRes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class ApiErrorsTest {

    private fun errorResponse(status: Int, body: String): Response<Any> =
        Response.error(status, body.toResponseBody("application/json".toMediaType()))

    @Test
    fun errorBodyGivesCodeAndMessage() {
        val e = errorResponse(404, """{"code":"REQUEST_NOT_FOUND","message":"Request not found"}""").toApiException()

        assertEquals("REQUEST_NOT_FOUND", e.code)
        assertEquals(404, e.status)
        assertEquals("Request not found", e.message)
    }

    @Test
    fun unreadableBodyKeepsOnlyTheStatus() {
        val e = errorResponse(502, "<html>Bad gateway</html>").toApiException()

        assertNull(e.code)
        assertEquals(502, e.status)
    }

    @Test
    fun bodyOrThrowThrowsTheCodedError() {
        val e = assertThrows(ApiException::class.java) {
            errorResponse(400, """{"code":"EMAIL_TAKEN","message":"Email already in use"}""").bodyOrThrow()
        }

        assertEquals(ApiCodes.EMAIL_TAKEN, e.code)
    }

    @Test
    fun apiResultTurnsHttpErrorsIntoCodedExceptions() = runBlocking {
        val result = apiResult<Unit> { throw HttpException(errorResponse(409, """{"code":"REQUEST_NOT_OPEN"}""")) }

        val e = result.exceptionOrNull() as ApiException
        assertEquals(ApiCodes.REQUEST_NOT_OPEN, e.code)
        assertEquals(409, e.status)
    }

    @Test
    fun apiResultKeepsNetworkErrorsAndRethrowsCancellation() = runBlocking {
        assertTrue(apiResult<Unit> { throw IOException("offline") }.exceptionOrNull() is IOException)
        try {
            apiResult<Unit> { throw CancellationException("cancelled") }
            fail("Coroutine iptali Result'a çevrilmemeli")
        } catch (_: CancellationException) {
        }
    }

    @Test
    fun knownCodesHaveTheirOwnText() {
        assertEquals(R.string.error_request_not_found, ApiException("REQUEST_NOT_FOUND", 404, null).messageRes())
        assertEquals(R.string.error_email_taken, ApiException("EMAIL_TAKEN", 400, null).messageRes())
        assertEquals(R.string.error_invalid_credentials, ApiException("INVALID_CREDENTIALS", 401, null).messageRes())
        assertEquals(R.string.session_expired, ApiException("REFRESH_EXPIRED", 401, null).messageRes())
    }

    @Test
    fun unknownCodesFallBackToTheStatus() {
        // Sunucuya sonradan eklenen bir kod ya da kodsuz eski sunucu: sunucunun İngilizce metni değil
        assertEquals(R.string.error_conflict, ApiException("SOMETHING_NEW", 409, "Conflict").messageRes())
        assertEquals(R.string.error_server, ApiException(null, 503, null).messageRes())
        assertEquals(R.string.error_request_failed, ApiException("INVALID_USER_ID", 400, null).messageRes())
    }

    @Test
    fun networkAndUnexpectedErrorsHaveGenericTexts() {
        assertEquals(R.string.error_network, IOException("offline").messageRes())
        assertEquals(R.string.error_unknown, IllegalStateException("boom").messageRes())
    }
}
