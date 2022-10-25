// Copyright 2019 Alpha Cephei Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

// File heavily modified. You are welcome to use the modifications to this file under Apache 2.0
package org.hobby.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.*
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.CompoundButton
import android.widget.ToggleButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import kotlinx.coroutines.runBlocking
import org.hobby.chat.ChatRecyclerView
import org.hobby.database.AppDatabase
import org.hobby.dispatcher.MainDispatcher
import org.hobby.files.AssetVersionControl
import org.hobby.lua.LuaStatic
import org.hobby.state.SpeakingState
import org.json.JSONException
import org.json.JSONObject
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.SpeechStreamService
import org.vosk.android.StorageService
import java.io.File
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : Activity(), RecognitionListener, LifecycleOwner, AudioManager.OnAudioFocusChangeListener {
    private var model: Model? = null

    /** For Vosk, the Speech to text API */
    private var speechService: SpeechService? = null
    private var isPaused = false

    /** For Vosk, speech to text API */
    private var speechStreamService: SpeechStreamService? = null
    private var recyclerView: RecyclerView? = null
    private var chatRecyclerViewAdapter: ChatRecyclerView? = null
    private var mediaPlayer: MediaPlayer? = null
    private var doCallbacks = false

    /**
     * This is an enum based state to keep track of the microphone status.
     */
    private var lastState = 0

    /**
     * is the last thing printed to the log unimportant or partial, and thus should
     * be replaced if we are placing something in the queue that wants to replace
     */
    private var canReplaceLast = true
    private lateinit var lifecycleRegistry: LifecycleRegistry
    private val lifecycleRegistryLock = Any()
    private var textToSpeech: TextToSpeech? = null
    private var micOn = false
    private var isZombie = false
    private var delayMillis: Long = 200
    private val speakingState = SpeakingState(
        { // start listening
            turnOnMic()
            true
        },
        { // begin mic on process
            playSound(R.raw.start_sound, null)
            true
        },
        { // turn off mic
            if (speechService != null) {
                shutDownMic()
                true
            } else {
                false
            }
        })


    /**
     * Sadly, we need ids for textToSpeech, so we just use an int and increment.
     * This is ok because each time we speak we want it said as a unique speech
     */
    private var utteranceId: Int = 0

    private val backgroundExecutor: ExecutorService = Executors.newFixedThreadPool(4)

    // Settings section
    private var settingsDB: AppDatabase? = null
    private var doAnnounceInput = MutableLiveData<Boolean>()


    public override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        synchronized(lifecycleRegistryLock) {
            doAnnounceInput.value = true
            settingsDB = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java, "settings"
            ).build()

            val booleanSettings = settingsDB?.booleanSettingDao()
            backgroundExecutor.execute {
                //booleanSettings?.insertAll(BooleanSetting("main/doAnnounceInput", false))
                val shouldAnnounceInput =
                    booleanSettings?.findByPath("main/doAnnounceInput")?.value ?: true
                doAnnounceInput.postValue(shouldAnnounceInput)
            }

            // Window setup
            context = applicationContext

            AssetVersionControl.copyAllResourcesToFiles()
            val externalFilesDir = getExternalFilesDir(null as String?)
            if (externalFilesDir == null) {
                val exception: Exception =
                    IOException("cannot get external files dir, external storage state is " + Environment.getExternalStorageState())
                setErrorState("Failed to unpack the model" + exception.message)
                return
            }


            /*LuaDispatcher("lua/hello-world.lua")?.let{
            lua ->
            var result = lua.callFunction("addition_test", arrayOf(Integer(1), Integer(2))) as? Integer?
            LuaStatic.LOG.warning("result of addition_test(1,2): $result")
            result = lua.callFunction("addition_test", arrayOf(Integer(12), Integer(13))) as? Integer?
            LuaStatic.LOG.warning("result of addition_test(12,13): $result")
            var resultString = lua.callFunction("ordering_test", arrayOf(Integer(12), Integer(34))) as? String?
            LuaStatic.LOG.warning("result of ordering_test(12,34): $resultString")
            resultString = lua.callFunction("string_roundtrip_test", arrayOf("Hello dear reader", "this is a test")) as? String?
            LuaStatic.LOG.warning("result of string_roundtrip_test(\"Hello dear reader\", \"this is a test\"): $resultString")

            lua.callFunction("test_array_in", arrayOf(arrayOf("wouldn't you love to dance", "with me?")))
            lua.callFunction("test_map_in", arrayOf(mapOf("key" to "value", "hello" to "you", "number?" to 123)))

            val resultCallback = lua.callFunction("test_closure", arrayOf("A *warm* hello")) as? LuaDispatcher.Callback?
            LuaStatic.LOG.warning("result of test_closure(...): $resultCallback")
            if (resultCallback != null) {
                resultCallback.call(arrayOf("and a pleasant goodbye!"))
            }

            var resultMap = lua.callFunction("test_map", arrayOf())
            LuaStatic.LOG.warning("result of test_map(): $resultMap")
            resultMap = lua.callFunction("test_map_out_complex", arrayOf())
            LuaStatic.LOG.warning("result of test_map_out_complex(): $resultMap")
            val resultArray = lua.callFunction("test_array_out", arrayOf()) as? Array<*>
            if (resultArray != null) {
                LuaStatic.LOG.warning("result of test_array_out(): ${resultArray.joinToString(", ")}")
            }
        }*/

            textToSpeech = TextToSpeech(applicationContext) {}
            val utteranceProgressListener = object : UtteranceProgressListener() {
                private var numActive = 0
                override fun onStart(utteranceId: String?) {
                    ++numActive
                    speakingState.stoppedListening(null)
                }

                override fun onDone(utteranceId: String?) {
                    --numActive

                    if (numActive < 0) {
                        numActive = 0
                    }
                    speakingState.utteranceFinished()
                }

                override fun onError(utteranceId: String?) {
                    onDone(utteranceId)
                }

            }
            textToSpeech!!.setOnUtteranceProgressListener(utteranceProgressListener)
            val window = this.window
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            } else {
                window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
                window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
            }
            setContentView(R.layout.main)

            // Layout
            recyclerView = findViewById(R.id.chat_log)
            chatRecyclerViewAdapter = ChatRecyclerView(mutableListOf())
            val rLayoutManager = LinearLayoutManager(this)
            recyclerView!!.layoutManager = rLayoutManager
            recyclerView!!.adapter = chatRecyclerViewAdapter
            recyclerView!!.itemAnimator = null

            setUiState(STATE_START)
            findViewById<View>(R.id.recognize_mic).setOnClickListener { recognizeMicrophone() }
            findViewById<ToggleButton>(R.id.pause).setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
                if (doCallbacks) {
                    isPaused = isChecked
                    if (isChecked) {
                        onPausedCallback(
                            true
                        )
                    } else {
                        playSound(R.raw.start_sound) {
                            onPausedCallback(
                                false
                            )
                        }
                    }
                }
            }

            // Async setup
            lifecycleRegistry = LifecycleRegistry(this)
            lifecycleRegistry.currentState = Lifecycle.State.CREATED
            LuaStatic.talkChannel.value = mutableListOf()
            LuaStatic.talkChannel.observe(this) { messages ->
                runOnUiThread {
                    if (!messages.isNullOrEmpty()) {
                        addItems(messages)
                    }
                    LuaStatic.clearTalk()
                }
            }
            LuaStatic.doQuitApp.value = false
            LuaStatic.doQuitApp.observe(this) { doQuit ->
                runOnUiThread {
                    if (doQuit) {
                        finishAndRemoveTask()
                    }
                }
            }

            // Vosk + Permissions
            LibVosk.setLogLevel(LogLevel.INFO)

            // Check if user has given permission to record audio, init the model after permission is granted
            val permissionCheck =
                ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.RECORD_AUDIO
                )
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    PERMISSIONS_REQUEST_RECORD_AUDIO
                )
            } else {
                initModel()
            }
        }
    }

    private fun initModel() {
        val sourcePath = "model-en-us"
        val targetPath = "model"
        val externalFilesDir = getExternalFilesDir(null as String?)
        if (externalFilesDir == null) {
            val exception: Exception =
                IOException("cannot get external files dir, external storage state is " + Environment.getExternalStorageState())
            setErrorState("Failed to unpack the model" + exception.message)
            return
        }

        /* Discover already unpacked */
        val targetDir = File(externalFilesDir, targetPath)
        val resultFile = File(targetDir, sourcePath)
        if (resultFile.exists()) {
            model = Model(resultFile.absolutePath)
            setUiState(STATE_READY)
            return
        }

        /* Unpack */
        StorageService.unpack(this, sourcePath, targetPath,
            { model: Model? ->
                this.model = model
                setUiState(STATE_READY)
            }
        ) { exception: IOException ->
            setErrorState(
                "Failed to unpack the model" + exception.message
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Recognizer initialization is a time-consuming and it involves IO,
                // so we execute it in async task
                initModel()
            } else {
                finish()
            }
        }
    }

    public override fun onStart() {
        super.onStart()
        synchronized(lifecycleRegistryLock) {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        }
    }

    public override fun onDestroy() {
        super.onDestroy()
        synchronized(lifecycleRegistryLock) {
            this.isZombie = true
            shutDownMic()
            handler = null
            MainDispatcher.dispatcher.unmuteBackground()

            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED

            context = null
            if (speechService != null) {
                speechService!!.stop()
                speechService!!.shutdown()
                speechService = null
            }
            if (speechStreamService != null) {
                speechStreamService!!.stop()
                speechStreamService = null
            }
            val mp = mediaPlayer
            if (mp != null) {
                mp.stop()
                mp.release()
                mediaPlayer = null
            }
        }
    }

    override fun onResult(hypothesis: String) {
        handleAction(hypothesis)
    }

    override fun onFinalResult(hypothesis: String) {
        handleAction(hypothesis)
    }

    private fun maybeWake(text: String, final: Boolean) {
        if (text.contains(wakeWord)) {
            wake(final)
            if (final) {
                speakingState.wokeUp()
            }
            val wakeSeen = "Wake word seen: \"$text\""
            if (canReplaceLast) {
                replaceLast(wakeSeen, false)
            } else {
                addItem(wakeSeen, false)
            }
            canReplaceLast = !final
        }
    }

    private fun handleAction(jsonHypothesis: String) {
        try {
            canReplaceLast = false
            val obj = JSONObject(jsonHypothesis)
            val text: String = obj.getString("text").trim { it <= ' ' }
            if (!isAwake && hasSeenWakeWord) {
                maybeWake(wakeWord, true)
                return
            }
            if (text.trim { it <= ' ' }.isEmpty()) {
                return
            }
            if (!isAwake && watchingForWakeWord) {
                maybeWake(text, true)
                return
            } else {
                replaceLast("Detected: \"$text\"", doAnnounceInput.value ?: true)
            }
            val pauseButton = findViewById<ToggleButton>(R.id.pause)

            speakingState.stoppedListening(false)
            backgroundExecutor.execute {
                val continueRunning = runBlocking {
                    MainDispatcher.dispatcher.dispatch(text)
                }
                runOnUiThread {
                    if (continueRunning) {
                        speakingState.pleaseListen()
                    } else if (watchingForWakeWord) {
                        speakingState.startSleeping()
                        MainDispatcher.dispatcher.unmuteBackground()
                    } else {
                        speakingState.stoppedListening(false)
                    }
                }
            }
        } catch (_: JSONException) {
        }
    }

    override fun onPartialResult(hypothesis: String) {
        try {
            val obj = JSONObject(hypothesis)
            val partial = obj.getString("partial")
            if (partial.trim { it <= ' ' }.isEmpty()) {
                return
            }
            if (!isAwake && watchingForWakeWord) {
                maybeWake(partial, false)
                return
            } else if (canReplaceLast) {
                replaceLast(partial, false)
            } else {
                addItem(partial, false)
                canReplaceLast = true
            }
        } catch (_: JSONException) {
        }
    }

    override fun onError(e: Exception) {
        setErrorState(e.message!!)
    }

    override fun onTimeout() {
        setUiState(STATE_DONE)
    }

    private fun setUiState(state: Int) {
        this.doCallbacks = false
        if (lastState == state) {
            return
        }
        val pauseButton = findViewById<ToggleButton>(R.id.pause)
        when (state) {
            STATE_START -> {
                replaceLast(getString(R.string.preparing), false)
                findViewById<View>(R.id.recognize_mic).isEnabled = false
                pauseButton.isEnabled = false
            }
            STATE_READY -> {
                replaceLast(getString(R.string.ready), false)
                (findViewById<View>(R.id.recognize_mic) as Button).setText(R.string.recognize_microphone)
                findViewById<View>(R.id.recognize_mic).isEnabled = true
                pauseButton.isEnabled = false
                speakingState.playSoundThenEnableMic()
            }
            STATE_DONE -> {
                (findViewById<View>(R.id.recognize_mic) as Button).setText(R.string.recognize_microphone)
                findViewById<View>(R.id.recognize_mic).isEnabled = true
                pauseButton.isEnabled = false
            }
            STATE_MIC -> {
                (findViewById<View>(R.id.recognize_mic) as Button).setText(R.string.stop_microphone)
                if (canReplaceLast) {
                    replaceLast(getString(R.string.say_something), false)
                } else {
                    addItem(getString(R.string.say_something), false)
                }
                canReplaceLast = true
                findViewById<View>(R.id.recognize_mic).isEnabled = true
                pauseButton.isEnabled = true
                pauseButton.isChecked = false
            }
            else -> throw IllegalStateException("Unexpected value: $state")
        }
        lastState = state
        this.doCallbacks = true
    }

    private fun setErrorState(message: String) {
        resetWithText(message)
        (findViewById<View>(R.id.recognize_mic) as Button).setText(R.string.recognize_microphone)
        playSound(R.raw.error_sound, null)
    }

    private fun ttsSay(message: String) {
        speakingState.startUtterance()
        val map = HashMap<String, String>()
        map[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = (++utteranceId).toString()
        textToSpeech!!.speak(message, TextToSpeech.QUEUE_ADD, map)
        speakingState.stoppedListening(null)
    }

    private fun replaceLast(message: String, speak: Boolean) {
        if (speak) {
            ttsSay(message)
        }
        chatRecyclerViewAdapter!!.replaceLast(message)
        recyclerView!!.scrollToPosition(chatRecyclerViewAdapter!!.itemCount - 1)
    }

    private fun addItem(message: String, speak: Boolean) {
        if (speak) {
            ttsSay(message)
        }
        chatRecyclerViewAdapter!!.addItem(message)
        recyclerView!!.scrollToPosition(chatRecyclerViewAdapter!!.itemCount - 1)
    }


    private fun addItems(messages: List<String>) {
        ttsSay(
            messages.joinToString(separator = "\n")
        )
        chatRecyclerViewAdapter!!.addItems(messages)
        recyclerView!!.scrollToPosition(chatRecyclerViewAdapter!!.itemCount - 1)
    }


    private fun resetWithText(message: String) {
        chatRecyclerViewAdapter!!.clear()
        addItem(message, true)
    }

    private fun connectBluetooth() {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        registerReceiver(object : BroadcastReceiver() {
            var firstDisconnect = true

            override fun onReceive(context: Context?, intent: Intent) {
                synchronized(lifecycleRegistryLock) {
                    if (isZombie) {
                        unregisterReceiver(this)
                        return
                    }
                    val state: Int = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1)

                    val initMic = if (state == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
                        isBluetoothMicRecording = true
                        true
                    } else if (state == AudioManager.SCO_AUDIO_STATE_ERROR) {
                        audioManager.stopBluetoothSco()
                        isBluetoothMicRecording = false
                        true
                    } else if (state == AudioManager.SCO_AUDIO_STATE_DISCONNECTED) {
                        if (firstDisconnect) {
                            firstDisconnect = false
                            false
                        } else {
                            audioManager.stopBluetoothSco()
                            isBluetoothMicRecording = false
                            true
                        }
                    } else {
                        false
                    }
                    if (initMic) {
                        unregisterReceiver(this)
                        initSpeechService()
                    }
                }
            }
        }, IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED))

        audioManager.startBluetoothSco()
    }

    private fun isBluetoothOn(): Boolean {
        // is ON
        // also the docs say this will continue to work forever
        // its just discouraged
        val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter
            .getDefaultAdapter()
        if(!bluetoothAdapter.isEnabled) {
            return false
        }
        return true
    }
    private fun shutDownBluetoothMic() {
        if (isBluetoothMicRecording) {
            val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            audioManager.stopBluetoothSco()
            isBluetoothMicRecording = false
        }
    }
    private fun shutDownMic() {
        micOn = false
        if (handler != null) {
            handler!!.removeCallbacks(connectBluetoothRunnable)
        }
        setUiState(STATE_DONE)
        shutDownBluetoothMic()
        if (speechService != null) {
            speechService!!.stop()
            speechService!!.shutdown()
            speechService = null
        }
    }
    private val connectBluetoothRunnable = {
        if (!isBluetoothMicRecording) {
            connectBluetooth()
        }
    }
    var handler: Handler? = null
    private fun recognizeMicrophone() {
        if (micOn) {
            speakingState.stoppedListening(false)
            playSound(R.raw.end_sound, null)
            MainDispatcher.dispatcher.unmuteBackground()
        } else {
            speakingState.playSoundThenEnableMic()
        }
    }

    private fun turnOnMic() {
        if (isBluetoothOn()) {
            if (handler == null) {
                handler = Handler(Looper.getMainLooper())
            }
            handler!!.removeCallbacks(connectBluetoothRunnable)
            handler!!.postDelayed(connectBluetoothRunnable, delayMillis)
        } else {
            initSpeechService()
        }
    }

    private fun initSpeechService() {
        try {
            MainDispatcher.dispatcher.muteBackground(this)
            if (speechService == null) {
                val rec = Recognizer(model, 16000.0f)
                speechService = SpeechService(rec, 16000.0f)
            }
            speechService!!.startListening(this)
            setUiState(STATE_MIC)
            micOn = true
        } catch (e: IOException) {
            setErrorState(e.message!!)
        }
    }

    private fun wake(final: Boolean) {
        MainDispatcher.dispatcher.muteBackground(this)
        if (final) {
            speakingState.playSoundThenEnableMic()
        }
    }

    private fun pause() {
        runOnUiThread {
            if (!watchingForWakeWord) {
                val pauseButton = findViewById<ToggleButton>(R.id.pause)
                pauseButton.isChecked = true
            }
        }
    }

    private fun onPausedCallback(shouldPause: Boolean) {
        if (shouldPause) {
            if (!speakingState.isDucked) {
                MainDispatcher.dispatcher.unmuteBackground()
            }
            speakingState.stoppedListening(false)
        } else {
            MainDispatcher.dispatcher.muteBackground(this)
            speakingState.readyToListen()
            speakingState.pleaseListen()
        }
    }

    private fun playSound(_id: Int, callback: (() -> Unit)?) {
        speakingState.isBeeping = true
        val playingMediaPlayer = mediaPlayer
        if (playingMediaPlayer != null) {
            if (playingMediaPlayer.isPlaying) {
                playingMediaPlayer.stop()
            }
            playingMediaPlayer.release()
            mediaPlayer = null
        }

        val am = this.getSystemService(AUDIO_SERVICE) as AudioManager
        mediaPlayer = MediaPlayer.create(
            this,
            _id,
            AudioAttributes.Builder().setContentType(
                AudioAttributes.CONTENT_TYPE_MUSIC
            ).setUsage(AudioAttributes.USAGE_MEDIA).build(),
            am.generateAudioSessionId()
        )
        val mp = mediaPlayer
        if (mp != null) {
            mp.setOnCompletionListener {
                speakingState.isBeeping = false
                speakingState.turnOnMicIfNeeded()
                if (callback != null) {
                    callback()
                }
            }
            speakingState.stoppedListening(null)
            mp.start()
        } else {
            speakingState.isBeeping = false
            speakingState.turnOnMicIfNeeded()
        }
    }

    companion object {

        /**
         * Could not import android.app.ActivityThread
         * See https://stackoverflow.com/questions/37709918/warning-do-not-place-android-context-classes-in-static-fields-this-is-a-memory
         */
        @SuppressLint("StaticFieldLeak")
        var context: Context? = null

        private const val STATE_START = 0
        private const val STATE_READY = 1
        private const val STATE_DONE = 2
        private const val STATE_MIC = 3

        var isBluetoothMicRecording: Boolean = false
        var hasSeenWakeWord: Boolean = false
        var watchingForWakeWord: Boolean = false
        var isAwake: Boolean = false
        var wakeWord: String = "computer"

        /**
         *  Used to handle permission request
         */
        private const val PERMISSIONS_REQUEST_RECORD_AUDIO = 1
    }

    override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (speakingState.shouldPlayAudioOnFocusGain()) {
                    mediaPlayer?.start()
                }
                speakingState.readyToListen()
                speakingState.turnOnMicIfNeeded()
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                speakingState.audioFocusLoss()
                MainDispatcher.dispatcher.lostFocus()
                speakingState.stoppedListening(null)
                pause()
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.pause()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                speakingState.audioFocusLossTransient(mediaPlayer?.isPlaying ?: false)
                speakingState.stoppedListening(null)
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.pause()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                speakingState.audioFocusLossTransient(mediaPlayer?.isPlaying ?: false)
                speakingState.stoppedListening(null)
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.pause()
                }
            }
        }
    }
}

