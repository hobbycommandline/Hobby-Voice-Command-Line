package org.hobby.dispatcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle

abstract class CountedBroadcastReceiver: BroadcastReceiver() {
    var activeBroadcasts = 0
}

abstract class FakeCountedBroadcastReceiver {
    abstract fun onReceive(context: Context, intent: Intent, resultCode: Int, resultData: String?, resultBundle: Bundle?)
}