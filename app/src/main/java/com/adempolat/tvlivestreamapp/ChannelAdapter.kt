package com.adempolat.tvlivestreamapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.adempolat.tvlivestreamapp.Channel
import com.adempolat.tvlivestreamapp.ChannelMoveCallback
import com.adempolat.tvlivestreamapp.R
import java.util.Collections

class ChannelAdapter(
    private val channels: MutableList<Channel>,
    private val onChannelClick: (Channel) -> Unit
) : RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder>(), ChannelMoveCallback.ChannelTouchHelperContract {

    inner class ChannelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val channelName: TextView = itemView.findViewById(R.id.channel_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_channel, parent, false)
        return ChannelViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        val channel = channels[position]
        holder.channelName.text = channel.name
        holder.itemView.setOnClickListener { onChannelClick(channel) }
    }

    override fun getItemCount(): Int {
        return channels.size
    }

    override fun onRowMoved(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(channels, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(channels, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onRowSelected(viewHolder: RecyclerView.ViewHolder) {
        // İstendiğinde burada seçilen satırı vurgulamak için kod ekleyebilirsiniz
    }

    override fun onRowClear(viewHolder: RecyclerView.ViewHolder) {
        saveChannelOrder(viewHolder.itemView.context)
    }

    private fun saveChannelOrder(context: Context) {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            putString(KEY_CHANNEL_ORDER, channels.joinToString(",") { it.name })
            apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "tv_prefs"
        private const val KEY_CHANNEL_ORDER = "channel_order"
    }
}
