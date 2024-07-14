package com.adempolat.tvlivestreamapp

import android.content.Context
import android.widget.Filter
import android.widget.Filterable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Collections

class ChannelAdapter(
    private val channels: MutableList<Channel>,
    private val onChannelClick: (Channel) -> Unit
) : RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder>(), ChannelMoveCallback.ChannelTouchHelperContract, Filterable {

    private var filteredChannels: MutableList<Channel> = channels.toMutableList()

    inner class ChannelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val channelName: TextView = itemView.findViewById(R.id.channel_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_channel, parent, false)
        return ChannelViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        val channel = filteredChannels[position]
        holder.channelName.text = channel.name
        holder.itemView.setOnClickListener { onChannelClick(channel) }
    }

    override fun getItemCount(): Int {
        return filteredChannels.size
    }

    override fun onRowMoved(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(filteredChannels, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(filteredChannels, i, i - 1)
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

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val charString = constraint?.toString() ?: ""
                filteredChannels = if (charString.isEmpty()) channels else {
                    val filteredList = mutableListOf<Channel>()
                    channels
                        .filter { it.name.contains(charString, true) }
                        .forEach { filteredList.add(it) }
                    filteredList
                }
                return FilterResults().apply { values = filteredChannels }
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredChannels = results?.values as MutableList<Channel>
                notifyDataSetChanged()
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "tv_prefs"
        private const val KEY_CHANNEL_ORDER = "channel_order"
    }
}
