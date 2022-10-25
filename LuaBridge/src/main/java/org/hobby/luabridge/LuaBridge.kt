package org.hobby.luabridge

import android.content.Context

class LuaBridge {
    companion object {
        var externalFilesPath = ""
        fun initialize(context: Context) {
            externalFilesPath = context.getExternalFilesDir(null as String?)!!.absolutePath
            System.loadLibrary("lua-bridge")
            LuaDispatcher.setup(externalFilesPath)
        }

        fun getInstance(context: Context): LuaBridge {
            return LuaBridge()
        }
    }
}