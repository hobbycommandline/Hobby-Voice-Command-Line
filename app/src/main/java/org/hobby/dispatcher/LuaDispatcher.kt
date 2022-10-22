package org.hobby.dispatcher

import androidx.annotation.Keep
import org.hobby.activity.BuildConfig
import org.hobby.activity.MainActivity
import org.hobby.lua.LuaMethods
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.nio.charset.Charset
import java.nio.file.Path
import java.util.logging.Logger
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
class LuaDispatcher(id: Int) {

    private var id: Int = id

    /**
     * A callback you can use to execute Lua code.
     * Accepts an array of arguments of any type.
     *
     * You should delete references to unused callbacks
     * as it will keep the Lua instance loaded while
     * references exist
     */
    @Keep
    class Callback(lua: LuaDispatcher, callbackId: Int) {
        private var lua: LuaDispatcher = lua
        private var callbackId = callbackId
        /**
         * Calls this callback in lua.
         *
         * please use as? casting, as errors may change the type to `String?`
         */
        fun call(arguments: Array<Any?>) {
            lua.executeCallback(lua.id, callbackId, arguments)
        }

        protected fun finalize() {
            destroyCallback(lua.id, callbackId);
        }
    }

    constructor(scriptPath: String) : this(runScript(filePath(scriptPath).toString())) {
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
        return LuaMethods().call(this, functionName, argument);
    }

    protected fun finalize() {
        destroyLuaInstance(id)
    }

    // not static: needs access to this object in case it returns a Callback
    @Synchronized
    private external fun executeFunction(scriptId: Int, functionName: String, arguments: Array<Any?>): Object?
    // not static: needs access to this object in case it returns a Callback
    @Synchronized
    private external fun executeCallback(scriptId: Int, callbackId: Int, arguments: Array<Any?>): Object?

    companion object {
        val LOG = Logger.getLogger("LuaDispatcher")

        @JvmStatic external fun setup(baseFilePath: String)
        @JvmStatic private external fun runScript(script: String): Int
        @JvmStatic private external fun destroyLuaInstance(scriptId: Int)
        @JvmStatic private external fun destroyCallback(scriptId: Int, callbackId: Int)

        // totally doable in c++ to do lua dual read from files/ and assets/
        // but i don't want to. Only will do so if bundled lua gets large
        private fun copyResourceToFiles(path: String) {
            val file: String? = readAssetFile(path)
            if (file != null) {
                val dest = filePath(path).toFile()
                val stream = FileOutputStream(dest)
                val output =
                    OutputStreamWriter(stream)
                output.write(file)
                output.close()
            }
        }
        private fun makeDirectory(path: String) {
            val file = filePath(path).toFile()
            if (!file.exists()) {
                file.mkdir()
            }
        }
        fun copyAllResourcesToFiles() {
            val versionName = "version.txt"
            val updateRequired = BuildConfig.VERSION_NAME != readFile(versionName)
            if (BuildConfig.DEBUG || updateRequired) {
                if (!updateRequired && BuildConfig.DEBUG) {
                    LOG.severe("Copying initial files due to DEBUG flag");
                }
                copyResourcesToFiles("lua")
                writeFile(versionName, BuildConfig.VERSION_NAME)
            }
        }
        private fun copyResourcesToFiles(path: String) {
            val files = MainActivity.context!!.assets.list(path) ?: return
            if (files.isEmpty()) {
                copyResourceToFiles(path)
            } else {
                makeDirectory(path)
                for (file in files) {
                    copyResourcesToFiles("$path/$file")
                }
            }
        }
        private fun readAssetFile(name: String): String? {
            return try {
                MainActivity.context!!.assets.open(name).bufferedReader().use {
                    it.readText()
                }
            } catch (e: IOException) {
                null
            }
        }
        fun readFile(path: String): String? {
            return try {
                val file = filePath(path).toFile()
                file.bufferedReader().use {
                    it.readText()
                }
            } catch (e: IOException) {
                null
            }
        }
        private fun writeFile(path: String, contents: String) {
            val file = filePath(path).toFile()
            file.writeBytes(contents.toByteArray(Charset.defaultCharset()))
        }
        fun filePath(path: String): Path {
            return Path(MainActivity.context!!.getExternalFilesDir(null as String?)!!.absolutePath, path)
        }
    }
}
