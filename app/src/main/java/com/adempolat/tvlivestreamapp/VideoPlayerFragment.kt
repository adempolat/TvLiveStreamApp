package com.adempolat.tvlivestreamapp

import android.app.AlertDialog
import android.content.Context
import android.content.res.Configuration
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import com.adempolat.tvlivestreamapp.databinding.FragmentVideoPlayerBinding

class VideoPlayerFragment : Fragment(), SurfaceHolder.Callback {

    private var _binding: FragmentVideoPlayerBinding? = null
    private val binding get() = _binding!!
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var surfaceHolder: SurfaceHolder
    private var currentChannelIndex = 0
    private var currentPosition = 0
    private var initialRxBytes: Long = 0
    private var initialTxBytes: Long = 0
    private val PREFS_NAME = "tv_prefs"
    private val KEY_CHANNEL_INDEX = "channel_index"
    private val KEY_POSITION = "position"
    private val handler = Handler(Looper.getMainLooper())
    private val hideButtonsRunnable = Runnable { hideFullscreenButtons() }
    private var isDialogShown = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVideoPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        initializePlayer()

        binding.btnPlayPause.setOnClickListener {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            } else {
                mediaPlayer?.start()
            }
        }

        binding.btnPrev.setOnClickListener {
            val sortedChannelList = loadChannelOrder()
            if (currentChannelIndex > 0) {
                currentChannelIndex--
                saveCurrentChannelIndex()
                playChannel(sortedChannelList[currentChannelIndex].url)
            }
        }

        binding.btnNext.setOnClickListener {
            val sortedChannelList = loadChannelOrder()
            if (currentChannelIndex < sortedChannelList.size - 1) {
                currentChannelIndex++
                saveCurrentChannelIndex()
                playChannel(sortedChannelList[currentChannelIndex].url)
            }
        }

        // Fullscreen mode buttons
        binding.btnFullscreenPrev.setOnClickListener {
            val sortedChannelList = loadChannelOrder()
            if (currentChannelIndex > 0) {
                currentChannelIndex--
                saveCurrentChannelIndex()
                playChannel(sortedChannelList[currentChannelIndex].url)
            }
            hideButtonsAfterDelay()
        }

        binding.btnFullscreenNext.setOnClickListener {
            val sortedChannelList = loadChannelOrder()
            if (currentChannelIndex < sortedChannelList.size - 1) {
                currentChannelIndex++
                saveCurrentChannelIndex()
                playChannel(sortedChannelList[currentChannelIndex].url)
            }
            hideButtonsAfterDelay()
        }

        binding.surfaceView.setOnClickListener {
            showFullscreenButtons()
            hideButtonsAfterDelay()
        }

        checkInternetConnection()
        checkLastPlayedChannel()
        updateLayoutForOrientation(resources.configuration.orientation)
        initialRxBytes = TrafficStats.getMobileRxBytes()
        initialTxBytes = TrafficStats.getMobileTxBytes()
    }

    private fun checkInternetConnection() {
        if (!isInternetAvailable(requireContext())) {
            showNoInternetDialog()
        }
    }

    private fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun showNoInternetDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Bağlantı Hatası")
            .setMessage("İnternet bağlantınız yok. Lütfen bağlantınızı kontrol edin.")
            .setPositiveButton("Tamam") { dialog, _ ->
                if (isInternetAvailable(requireContext())) {
                    dialog.dismiss()
                } else {
                    showNoInternetDialog()
                }
            }
            .setCancelable(false)
            .show()
    }

    override fun onResume() {
        super.onResume()
        surfaceHolder = binding.surfaceView.holder
        surfaceHolder.addCallback(this)
        checkLastPlayedChannel()
        // calculateAndShowDataUsage()

    }

    override fun onPause() {
        super.onPause()
        currentPosition = mediaPlayer?.currentPosition ?: 0
        saveCurrentChannelPosition()
        if (activity?.isInPictureInPictureMode != true) {
            releasePlayer()
        }
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        releasePlayer()
        handler.removeCallbacks(hideButtonsRunnable)
        _binding = null
    }

    private fun setupRecyclerView() {
        val sortedChannelList = loadChannelOrder()
        val adapter = ChannelAdapter(sortedChannelList) { channel ->
            currentChannelIndex = sortedChannelList.indexOf(channel)
            saveCurrentChannelIndex()
            playChannel(channel.url)
        }

        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerView.adapter = adapter

        val callback = ChannelMoveCallback(adapter)
        val touchHelper = ItemTouchHelper(callback)
        touchHelper.attachToRecyclerView(binding.recyclerView)
    }

    private fun loadChannelOrder(): MutableList<Channel> {
        val sharedPref = activity?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedOrder = sharedPref?.getString(KEY_CHANNEL_ORDER, null)
        return if (savedOrder != null) {
            val order = savedOrder.split(",")
            val sortedList = mutableListOf<Channel>()
            order.forEach { name ->
                val channel = channelList.find { it.name == name }
                if (channel != null) {
                    sortedList.add(channel)
                }
            }
            sortedList
        } else {
            channelList.toMutableList()
        }
    }

    private fun initializePlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer()
            mediaPlayer?.setOnPreparedListener {
                it.start()
            }
        } else {
            mediaPlayer?.reset()
            mediaPlayer?.setDisplay(surfaceHolder)
        }
    }

    private fun playChannel(url: String) {
        try {
            mediaPlayer?.reset()
            mediaPlayer?.setDisplay(surfaceHolder)
            mediaPlayer?.setDataSource(requireContext(), Uri.parse(url))
            mediaPlayer?.prepareAsync()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releasePlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun saveCurrentChannelIndex() {
        val sharedPref = activity?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            putInt(KEY_CHANNEL_INDEX, currentChannelIndex)
            apply()
        }
    }

    private fun saveCurrentChannelPosition() {
        val sharedPref = activity?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            putInt(KEY_POSITION, currentPosition)
            apply()
        }
    }

    private fun checkLastPlayedChannel() {
        if (!isDialogShown) {
            val sharedPref = activity?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lastChannelIndex = sharedPref?.getInt(KEY_CHANNEL_INDEX, -1) ?: -1
            if (lastChannelIndex != -1) {
                //showResumeDialog(lastChannelIndex)
            }
        }
    }

    private fun updateLayoutForOrientation(orientation: Int) {
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            enterFullScreen()
            binding.arrowChannels.visibility = View.GONE
            binding.recyclerView.visibility = View.GONE
            hideFullscreenButtons()
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            exitFullScreen()
            binding.arrowChannels.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.VISIBLE
            binding.btnFullscreenPrev.visibility = View.GONE
            binding.btnFullscreenNext.visibility = View.GONE
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun enterFullScreen() {
        activity?.window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
    }

    private fun exitFullScreen() {
        activity?.window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
    }

    private fun showFullscreenButtons() {
        binding.btnFullscreenPrev.visibility = View.VISIBLE
        binding.btnFullscreenNext.visibility = View.VISIBLE
    }

    private fun hideFullscreenButtons() {
        binding.btnFullscreenPrev.visibility = View.GONE
        binding.btnFullscreenNext.visibility = View.GONE
    }

    private fun hideButtonsAfterDelay() {
        handler.removeCallbacks(hideButtonsRunnable)
        handler.postDelayed(hideButtonsRunnable, 5000)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateLayoutForOrientation(newConfig.orientation)
    }


    override fun surfaceCreated(holder: SurfaceHolder) {
        mediaPlayer?.setDisplay(holder)
        mediaPlayer?.seekTo(currentPosition)
        mediaPlayer?.start()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // Handle surface changes if needed
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        mediaPlayer?.setDisplay(null)
    }

    private fun calculateAndShowDataUsage() {
        val finalRxBytes = TrafficStats.getMobileRxBytes()
        val finalTxBytes = TrafficStats.getMobileTxBytes()

        val dataUsed = (finalRxBytes - initialRxBytes) + (finalTxBytes - initialTxBytes)
        val dataUsedInMB = dataUsed / (1024 * 1024)

        Toast.makeText(requireContext(), "Kullanılan Mobil Veri: $dataUsedInMB MB", Toast.LENGTH_LONG).show()
    }

    companion object {
        private const val PREFS_NAME = "tv_prefs"
        private const val KEY_CHANNEL_ORDER = "channel_order"
    }
}
