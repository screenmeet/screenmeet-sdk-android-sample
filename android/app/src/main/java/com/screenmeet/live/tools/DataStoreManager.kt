package com.screenmeet.live.tools

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.screenmeet.live.BuildConfig
import com.screenmeet.live.tools.DataStoreManager.PreferencesKeys.SESSION_ID
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

val Context.getDataStore: DataStore<Preferences> by preferencesDataStore(
    name = BuildConfig.APPLICATION_ID
)

class DataStoreManager @Inject constructor(@ApplicationContext val context: Context) {

    private val dataStore = context.getDataStore

    var sessionId: String?
        get() = runBlocking { dataStore.data.first()[SESSION_ID] }
        set(value) = runBlocking {
            dataStore.edit { prefs ->
                prefs[SESSION_ID] = value ?: ""
            }
        }

    suspend fun setConnectionPrefs(endpoint: String, serverTag: String, apiKey: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.URL_ENDPOINT] = endpoint
            preferences[PreferencesKeys.SERVER_TAG] = serverTag
            preferences[PreferencesKeys.API_KEY] = apiKey
        }
    }

    suspend fun getConnectionPrefs(): Triple<String?, String?, String?> {
        val prefs = dataStore.data.first()
        val endpoint = prefs[PreferencesKeys.URL_ENDPOINT]?.ifEmpty { null }
        val tag = prefs[PreferencesKeys.SERVER_TAG]?.ifEmpty { null }
        val apiKey = prefs[PreferencesKeys.API_KEY]?.ifEmpty { null }
        return Triple(endpoint, tag, apiKey)
    }

    private object PreferencesKeys {
        val URL_ENDPOINT = stringPreferencesKey("endpoint")
        val SERVER_TAG = stringPreferencesKey("tag")
        val API_KEY = stringPreferencesKey("apiKey")
        val SESSION_ID = stringPreferencesKey("sessionId")
    }
}
