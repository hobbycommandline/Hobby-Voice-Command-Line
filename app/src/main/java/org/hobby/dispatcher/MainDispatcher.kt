package org.hobby.dispatcher

import android.content.Context.AUDIO_SERVICE
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import androidx.annotation.Keep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.hobby.activity.MainActivity
import org.hobby.lua.LuaHelpers
import org.hobby.lua.LuaStatic

@Keep
class MainDispatcher internal constructor() {
    companion object {
        var dispatcher = MainDispatcher()
    }
    private var focusRequest: AudioFocusRequest? = null

    suspend fun dispatch(phrase: String): Boolean {
        val schemeArguments = phrase.split(' ')
        val firstWord = schemeArguments[0]

        /*
         * User panic safe word
         */
        if (phrase.indexOf("stop stop stop") != -1) {
            LuaStatic.say("> stopped")
            return false
        }

        /*
         * Program has requested additional user feedback.
         *
         * Provide phrase to script instead of issuing new Action.
         */
        if (LuaHelpers.hasSpeechCallback()) {
            return withContext(Dispatchers.Default) {
                val callback = LuaStatic.additionalInfoCallback
                if (callback != null) {
                    /*
                     * End for unlimited input
                     */
                    val userRequestedStop = firstWord == "stop" || (phrase.length < 10 && phrase.indexOf("stop") != -1  )
                    if (userRequestedStop) {
                        LuaHelpers.clearSpeechCallback()
                    }
                    callback.call(arrayOf(schemeArguments.toTypedArray(), userRequestedStop))

                    if (LuaHelpers.hasSpeechCallback()) {
                        LuaStatic.say("> ...")
                        true
                    } else {
                        unmuteBackground()
                        LuaStatic.say("> ok")
                        false
                    }
                } else {
                    unmuteBackground()
                    LuaStatic.say("unknown error")
                    false
                }
            }
        }

        if (firstWord == "stop") {
            unmuteBackground()
        }

        /*
         * New action from firstWord
         */
        return withContext(Dispatchers.Default) {
            LuaStatic.additionalInfoCallback = null
            val scriptName = "lua/actions/${firstWord}.lua"
            var found = false
            if (!LuaDispatcher.filePath(scriptName).toFile().exists()) {
                LuaStatic.say("> ${firstWord} not found")
            } else {
                val lua = LuaDispatcher(scriptName)
                val result = lua.callFunction("main", arrayOf(schemeArguments.toTypedArray()))
                LuaStatic.say("> $result")
                found = true
            }
            if (LuaHelpers.hasSpeechCallback()) {
                LuaStatic.say("> ...")
                true
            } else {
                !found
            }
        }
    }

    fun lostFocus() {
        focusRequest = null
    }

    fun muteBackground(listener: AudioManager.OnAudioFocusChangeListener) {
        if (focusRequest != null) {
            return
        }
        val am = MainActivity.context!!.getSystemService(AUDIO_SERVICE) as AudioManager
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build())
            .setOnAudioFocusChangeListener(listener)
        .build()
        focusRequest = request
        val result = am.requestAudioFocus(request)
    }

    fun unmuteBackground() {
        val request = focusRequest
        if (request != null) {
            val am = MainActivity.context!!.getSystemService(AUDIO_SERVICE) as AudioManager
            am.abandonAudioFocusRequest(request)
            focusRequest = null
        }
    }
}