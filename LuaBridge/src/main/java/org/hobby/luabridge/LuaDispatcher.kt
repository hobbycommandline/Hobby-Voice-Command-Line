package org.hobby.luabridge

import androidx.annotation.Keep
import java.nio.file.Path
import kotlin.io.path.Path
/**
 * A lua script instance, each contains its own Lua_State
 *
 * Theoretically, because each callback will contain a
 * reference to the parent Lua instance, and the constructor
 * caller will hold a reference as well, all references should
 * be managed properly and there won't be any memory leaks
 * or bad access. Needs some testing to ensure this.
 * Better than manual reference counting I think.
 */
@Keep
class LuaDispatcher(val id: Int, val methods: LuaMethods) {

    /**
     * A callback you can use to execute Lua code.
     * Accepts an array of arguments of any type.
     *
     * You should delete references to unused callbacks
     * as it will keep the Lua instance loaded while
     * references exist
     */
    @Keep
    class Callback(private var lua: LuaDispatcher, private var callbackId: Int) {
        /**
         * Calls this callback in lua.
         *
         * please use as? casting, as errors may change the type to `String?`
         */
        fun call(arguments: Array<Any?>) {
            lua.executeCallback(lua.id, callbackId, arguments)
        }

        protected fun finalize() {
            destroyCallback(lua.id, callbackId)
        }
    }

    constructor(scriptPath: String, methods: LuaMethods) : this(runScript(filePath(scriptPath).toString()), methods) {
    }

    /**
     * Calls the specified function in lua.
     *
     * please use as? casting, as errors may change the type to `String?`
     */
    @Synchronized
    fun callFunction(functionName: String, arguments: Array<Any?>): Any? {
        return executeFunction(id, functionName, arguments)
    }

    /**
     * Already in the Synchronized context
     *
     * please to not cause race conditions
     */
    fun execJvm(functionName: String, argument: Any?): Any? {
        return methods.call(this, functionName, argument)
    }

    protected fun finalize() {
        destroyLuaInstance(id)
    }

    // not static: needs access to this object in case it returns a Callback
    @Synchronized
    private external fun executeFunction(scriptId: Int, functionName: String, arguments: Array<Any?>): Any?
    // not static: needs access to this object in case it returns a Callback
    @Synchronized
    private external fun executeCallback(scriptId: Int, callbackId: Int, arguments: Array<Any?>): Any?

    companion object {
        @JvmStatic external fun setup(baseFilePath: String)
        @JvmStatic private external fun runScript(script: String): Int
        @JvmStatic private external fun destroyLuaInstance(scriptId: Int)
        @JvmStatic private external fun destroyCallback(scriptId: Int, callbackId: Int)

        fun filePath(path: String): Path {
            return Path(LuaBridge.externalFilesPath, path)
        }
    }
}
