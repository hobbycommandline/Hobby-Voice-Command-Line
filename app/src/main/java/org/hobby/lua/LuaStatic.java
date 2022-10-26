package org.hobby.lua;

import android.content.Intent;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;

import org.hobby.luabridge.LuaDispatcher;

import java.util.List;
import java.util.logging.Logger;

@Keep
public class LuaStatic {
    /**
     * Logcat channel.
     */
    public static final Logger LOG = Logger.getLogger("Lua");

    public static Intent lastIntent = null;

    /**
     * Use org.hobby.scheme.SchemeMethods.setSpeechCallback instead.
     */
    @Nullable
    public static volatile LuaDispatcher.Callback additionalInfoCallback = null;

    /**
     * Use org.hobby.lua.LuaStatic.say instead.
     */
    public static MutableLiveData<List<String>> talkChannel = new MutableLiveData<>();

    /**
     * Use org.hobby.lua.LuaStatic.quitApp instead.
     */
    public static MutableLiveData<Boolean> doQuitApp = new MutableLiveData<>();

    /**
     * Quit the App.
     */
    public static void quitApp () {
        doQuitApp.postValue(true);
    }
    /**
     * Print to chat.
     */
    public static void say (Object message) {
        List<String> channel = talkChannel.getValue();
        if (channel != null) {
            // It is a list due to main thread frequency.
            channel.add(message.toString());
        }
        talkChannel.postValue(channel);
    }

    /**
     * Clear any chat sent during the current loop.
     */
    public static void clearTalk () {
        List<String> channel = talkChannel.getValue();
        if (channel != null) {
            channel.clear();
        }
    }
}
