package org.hobby.luabridge

interface LuaMethods {
    fun call(lua: LuaDispatcher, functionName: String, argument: Any?): Any?
}