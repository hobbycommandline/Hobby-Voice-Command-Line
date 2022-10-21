package org.hobby.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.hobby.activity.R

class ChatRecyclerView(private val dataSet: MutableList<String>) :
    RecyclerView.Adapter<ChatViewHolder>() {
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.chat_row_item, viewGroup, false)

        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ChatViewHolder, position: Int) {
        viewHolder.textView.text = dataSet[position]
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    fun clear() {
        val size = dataSet.size
        dataSet.clear()
        notifyItemRangeRemoved(0, size)
    }

    fun replaceLast(item: String) {
        if (dataSet.isEmpty()) {
            addItem(item)
            return
        }
        dataSet[dataSet.size - 1] = item
        notifyItemChanged(dataSet.size - 1)
    }

    fun addItem(item: String) {
        dataSet.add(item)
        notifyItemInserted(dataSet.size - 1)
    }

    fun addItems(items: List<String>) {
        if (items.isEmpty()) {
            return
        }
        val positionStart = dataSet.size
        dataSet.addAll(items)
        notifyItemRangeInserted(positionStart, items.size)
    }
}