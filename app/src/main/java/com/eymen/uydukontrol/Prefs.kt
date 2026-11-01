package com.eymen.uydukontrol

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Uses the same SharedPreferences file and keys as v0.12 ("eymen_tv"), so favourites,
 * hidden channels, custom names and channel order survive the update to 2.0.
 */
class Prefs(context: Context) {
    private val sp: SharedPreferences = context.getSharedPreferences("eymen_tv", Context.MODE_PRIVATE)

    fun stringSet(key: String): MutableSet<String> = try {
        HashSet(sp.getStringSet(key, null) ?: emptySet())
    } catch (e: Exception) {
        HashSet()
    }

    fun saveSet(key: String, values: Collection<String>) {
        sp.edit().putStringSet(key, HashSet(values)).apply()
    }

    fun bool(key: String, def: Boolean): Boolean = try { sp.getBoolean(key, def) } catch (e: Exception) { def }

    fun putBool(key: String, v: Boolean) {
        sp.edit().putBoolean(key, v).apply()
    }

    fun int(key: String, def: Int): Int = try { sp.getInt(key, def) } catch (e: Exception) { def }

    fun putInt(key: String, v: Int) {
        sp.edit().putInt(key, v).apply()
    }

    fun str(key: String, def: String): String = try { sp.getString(key, def) ?: def } catch (e: Exception) { def }

    fun putStr(key: String, v: String?) {
        val e = sp.edit()
        if (v == null) e.remove(key) else e.putString(key, v)
        e.apply()
    }

    fun customName(key: String): String? = try { sp.getString("name_$key", null) } catch (e: Exception) { null }

    fun setCustomName(key: String, v: String?) {
        putStr("name_$key", v)
    }

    fun customCategory(key: String): String? = try { sp.getString("category_$key", null) } catch (e: Exception) { null }

    fun setCustomCategory(key: String, v: String?) {
        putStr("category_$key", v)
    }

    var order: String
        get() = str("order", "")
        set(v) {
            putStr("order", v)
        }

    var lastChannel: String?
        get() {
            val k = try { sp.getString("lastChannelKey", null) } catch (e: Exception) { null }
            if (k != null) return k
            val old = try { sp.getLong("lastChannel", -1L) } catch (e: Exception) { -1L }
            return if (old >= 0) old.toString() else null
        }
        set(v) {
            putStr("lastChannelKey", v)
        }

    var pin: String
        get() = str("pin", "")
        set(v) {
            putStr("pin", if (v.isEmpty()) null else v)
        }

    fun loadReminders(): List<Reminder> {
        val out = ArrayList<Reminder>()
        try {
            val arr = JSONArray(str("reminders", "[]"))
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(Reminder(o.getString("k"), o.getString("c"), o.getString("t"), o.getLong("s")))
            }
        } catch (e: Exception) {
        }
        return out
    }

    fun saveReminders(list: List<Reminder>) {
        val arr = JSONArray()
        for (r in list) {
            val o = JSONObject()
            o.put("k", r.channelKey)
            o.put("c", r.channelName)
            o.put("t", r.title)
            o.put("s", r.start)
            arr.put(o)
        }
        putStr("reminders", arr.toString())
    }
}
