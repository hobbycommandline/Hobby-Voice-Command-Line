package org.hobby.lua

import android.content.Intent
import androidx.annotation.Keep
import org.hobby.luabridge.LuaDispatcher
import java.util.*


@Keep
class LuaHelpers {
    companion object {
        fun mapToIntent(map: Any?): Pair<Intent, LuaDispatcher.Callback?> {
            val intent = Intent()
            var callback: LuaDispatcher.Callback? = null
            (map as? Map<String,Any?>)?.let {
                    map ->
                intent.action = map["action"] as? String
                val extras = map["extras"] as? Map<String, Any?>
                extras?.forEach{
                        entry ->
                    when(entry.value?.javaClass) {
                        java.lang.Boolean::class.java ->
                            intent.putExtra(entry.key, entry.value as? Boolean)
                        java.lang.String::class.java ->
                            intent.putExtra(entry.key, entry.value as? String)
                        java.lang.Integer::class.java ->
                            intent.putExtra(entry.key, entry.value as? Int)
                        java.lang.Long::class.java ->
                            intent.putExtra(entry.key, entry.value as? Long)
                        java.lang.Float::class.java ->
                            intent.putExtra(entry.key, entry.value as? Float)
                        java.lang.Double::class.java ->
                            intent.putExtra(entry.key, entry.value as? Double)
                    }
                }
                // since lua uses doubles, we need to be able to specifically target float
                val floatExtras = map["floatExtras"] as? Map<String, Any?>
                floatExtras?.forEach{
                        entry ->
                    when(entry.value?.javaClass) {
                        Double.javaClass ->
                            intent.putExtra(entry.key, (entry.value as? Double)?.toFloat())
                    }
                }

                (map["type"] as? String)?.let{
                    intent.type = it
                }

                (map["categories"] as? Array<*>)?.let{
                    categories ->
                    categories.forEach {
                        (it as? String)?.let{
                            category ->
                            intent.addCategory(category)
                        }
                    }
                }
                callback = map["callback"] as? LuaDispatcher.Callback
            }

            return Pair(intent, callback)
        }

        fun intentToMap(intent: Intent): Map<String, Object?> {
            val map = HashMap<String, Object?>()
            val extrasMap = HashMap<String, Object?>()
            val extras = intent.extras
            if (extras != null) {
                val keys = extras.keySet()
                keys.forEach {
                    key ->
                    extrasMap[key] = extras.get(key) as? Object
                }
            }


            map["action"] = intent.action as? Object
            map["extras"] = extrasMap as? Object
            map["type"] = intent.type as? Object
            map["categories"] = (intent.categories as? Set<String>)?.toTypedArray() as? Object
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