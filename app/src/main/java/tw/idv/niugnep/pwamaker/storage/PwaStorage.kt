package tw.idv.niugnep.pwamaker.storage

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import tw.idv.niugnep.pwamaker.model.PwaConfig
import tw.idv.niugnep.pwamaker.model.UaMode

object PwaStorage {
    private const val PREF_NAME = "pwa_maker_prefs"
    private const val KEY_PWAS = "saved_pwas"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getAll(context: Context): List<PwaConfig> {
        val jsonString = getPrefs(context).getString(KEY_PWAS, null) ?: return emptyList()
        val list = mutableListOf<PwaConfig>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    PwaConfig(
                        id = obj.optString("id"),
                        name = obj.optString("name"),
                        url = obj.optString("url"),
                        iconUri = if (obj.has("iconUri") && !obj.isNull("iconUri")) obj.optString("iconUri") else null,
                        uaMode = try {
                            UaMode.valueOf(obj.optString("uaMode", UaMode.BASIC_MOBILE.name))
                        } catch (e: Exception) {
                            UaMode.BASIC_MOBILE
                        },
                        customUa = obj.optString("customUa", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun save(context: Context, config: PwaConfig) {
        val current = getAll(context).toMutableList()
        val index = current.indexOfFirst { it.id == config.id }
        if (index >= 0) {
            current[index] = config
        } else {
            current.add(0, config)
        }
        saveAll(context, current)
    }

    fun delete(context: Context, id: String) {
        val current = getAll(context).filterNot { it.id == id }
        saveAll(context, current)
    }

    private fun saveAll(context: Context, list: List<PwaConfig>) {
        val jsonArray = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("name", item.name)
            obj.put("url", item.url)
            obj.put("iconUri", item.iconUri)
            obj.put("uaMode", item.uaMode.name)
            obj.put("customUa", item.customUa)
            jsonArray.put(obj)
        }
        getPrefs(context).edit().putString(KEY_PWAS, jsonArray.toString()).apply()
    }
}
