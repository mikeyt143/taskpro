package org.tasks.googleapis

import com.google.api.client.googleapis.services.json.AbstractGoogleJsonClientRequest
import com.google.api.client.http.HttpResponseException
import com.google.api.client.json.GenericJson
import com.todoroo.astrid.gtasks.api.HttpCredentialsAdapter
import com.todoroo.astrid.gtasks.api.HttpNotFoundException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tasks.BuildConfig
import timber.log.Timber
import java.io.IOException

abstract class BaseInvoker(
        private val credentialsAdapter: HttpCredentialsAdapter,
) {
    @Throws(IOException::class)
    protected suspend fun <T> execute(request: AbstractGoogleJsonClientRequest<T>): T? = execute(request, false)

    @Throws(IOException::class)
    private suspend fun <T> execute(request: AbstractGoogleJsonClientRequest<T>, retry: Boolean): T? =
            withContext(Dispatchers.IO) {
                credentialsAdapter.checkToken()
                Timber.d(caller)
                val response: T? = try {
                    request.execute()
                } catch (e: HttpResponseException) {
                    return@withContext if (e.statusCode == 401 && !retry) {
                        credentialsAdapter.invalidateToken()
                        execute(request, true)
                    } else if (e.statusCode == 404) {
                        throw HttpNotFoundException(e)
                    } else {
                        throw e
                    }
                }
                Timber.d("%s -> %s", caller, if (BuildConfig.DEBUG) prettyPrint(response) else "<redacted>")
                response
            }

    @Throws(IOException::class)
    private fun <T> prettyPrint(`object`: T?): Any? {
        if (BuildConfig.DEBUG) {
            if (`object` is GenericJson) {
                return (`object` as GenericJson).toPrettyString()
            }
        }
        return `object`
    }

    private val caller: String
        get() = try {
            Thread.currentThread().stackTrace[12].methodName
        } catch (e: Exception) {
            Timber.e(e)
            ""
        }

    companion object {
        const val APP_NAME = "Tasks/${BuildConfig.VERSION_NAME}"
    }
}