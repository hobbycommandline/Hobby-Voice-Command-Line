package org.hobby.chat

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.hobby.activity.R
class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    var textView: TextView
        get() = itemView.findViewById(R.id.chat_text)
        set(_) {}
}