package org.hobby.lua

import androidx.annotation.Keep
import org.hobby.activity.MainActivity
import org.hobby.dispatcher.IntentDispatcher
import org.hobby.luabridge.LuaDispatcher
import org.hobby.dispatcher.MainDispatcher
import java.util.logging.Logger

@Keep
class LuaMethods(): org.hobby.luabridge.LuaMethods {
    override fun call(lua: LuaDispatcher, functionName: String, argument: Any?): Any?{
        try {
            return LuaMethods::class.java
                .getMethod(functionName, LuaDispatcher::class.java, Object::class.java)
                .invoke(this, lua, argument)
        } catch (e: NoSuchMethodException) {
            LOG.severe("No such method: $functionName");
            return null;
        } catch (t: Throwable) {
            LOG.severe("Error during execution of $functionName: $t");
            t.printStackTrace()
            return null;
        }
    }

    /**
     * Quit the App.
     */
    fun wakeWordWatch(unused1: LuaDispatcher, unused2: Any?): Boolean {
        MainActivity.watchingForWakeWord = true
        MainActivity.wakeWord = unused2 as? String ?: "computer"
        return true
    }

    /**
     * Quit the App.
     */
    fun wakeWordStop(unused1: LuaDispatcher, unused2: Any?): Boolean {
        MainActivity.watchingForWakeWord = false
        MainActivity.isAwake = true
        return true
    }

    /**
     * Quit the App.
     */
    fun quit(unused1: LuaDispatcher, unused2: Any?): Boolean {
        if (!MainActivity.watchingForWakeWord) {
            LuaStatic.doQuitApp.postValue(true)
        }
        return true
    }

    fun unmuteBackground(unused1: LuaDispatcher, unused2: Any?): Boolean {
        MainDispatcher.dispatcher.unmuteBackground()
        return true
    }

    /**
     * Quit the App.
     */
    fun forceQuit(unused1: LuaDispatcher, unused2: Any?): Boolean {
        LuaStatic.doQuitApp.postValue(true)
        return true
    }

    /**
     * Stop currently playing music.
     */
    fun stopMusic(unused1: LuaDispatcher, unused2: Any?): Boolean {
        IntentDispatcher.stopMusic()
        return true
    }

    /**
     * Print to chat.
     */
    fun say(unused1: LuaDispatcher, argument: Any?): Boolean {
        (argument as? String)?.let {
            LuaStatic.say(it)
        }
        return true
    }

    /**
     * Dispatch intent
     */
    fun sendIntent(unused1: LuaDispatcher, argument: Any?): Boolean {
        val (intent, callback) = LuaHelpers.mapToIntent(argument)
        LuaStatic.lastIntent = intent;
        IntentDispatcher.sendIntent(intent, callback)
        return true
    }

    /**
     * Dispatch intent
     */
    fun startActivity(unused1: LuaDispatcher, argument: Any?): kotlin.Boolean {
        // callback not accepted by startActivity in java
        val (intent, _) = LuaHelpers.mapToIntent(argument)
        LuaStatic.lastIntent = intent;
        IntentDispatcher.startActivity(intent, false)
        return true;
    }

    fun resendLastIntent(unused1: LuaDispatcher, argument: Any?): kotlin.Boolean {
        IntentDispatcher.startActivity(LuaStatic.lastIntent, false)
        return true
    }

    fun sendMediaButtonAction(unused1: LuaDispatcher, argument: Any?): Boolean {
        (argument as? Long)?.let {
            IntentDispatcher.sendMediaButtonAction(it)
        }
        return true
    }

    fun sendMediaButtonActionCallback(unused1: LuaDispatcher, argument: Any?): Boolean {
        (argument as? Map<String, Object>)?.let {
            map->
            val button = map["button"] as? java.lang.Integer
            val down_callback = map["down"] as? LuaDispatcher.Callback
            val up_callback = map["up"] as? LuaDispatcher.Callback
            if (button != null) {
                IntentDispatcher.sendMediaButtonAction(button.toInt(), down_callback, up_callback)
            }
        }
        return true
    }

    fun sendFile(unused1: LuaDispatcher, argument: Any?): Boolean {
        (argument as? Map<String,Any?>)?.let {
            arguments ->
            val name = arguments["name"] as? String
            val text = arguments["text"] as? String
            val (intent, callback) = LuaHelpers.mapToIntent(arguments["intent"])
            val mimeType = arguments["mimeType"] as? String
            IntentDispatcher.sendFile(name, text, intent, mimeType, callback)
        }
        return true
    }

    fun setSpeechCallback(unused1: LuaDispatcher, argument: Any?): Boolean {
        (argument as? LuaDispatcher.Callback)?.let { callback ->
            LuaHelpers.setSpeechCallback(callback)
        }
        return true
    }

    fun hasSpeechCallback(unused1: LuaDispatcher, argument: Any?): Boolean {
        return LuaHelpers.hasSpeechCallback()
    }

    fun clearSpeechCallback(unused1: LuaDispatcher, argument: Any?): Boolean {
        LuaHelpers.clearSpeechCallback()
        return true
    }

    fun observeMusicState(unused1: LuaDispatcher, argument: Any?): Boolean {
        (argument as? LuaDispatcher.Callback)?.let { callback ->
            IntentDispatcher.observeMusic(callback)
            return true
        }
        return false
    }

    companion object {
        val LOG = Logger.getLogger("LuaMethods")
    }
}