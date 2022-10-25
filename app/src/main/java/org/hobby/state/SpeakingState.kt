package org.hobby.state

import org.hobby.activity.MainActivity

class SpeakingState(private val turnOnMic: ()->Boolean, private val playMicOnSound: ()->Boolean, private val stopListening: ()->Boolean) {
    var isDucked = false
        private set
    /**
     * whether we're playing audio for an alert sound like the 'now listening'
     * sound, and thus don't want mic input until it ends
     */
    var isBeeping = false

    /**
     * number of utterances active via textToSpeech, and thus
     * don't want to record mic input, as it would corrupt the
     * stream and we might try to execute feedback from an action
     */
    private var speakingCount = 0
    private val focusLock = Any()
    private var intentToListen = false
    private var probablyListening = false
    private var micOn = false
    private var playbackDelayed = false
    private var resumeOnFocusGain = true

    fun shouldPlayAudioOnFocusGain(): Boolean {
        return playbackDelayed || resumeOnFocusGain
    }

    fun audioFocusLoss() {
        synchronized(focusLock) {
            resumeOnFocusGain = false
            playbackDelayed = false
            isDucked = true
            MainActivity.isAwake = false
        }
    }

    fun audioFocusLossTransient(isMediaPlaying: Boolean) {
        synchronized(focusLock) {
            // only resume if playback is being interrupted
            resumeOnFocusGain = isMediaPlaying
            playbackDelayed = isMediaPlaying
            isDucked = true
            MainActivity.isAwake = false
        }
    }

    fun utteranceFinished() {
        --speakingCount
        if (speakingCount < 0) {
            speakingCount = 0
        }
        if (speakingCount == 0) {
            playSoundThatMicWillTurnOnIfNeeded()
        }
    }

    fun startUtterance() {
        ++speakingCount
    }

    private fun canListen(): Boolean {
        return !isBeeping && !isDucked && speakingCount == 0  && intentToListen && !micOn
    }

    private fun canPlayOnSound(): Boolean {
        return !isBeeping && !isDucked && speakingCount == 0  && intentToListen && !probablyListening
    }

    fun pleaseListen() {
        intentToListen = true
        turnOnMicIfNeeded()
    }

    fun turnOnMicIfNeeded() {
        if (canListen()) {
            if(turnOnMic()){
                probablyListening = true
                micOn = true
            }
        }
    }

    fun playSoundThenEnableMic() {
        intentToListen = true
        playSoundThatMicWillTurnOnIfNeeded()
    }

    private fun playSoundThatMicWillTurnOnIfNeeded() {
        if (canPlayOnSound()) {
            if(playMicOnSound()){
                probablyListening = true
            }
        }
    }

    fun stoppedListening(wantToListen: Boolean?) {
        if (probablyListening) {
            stopListening()
        }
        probablyListening = false
        micOn = false
        if (wantToListen != null) {
            intentToListen = wantToListen
        }
    }

    fun readyToListen() {
        synchronized(focusLock) {
            isDucked = false
            playbackDelayed = false
            resumeOnFocusGain = false
            MainActivity.isAwake = true
        }
    }

    fun wokeUp() {
        MainActivity.isAwake = true
    }

    fun startSleeping() {
        MainActivity.isAwake = false
    }
}