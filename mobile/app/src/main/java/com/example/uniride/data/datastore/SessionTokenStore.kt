package com.example.uniride.data.datastore

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Secure storage for the JWT Bearer token (CONVENCIONES §6.4, preentrega 11.3).
 * Never persists the token in Room, plaintext DataStore or logs.
 */
interface SessionTokenStore {
    /** Emits `true` when an encrypted token is stored, `false` otherwise. */
    val hasToken: Flow<Boolean>

    /** Emits whenever the backend rejects an authenticated request with HTTP 401. */
    val unauthorizedEvents: SharedFlow<Unit>

    /** Synchronous in-memory token snapshot for OkHttp [AuthInterceptor] (avoids runBlocking). */
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
 * Encrypts the JWT using Tink AEAD (AES256-GCM) backed by Android Keystore before writing to DataStore.
 */
class EncryptedSessionTokenStore(
    context: Context,
    private val appScope: CoroutineScope,
) : SessionTokenStore {

    private val appContext = context.applicationContext
    private val dataStore = appContext.sessionTokenDataStore

    private val aead: Aead by lazy { createAead(appContext) }
    private val cachedToken = MutableStateFlow<String?>(null)
    private val _unauthorizedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override val unauthorizedEvents: SharedFlow<Unit> = _unauthorizedEvents.asSharedFlow()

    override val hasToken: Flow<Boolean> = dataStore.data.map { prefs ->
        val encrypted = prefs[KEY_ACCESS_TOKEN]
        val decrypted = encrypted?.let(::decryptOrNull)
        cachedToken.value = decrypted
        !decrypted.isNullOrBlank()
    }

    init {
        appScope.launch {
            cachedToken.value = readToken()
        }
    }

    override fun currentToken(): String? = cachedToken.value

    override suspend fun readToken(): String? {
        val encrypted = dataStore.data.first()[KEY_ACCESS_TOKEN] ?: return null
        val decrypted = decryptOrNull(encrypted)
        cachedToken.value = decrypted
        return decrypted
    }

    override suspend fun saveToken(token: String) {
        val ciphertext = aead.encrypt(
            token.toByteArray(Charsets.UTF_8),
            ASSOCIATED_DATA,
        )
        val encoded = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = encoded
        }
        cachedToken.value = token
    }

    override suspend fun clearToken() {
        cachedToken.value = null
        dataStore.edit { prefs ->
            prefs.remove(KEY_ACCESS_TOKEN)
        }
    }

    override fun notifyUnauthorized() {
        cachedToken.value = null
        appScope.launch {
            dataStore.edit { prefs ->
                prefs.remove(KEY_ACCESS_TOKEN)
            }
            _unauthorizedEvents.tryEmit(Unit)
        }
    }

    private fun decryptOrNull(encoded: String): String? = runCatching {
        val ciphertext = Base64.decode(encoded, Base64.NO_WRAP)
        val plaintext = aead.decrypt(ciphertext, ASSOCIATED_DATA)
        String(plaintext, Charsets.UTF_8)
    }.getOrNull()

    private fun createAead(context: Context): Aead {
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
