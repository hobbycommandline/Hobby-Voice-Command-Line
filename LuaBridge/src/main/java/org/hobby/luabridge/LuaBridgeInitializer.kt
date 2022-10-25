package org.hobby.luabridge

import android.content.Context
import androidx.startup.Initializer


class LuaBridgeInitializer: Initializer<LuaBridge> {
    override fun create(context: Context): LuaBridge {
        LuaBridge.initialize(context)
        return LuaBridge.getInstance(context)
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> {
        return mutableListOf()
    }
}