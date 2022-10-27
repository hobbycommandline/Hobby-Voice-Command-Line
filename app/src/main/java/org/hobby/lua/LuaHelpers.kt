package org.hobby.lua

import android.content.Intent
import android.os.Bundle
import androidx.annotation.Keep
import org.hobby.luabridge.LuaDispatcher
import java.util.*


@Keep
class LuaHelpers {
    companion object {
        fun mapToIntent(map: Any?): Pair<Intent?, LuaDispatcher.Callback?> {
            var intent: Intent? = null
            var callback: LuaDispatcher.Callback? = null
            (map as? Map<String,Any?>)?.let {
                    map ->
                val innerIntent = Intent()
                intent = innerIntent
                innerIntent.action = map["action"] as? String
                val extras = map["extras"] as? Map<String, Any?>
                extras?.forEach{
                        entry ->
                    when(entry.value?.javaClass) {
                        java.lang.Boolean::class.java ->
                            innerIntent.putExtra(entry.key, entry.value as? Boolean)
                        java.lang.String::class.java ->
                            innerIntent.putExtra(entry.key, entry.value as? String)
                        java.lang.Integer::class.java ->
                            innerIntent.putExtra(entry.key, entry.value as? Int)
                        java.lang.Long::class.java ->
                            innerIntent.putExtra(entry.key, entry.value as? Long)
                        java.lang.Float::class.java ->
                            innerIntent.putExtra(entry.key, entry.value as? Float)
                        java.lang.Double::class.java ->
                            innerIntent.putExtra(entry.key, entry.value as? Double)
                        HashMap::class.java ->
                            innerIntent.putExtra(entry.key, mapToIntent(entry.value))
                    }
                }
                // since lua uses doubles, we need to be able to specifically target float
                val floatExtras = map["floatExtras"] as? Map<String, Any?>
                floatExtras?.forEach{
                        entry ->
                    when(entry.value?.javaClass) {
                        Double::class.java ->
                            innerIntent.putExtra(entry.key, (entry.value as? Double)?.toFloat())
                    }
                }
                // since lua uses Longs, we need to be able to specifically target int
                val intExtras = map["intExtras"] as? Map<String, Any?>
                intExtras?.forEach{
                        entry ->
                    when(entry.value?.javaClass) {
                        java.lang.Long::class.java ->
                            innerIntent.putExtra(entry.key, (entry.value as? Long)?.toInt())
                    }
                }

                (map["type"] as? String)?.let{
                    innerIntent.type = it
                }

                (map["categories"] as? Array<*>)?.let{
                    categories ->
                    categories.forEach {
                        (it as? String)?.let{
                            category ->
                            innerIntent.addCategory(category)
                        }
                    }
                }
                callback = map["callback"] as? LuaDispatcher.Callback
            }

            return Pair(intent, callback)
        }

        fun bundleToMap(bundle: Bundle?): HashMap<String, Any?>? {
            if (bundle == null) {
                return null
            }
            val map = HashMap<String, Any?>()
            val keys = bundle.keySet()
            keys.forEach {
                    key ->
                // reminder not to fix this; get preserves underlying type
                map[key] = bundle.get(key)
            }
            return map
        }

        fun intentToMap(intent: Intent): Map<String, Any?> {
            val map = HashMap<String, Any?>()
            val extrasMap = HashMap<String, Any?>()
            val extras = intent.extras
            if (extras != null) {
                val keys = extras.keySet()
                keys.forEach {
                    key ->
                    // reminder not to fix this; get preserves underlying type
                    extrasMap[key] = extras.get(key)
                }
            }


            map["action"] = intent.action as? Any
            map["extras"] = extrasMap as? Any
            map["type"] = intent.type as? Any
            map["categories"] = (intent.categories as? Set<String>)?.toTypedArray() as? Any
            return map
        }

            /**
         * Use to receive more input
         */
        fun setSpeechCallback(callback: LuaDispatcher.Callback) {
            LuaStatic.additionalInfoCallback = callback
        }

        fun hasSpeechCallback(): Boolean {
            return LuaStatic.additionalInfoCallback != null
        }

        fun clearSpeechCallback() {
            LuaStatic.additionalInfoCallback = null
        }
    }
}