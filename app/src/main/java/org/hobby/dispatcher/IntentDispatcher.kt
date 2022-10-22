package org.hobby.dispatcher

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.support.v4.media.session.PlaybackStateCompat
import android.view.KeyEvent
import androidx.annotation.Keep
import org.hobby.activity.MainActivity
import org.hobby.lua.LuaHelpers
import org.hobby.lua.LuaStatic
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

        @JvmStatic fun createBroadcastReceiver(callback: LuaDispatcher.Callback?): BroadcastReceiver? {
            return if (callback == null) {
                null
            } else {
                object : BroadcastReceiver() {
                    override fun onReceive(c: Context, intent: Intent) {
                        // do not simplify this guard statement, you will regret it
                        if (callback == null) {
                            return
                        }
                        val arguments: Array<Any?> = arrayOf(resultCode, resultData, LuaHelpers.intentToMap(intent))
                        callback.call(arguments)
                    }
                }
            }
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

        @JvmStatic fun sendMediaButtonAction(action: Int) {
            sendMediaButtonAction(action.toLong())
        }

        @JvmStatic fun sendMediaButtonAction(action: Long) {
            val context = MainActivity.context!!
            val keyCode = PlaybackStateCompat.toKeyCode(action)
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        }

        @JvmStatic fun stopMusic() {
            val br = object: BroadcastReceiver() {
                private var wasCancelled: Boolean = false
                override fun onReceive(c:Context, i:Intent){
                    val event: KeyEvent =
                        i.getParcelableExtra(Intent.EXTRA_KEY_EVENT) as KeyEvent? ?: return
                    clearAbortBroadcast()
                    if (resultCode == Activity.RESULT_OK) {
                        LOG.warning("Was cancelled $resultCode")
                        wasCancelled = true
                    }
                    if (event.action == KeyEvent.ACTION_UP) {
                        val am = c.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                        // If the app playing music did not respect the normal API, shut music off.
                        // This means the app won't respond to play/next/prev commands.
                        if (!wasCancelled) {
                            LuaStatic.say("force stop")
                            am.requestAudioFocus(
                                AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(
                                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build()).build())
                        }
                    }
                }
            }
            sendMediaButtonActionBR(PlaybackStateCompat.ACTION_STOP, br, br)
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

        @JvmStatic fun sendMediaButtonAction(action: Int, down_callback: LuaDispatcher.Callback?, up_callback: LuaDispatcher.Callback?) {
            sendMediaButtonActionBR((action as Number).toLong(), createBroadcastReceiver(down_callback), createBroadcastReceiver(up_callback))
        }

        @JvmStatic fun sendMediaButtonAction(action: Long, down_callback: LuaDispatcher.Callback?, up_callback: LuaDispatcher.Callback?) {
            sendMediaButtonActionBR(action, createBroadcastReceiver(down_callback), createBroadcastReceiver(up_callback))
        }

        private fun sendMediaButtonActionBR(action: Long, broadcastReceiverDown: BroadcastReceiver?, broadcastReceiverUp: BroadcastReceiver?) {
            val context = MainActivity.context!!
            val keyCode = PlaybackStateCompat.toKeyCode(action)
            val intent = Intent(Intent.ACTION_MEDIA_BUTTON)
            intent.putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            context.sendOrderedBroadcast(intent, null, broadcastReceiverDown, null, 0, null, null)
            intent.putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_UP, keyCode))
            context.sendOrderedBroadcast(intent, null, broadcastReceiverUp, null, 0, null, null)
        }
    }
}