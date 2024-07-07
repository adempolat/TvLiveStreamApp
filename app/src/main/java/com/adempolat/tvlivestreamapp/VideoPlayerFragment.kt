package com.adempolat.tvlivestreamapp

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.adempolat.tvlivestreamapp.databinding.FragmentVideoPlayerBinding

class VideoPlayerFragment : Fragment(), SurfaceHolder.Callback {

    private var _binding: FragmentVideoPlayerBinding? = null
    private val binding get() = _binding!!
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var surfaceHolder: SurfaceHolder
    private var currentChannelIndex = 0

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
            if (currentChannelIndex > 0) {
                currentChannelIndex--
                playChannel(channelList[currentChannelIndex].url)
            }
        }

        binding.btnNext.setOnClickListener {
            if (currentChannelIndex < channelList.size - 1) {
                currentChannelIndex++
                playChannel(channelList[currentChannelIndex].url)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        surfaceHolder = binding.surfaceView.holder
        surfaceHolder.addCallback(this)
    }

    override fun onPause() {
        super.onPause()
        releasePlayer()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerView.adapter = ChannelAdapter(channelList) { channel ->
            currentChannelIndex = channelList.indexOf(channel)
            playChannel(channel.url)
        }

    }

    private fun initializePlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer()
            mediaPlayer?.setOnPreparedListener {
                it.start()
            }
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

    override fun onDestroyView() {
        super.onDestroyView()
        releasePlayer()
        _binding = null
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        mediaPlayer?.setDisplay(holder)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // Handle surface changes if needed
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        mediaPlayer?.setDisplay(null)
    }
}
