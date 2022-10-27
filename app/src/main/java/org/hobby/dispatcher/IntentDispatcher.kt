package org.hobby.dispatcher

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.content.Context
import android.content.ComponentName
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import androidx.annotation.Keep
import org.hobby.activity.MainActivity
import org.hobby.lua.LuaHelpers
import org.hobby.lua.LuaStatic
import org.hobby.luabridge.LuaDispatcher
import java.io.File
import java.util.logging.Logger

/**
 * Manage sending intents for Scheme, and telling scheme the result.
 */
@Keep
class IntentDispatcher {
    companion object {
        val LOG = Logger.getLogger("IntentDispatcher")

        @JvmStatic fun sendIntent(intent: Intent, callback: LuaDispatcher.Callback?) {
            val context = MainActivity.context
            val br = createBroadcastReceiver(callback)
            // If we receive this back, the Intent has not been handled or it has been cancelled.
            val initialCode = Activity.RESULT_CANCELED
            context!!.sendOrderedBroadcast(intent, null, br, null, initialCode, null, null)
        }

        @JvmStatic fun sendIntentImplicit(intent: Intent, callback: LuaDispatcher.Callback?) {
            val context = MainActivity.context!!
            val br = createFakeBroadcastReceiver(callback)
            // If we receive this back, the Intent has not been handled or it has been cancelled.
            sendImplicitBroadcast(context, intent, br)
        }

        @JvmStatic fun createBroadcastReceiver(callback: LuaDispatcher.Callback?): CountedBroadcastReceiver? {
            return if (callback == null) {
                null
            } else {
                object : CountedBroadcastReceiver() {
                    override fun onReceive(c: Context, intent: Intent) {
                        activeBroadcasts -= 1
                        // do not simplify this guard statement, you will regret it
                        if (callback == null) {
                            return
                        }
                        val arguments: Array<Any?> = arrayOf(activeBroadcasts, resultCode, resultData, LuaHelpers.intentToMap(intent))
                        callback.call(arguments)
                    }
                }
            }
        }

        @JvmStatic fun createFakeBroadcastReceiver(callback: LuaDispatcher.Callback?): FakeCountedBroadcastReceiver? {
            return if (callback == null) {
                null
            } else {
                object : FakeCountedBroadcastReceiver() {
                    override fun onReceive(c: Context, intent: Intent, resultCode: Int, resultData: String?, resultBundle: Bundle?) {
                        // do not simplify this guard statement, you will regret it
                        if (callback == null) {
                            return
                        }
                        val arguments: Array<Any?> = arrayOf(resultCode, resultData, LuaHelpers.bundleToMap(resultBundle))
                        callback.call(arguments)
                    }
                }
            }
        }

        @JvmStatic fun observeMusic(callback: LuaDispatcher.Callback) {
            val filter = IntentFilter()
            filter.addAction("com.android.music.metachanged")
            val br: BroadcastReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    context.unregisterReceiver(this)
                    callback.call(arrayOf(LuaHelpers.intentToMap(intent)))
                }
            }
            MainActivity.context!!.registerReceiver(br, filter)

        }

        @JvmStatic fun startActivity(intent: Intent) {
            startActivity(intent, true)
        }

        @JvmStatic fun startActivity(intent: Intent, quit: Boolean) {
            val context = MainActivity.context
            intent.flags = intent.flags or Intent.FLAG_ACTIVITY_NEW_TASK
            context!!.startActivity(intent)
            if (quit) {
                LuaStatic.quitApp()
            }
        }

        @JvmStatic fun sendMediaButtonKeyCode(keyCode: Long) {
            val context = MainActivity.context!!
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode.toInt()))
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode.toInt()))
        }

        @JvmStatic fun stopMusic() {
            val am = MainActivity.context!!.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.requestAudioFocus(
                AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build()).build())
            return

            val br = object: FakeCountedBroadcastReceiver() {
                private var wasCancelled: Boolean = false
                override fun onReceive(c:Context, i:Intent, resultCode: Int, resultData: String?, resultBundle: Bundle?){
                    val event: KeyEvent =
                        i.getParcelableExtra(Intent.EXTRA_KEY_EVENT) as KeyEvent? ?: return
                    // abortBroadcast does nothing, we don't receive it
                    // always cleared by the system before we get here
                    if (resultCode == Activity.RESULT_OK) {
                        wasCancelled = true
                    }
                    if (event.action == KeyEvent.ACTION_UP) {
                        // If the app playing music did not respect the normal API, shut music off.
                        // This means the app won't respond to play/next/prev commands.
                        if (!wasCancelled) {
                            LuaStatic.say("force stop")
                            val am = c.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                            am.requestAudioFocus(
                                AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(
                                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build()).build())
                        }
                    }
                }
            }
            sendMediaButtonKeyCodeBR(KeyEvent.KEYCODE_MEDIA_STOP.toLong(), null, br, br)
        }

        @JvmStatic fun sendFile(name: String?, text: String?, intent: Intent, mimeType: String?, callback: LuaDispatcher.Callback?) {
            var fName = name ?: "null"
            if (fName.length < 3) {
                fName = "$name Untitled"
            }
            val file = File.createTempFile(fName, null)
            file.writeText(text ?: "", Charsets.UTF_8)
            val uri = Uri.fromFile(file)
            intent.setDataAndTypeAndNormalize(uri, mimeType)
            sendIntent(intent, callback)
        }

        @JvmStatic fun sendMediaButtonKeyCode(keyCode: Long, intent: Intent?, down_callback: LuaDispatcher.Callback?, up_callback: LuaDispatcher.Callback?) {
            sendMediaButtonKeyCodeBR(keyCode, intent, createFakeBroadcastReceiver(down_callback), createFakeBroadcastReceiver(up_callback))
        }

        private fun sendMediaButtonKeyCodeBR(
            keyCode: Long,
            baseIntent: Intent?,
            broadcastReceiverDown: FakeCountedBroadcastReceiver?,
            broadcastReceiverUp: FakeCountedBroadcastReceiver?) {
            val context = MainActivity.context!!
            val intentDown = if (baseIntent != null) {
                Intent(baseIntent)
            } else {
                Intent(Intent.ACTION_MEDIA_BUTTON)
            }
            val intentUp = Intent(intentDown)
            intentDown.addFlags(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES)
            intentUp.addFlags(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES)
            intentDown.putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN,
                keyCode.toInt()
            ))
            sendImplicitBroadcast(context, intentDown, broadcastReceiverDown)
            intentUp.putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_UP, keyCode.toInt()))
            sendImplicitBroadcast(context, intentUp, broadcastReceiverUp)
        }

        private fun sendImplicitBroadcast(
            context: Context, intent: Intent, cr: FakeCountedBroadcastReceiver?) {
            val pm: PackageManager = context.packageManager
            val matches: List<ResolveInfo> = pm.queryBroadcastReceivers(intent, 0)
            if (matches.isEmpty()) {
                return
            }
            val cns = matches.map {
                ComponentName(
                it.activityInfo.applicationInfo.packageName,
                it.activityInfo.name)
            }
            val orderedDispatcherBR = object: CountedBroadcastReceiver() {

                override fun onReceive(c:Context, i:Intent){
                    activeBroadcasts--
                    val resultBundle = getResultExtras(false)
                    if (resultCode == Activity.RESULT_CANCELED
                        && resultData == null
                        && resultBundle == null
                        && activeBroadcasts > 0) {
                        val intentWithComponentName = Intent(intent)
                        intentWithComponentName.component = cns[cns.size - activeBroadcasts]
                        context.sendOrderedBroadcast(
                            intentWithComponentName,
                            null,
                            this,
                            null,
                            resultCode,
                            null,
                            null
                            )
                    } else {
                        cr?.onReceive(context, intent, resultCode, resultData, getResultExtras(false))
                    }
                }
            }
            orderedDispatcherBR.setOrderedHint(true)
            orderedDispatcherBR.activeBroadcasts = matches.size
            val intentWithComponentName = Intent(intent)
            intentWithComponentName.component = cns.first()
            context.sendOrderedBroadcast(
                intentWithComponentName,
                null,
                orderedDispatcherBR,
                null,
                Activity.RESULT_CANCELED,
                null,
                null)
        }
    }
}