package org.hobby.files

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
class AssetVersionControl {
    companion object {
        private val LOG = Logger.getLogger("Files")

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
            if (updateRequired) {
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
