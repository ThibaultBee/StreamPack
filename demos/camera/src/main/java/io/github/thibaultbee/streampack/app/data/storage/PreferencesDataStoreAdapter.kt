package io.github.thibaultbee.streampack.app.data.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.preference.PreferenceDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * A data store adapter for preferences.
 */
class PreferencesDataStoreAdapter(
    private val dataStore: DataStore<Preferences>, private val coroutineScope: CoroutineScope
) : PreferenceDataStore() {
    private fun edit(block: suspend (MutablePreferences) -> Unit) {
        coroutineScope.launch { dataStore.edit(block) }
    }

    private inline fun <reified T> getData(key: String, defValue: T) =
        runBlocking { dataStore.data.first()[key(key)] } ?: defValue

    override fun putString(key: String, value: String?) =
        edit { it[stringPreferencesKey(key)] = value ?: "" }

    override fun putInt(key: String, value: Int) = edit { it[intPreferencesKey(key)] = value }
    override fun putLong(key: String, value: Long) = edit { it[longPreferencesKey(key)] = value }
    override fun putFloat(key: String, value: Float) = edit { it[floatPreferencesKey(key)] = value }
    override fun putBoolean(key: String, value: Boolean) =
        edit { it[booleanPreferencesKey(key)] = value }

    override fun getString(key: String, defValue: String?) = getData(key, defValue)
    override fun getInt(key: String, defValue: Int) = getData(key, defValue)
    override fun getLong(key: String, defValue: Long) = getData(key, defValue)
    override fun getFloat(key: String, defValue: Float) = getData(key, defValue)
    override fun getBoolean(key: String, defValue: Boolean) = getData(key, defValue)
}

private val keyMap: MutableMap<String, Preferences.Key<*>> by lazy { mutableMapOf() }

@Suppress("UNCHECKED_CAST")
private inline fun <reified T> key(key: String, isCache: Boolean = true): Preferences.Key<T> =
    key(key, T::class.java, isCache)

@Suppress("UNCHECKED_CAST")
private fun <T> key(key: String, typeClass: Class<T>, isCache: Boolean = true): Preferences.Key<T> =
    (keyMap[key] ?: when (typeClass) {
        Integer::class.java, Int::class.java -> intPreferencesKey(key)
        Long::class.java -> longPreferencesKey(key)
        Float::class.java -> floatPreferencesKey(key)
        Double::class.java -> doublePreferencesKey(key)
        String::class.java -> stringPreferencesKey(key)
        Boolean::class.java, java.lang.Boolean::class.java -> booleanPreferencesKey(key)
        else -> throw RuntimeException("Unknown class：${typeClass.name}")
    }.also {
        if (isCache) keyMap[key] = it
    }) as Preferences.Key<T>