package com.jishiyong.agent

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jishiyong.data.db.entity.Item
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

private val Context.agentMemoryDataStore by preferencesDataStore(name = "agent_memory")

class PreferenceAgentMemoryStore(
    context: Context,
    private val engine: InventoryAliasMemoryEngine = InventoryAliasMemoryEngine()
) : AgentMemoryStore {

    private val dataStore = context.applicationContext.agentMemoryDataStore

    override suspend fun relevantMemoriesFor(input: String, activeItems: List<Item>): List<AgentMemory> {
        val records = readRecords()
        return engine.relevantMemoriesFor(records, input, activeItems)
    }

    override suspend fun rememberSuccessfulAction(
        recognizedText: String,
        action: InventoryAction,
        matchedItem: Item?
    ) {
        try {
            dataStore.edit { preferences ->
                val records = decodeRecords(preferences[ALIASES_KEY].orEmpty())
                val updated = engine.learnFromAction(records, recognizedText, action, matchedItem)
                preferences[ALIASES_KEY] = encodeRecords(updated)
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            // Memory must never make a confirmed inventory operation fail.
        }
    }

    private suspend fun readRecords(): List<InventoryAliasMemory> {
        return try {
            val preferences = dataStore.data.first()
            decodeRecords(preferences[ALIASES_KEY].orEmpty())
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun decodeRecords(text: String): List<InventoryAliasMemory> {
        if (text.isBlank()) return emptyList()
        return try {
            val array = JSONArray(text)
            List(array.length()) { index ->
                val item = array.optJSONObject(index) ?: return@List null
                InventoryAliasMemory(
                    alias = item.optString("alias"),
                    canonicalName = item.optString("canonical_name"),
                    categoryName = item.optString("category_name").takeIf { it.isNotBlank() },
                    hits = item.optInt("hits", 1).coerceAtLeast(1),
                    updatedAt = item.optLong("updated_at", 0L)
                )
            }
                .filterNotNull()
                .filter { it.alias.isNotBlank() && it.canonicalName.isNotBlank() }
        } catch (_: JSONException) {
            emptyList()
        }
    }

    private fun encodeRecords(records: List<InventoryAliasMemory>): String {
        val array = JSONArray()
        records.forEach { record ->
            array.put(
                JSONObject()
                    .put("alias", record.alias)
                    .put("canonical_name", record.canonicalName)
                    .put("category_name", record.categoryName.orEmpty())
                    .put("hits", record.hits)
                    .put("updated_at", record.updatedAt)
            )
        }
        return array.toString()
    }

    private companion object {
        val ALIASES_KEY = stringPreferencesKey("inventory_alias_memories")
    }
}
