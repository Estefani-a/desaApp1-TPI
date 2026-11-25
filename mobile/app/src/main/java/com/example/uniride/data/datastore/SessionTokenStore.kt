package com.example.uniride.data.datastore

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.uniride.core.dispatcher.DispatchersProvider
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.security.GeneralSecurityException

/**
 * Secure storage for the JWT Bearer token (CONVENCIONES §6.4, preentrega 11.3).
 * Never persists the token in Room, plaintext DataStore or logs.
 */
interface SessionTokenStore {
    /**
     * Emits `true` when an encrypted token is stored, `false` otherwise.
     *
     * Collecting or awaiting `hasToken.first()` also warms the synchronous in-memory snapshot
     * read by [currentToken]. Startup flows should await `hasToken.first()` before issuing
     * authenticated requests so the initial token is guaranteed to be loaded.
     */
    val hasToken: Flow<Boolean>

    /**
     * Emits whenever the backend rejects an authenticated request with HTTP 401.
     *
     * `SessionRepositoryImpl` listens to this flow in `@ApplicationScope` to clear Room
     * (`clearAllTables()`) and user preferences (CONVENCIONES §6.4); the UI observes
     * `SessionRepository.observeIsLoggedIn()` to navigate back to the login graph.
     */
    val unauthorizedEvents: SharedFlow<Unit>

    /**
     * Synchronous in-memory token snapshot for OkHttp `AuthInterceptor` (avoids `runBlocking`).
     * Warmed asynchronously on init and synchronously updated when [hasToken] emits or
     * [readToken]/[saveToken]/[clearToken] runs.
     */
    fun currentToken(): String?

    suspend fun readToken(): String?
    suspend fun saveToken(token: String)
    suspend fun clearToken()
    fun notifyUnauthorized()
}

private val Context.sessionTokenDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "uniride_session_token",
)

/**
 * Encrypts the JWT using Tink AEAD (AES256-GCM) backed by Android Keystore before writing to
 * DataStore. Runs Keystore/Tink and disk I/O on [DispatchersProvider.io], and regenerates the
 * keyset if it becomes unreadable (for example, after an unexpected Keystore invalidation).
 */
class EncryptedSessionTokenStore(
    context: Context,
    private val appScope: CoroutineScope,
    private val dispatchers: DispatchersProvider,
) : SessionTokenStore {

    private val appContext = context.applicationContext
    private val dataStore = appContext.sessionTokenDataStore

    private val aeadLock = Any()
    @Volatile
    private var cachedAead: Aead? = null
    private val cachedToken = MutableStateFlow<String?>(null)
    private val _unauthorizedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override val unauthorizedEvents: SharedFlow<Unit> = _unauthorizedEvents.asSharedFlow()

    override val hasToken: Flow<Boolean> = dataStore.data
        .map { prefs ->
            val encrypted = prefs[KEY_ACCESS_TOKEN]
            val decrypted = encrypted?.let(::decryptOrNull)
            cachedToken.value = decrypted
            !decrypted.isNullOrBlank()
        }
        .flowOn(dispatchers.io)

    init {
        appScope.launch(dispatchers.io) {
            readToken()
        }
    }

    override fun currentToken(): String? = cachedToken.value

    override suspend fun readToken(): String? = withContext(dispatchers.io) {
        val encrypted = dataStore.data.first()[KEY_ACCESS_TOKEN] ?: run {
            cachedToken.value = null
            return@withContext null
        }
        val decrypted = decryptOrNull(encrypted)
        cachedToken.value = decrypted
        decrypted
    }

    override suspend fun saveToken(token: String) = withContext(dispatchers.io) {
        val ciphertext = encryptWithRecovery(token.toByteArray(Charsets.UTF_8))
        val encoded = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = encoded
        }
        cachedToken.value = token
    }

    override suspend fun clearToken() = withContext(dispatchers.io) {
        cachedToken.value = null
        dataStore.edit { prefs ->
            prefs.remove(KEY_ACCESS_TOKEN)
        }
        Unit
    }

    override fun notifyUnauthorized() {
        cachedToken.value = null
        appScope.launch(dispatchers.io) {
            dataStore.edit { prefs ->
                prefs.remove(KEY_ACCESS_TOKEN)
            }
            _unauthorizedEvents.tryEmit(Unit)
        }
    }

    private fun decryptOrNull(encoded: String): String? = runCatching {
        val ciphertext = Base64.decode(encoded, Base64.NO_WRAP)
        val plaintext = getOrCreateAead().decrypt(ciphertext, ASSOCIATED_DATA)
        String(plaintext, Charsets.UTF_8)
    }.getOrNull()

    private fun encryptWithRecovery(plaintext: ByteArray): ByteArray = try {
        getOrCreateAead().encrypt(plaintext, ASSOCIATED_DATA)
    } catch (_: GeneralSecurityException) {
        resetAndCreateAead().encrypt(plaintext, ASSOCIATED_DATA)
    }

    private fun getOrCreateAead(): Aead {
        cachedAead?.let { return it }
        return synchronized(aeadLock) {
            cachedAead ?: try {
                buildAead(appContext)
            } catch (_: GeneralSecurityException) {
                resetKeysetAndBuild()
            } catch (_: IOException) {
                resetKeysetAndBuild()
            }.also { cachedAead = it }
        }
    }

    private fun resetAndCreateAead(): Aead = synchronized(aeadLock) {
        resetKeysetAndBuild().also { cachedAead = it }
    }

    private fun resetKeysetAndBuild(): Aead {
        cachedAead = null
        appContext.deleteSharedPreferences(KEYSET_PREFS_FILE)
        return buildAead(appContext)
    }

    private fun buildAead(context: Context): Aead {
        AeadConfig.register()
        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, KEYSET_PREFS_FILE)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle
        return keysetHandle.getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }

    private companion object {
        val KEY_ACCESS_TOKEN = stringPreferencesKey("encrypted_access_token")
        const val KEYSET_NAME = "uniride_token_keyset"
        const val KEYSET_PREFS_FILE = "uniride_token_keyset_prefs"
        const val MASTER_KEY_URI = "android-keystore://uniride_token_master_key"
        val ASSOCIATED_DATA = "uniride_session_jwt".toByteArray(Charsets.UTF_8)
    }
}
