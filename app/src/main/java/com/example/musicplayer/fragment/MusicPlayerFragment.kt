package com.example.musicplayer.fragment


import android.annotation.SuppressLint
import android.content.*
import android.content.Context.BIND_AUTO_CREATE
import android.media.MediaPlayer
import android.net.Uri.parse
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.widget.SeekBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.musicplayer.R
import com.example.musicplayer.databinding.FragmentMusicPlayerBinding
import com.example.musicplayer.model.Song
import com.example.musicplayer.service.MusicPlayerService
import com.example.musicplayer.utils.ConnectivityObserver
import com.example.musicplayer.utils.Contanst
import com.example.musicplayer.utils.NetworkConnectivityObserver
import com.example.musicplayer.utils.formatSongDuration
import com.example.musicplayer.vm.*
import kotlinx.coroutines.launch

class MusicPlayerFragment : Fragment(), ServiceConnection, MediaPlayer.OnCompletionListener {
    companion object {
        const val LOG: String = "TCR"
        var musicPlayerService: MusicPlayerService? = null
        var isPlaying: Boolean = false

        @SuppressLint("StaticFieldLeak")
        lateinit var binding: FragmentMusicPlayerBinding
        lateinit var connectivityObserver: ConnectivityObserver
        lateinit var animation: Animation
    }

    private val viewModel: MusicPlayerViewModel by viewModels()
    private val favouriteViewModel: FavouriteViewModel by viewModels()
    private val songViewModel: SongViewModel by viewModels()
    private val playlistViewModel: PlaylistViewModel by viewModels()
    private val downloadViewModel: DownloadViewModel by viewModels()
    private var onCompleted: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            downloadViewModel.stopDownloadService()
            val url = intent.getStringExtra("downloaded")
            Log.d(Contanst.TAG, "downloaded1: ${url.toString()}")
            //downloadViewModel.updateUrlSong(url!!)
            if (url != null) {
                downloadViewModel.updateUrlSong(url)
            }
            //songViewModel.updateLocalSongs()
            Toast.makeText(context, "Download success", Toast.LENGTH_LONG).show()
            //unregister()
        }
    }

    private fun unregister() {
        requireActivity().unregisterReceiver(onCompleted)
    }

    private var getID: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMusicPlayerBinding.inflate(inflater, container, false)

        val song = requireActivity().intent.getSerializableExtra("song") as Song
        binding.song = song
        binding.favourite = favouriteViewModel
        binding.lifecycleOwner = this
        favouriteViewModel.checkFavouriteSong(song.idSong!!)
        /*favouriteViewModel.checkSong.observe(viewLifecycleOwner){
            if (it!=null) {
                //binding.btnFavourite.setImageResource(R.drawable.ic_favorite)
                Log.d(Contanst.TAG, "like")
            } else {
               // binding.btnFavourite.setImageResource(R.drawable.ic_unfavorite)
                Log.d(Contanst.TAG, "unlike")
            }
        }*/
        binding.btnFavourite.setOnClickListener() {
            favouriteViewModel.checkFavouriteCheckUnCheck(song)
        }

        binding.btnAddPlaylist.setOnClickListener() {
            songViewModel.setSelectSong(song)
            findNavController().navigate(
                MusicPlayerFragmentDirections.actionMusicPlayerFragment2ToAddToPlaylistFragment2(
                    song.nameSong!!
                )
            )

        }

        binding.btnDownload.setOnClickListener() {
            LocalBroadcastManager.getInstance(requireActivity())
                .registerReceiver(onCompleted, IntentFilter("Download"))
            downloadViewModel.startDownloadService(song)
        }
//        lifecycleScope.launch {
//            viewModel.getSongById(1004).observe(viewLifecycleOwner) {
//                it?.let {
//                    when (it.status) {
//                        Status.SUCCESS ->
//                        Status.LOADING-> Log.d(LOG, "Loading data MusicPlayerViewModel")
//                        Status.ERROR -> Log.d(LOG, "Error data MusicPlayerViewModel")
//                    }
//
//                }
//
//            }}
        // getID = requireActivity().intent.getIntExtra("idSong", 1003)
        getID = song.idSong!!
        Log.d(LOG, getID.toString())


        //initial check internet
        connectivityObserver = NetworkConnectivityObserver(requireContext())

        //Animation disk
        animation = AnimationUtils.loadAnimation(requireContext(), R.anim.animation_rotate)

        animation.repeatCount = Animation.INFINITE
        //smooth animation rotate
        animation.interpolator = LinearInterpolator()

        //Play music
        controlMusic()
        binding.musicDisc.startAnimation(animation)


        //Control
        binding.btnPlayStopMusic.setOnClickListener {
            if (isPlaying) {
                pauseMusic()
                binding.musicDisc.clearAnimation()
            } else {
                binding.btnPlayStopMusic.setImageResource(R.drawable.ic_pause)
                playMusic()
                binding.musicDisc.startAnimation(animation)
            }

        }

        //Seekbar
        binding.seekbarTime.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) musicPlayerService!!.mediaPlayer!!.seekTo(progress)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                }

            }
        )


        //Start service
        val intent = Intent(requireContext(), MusicPlayerService::class.java)
        activity?.bindService(intent, this, BIND_AUTO_CREATE)
        activity?.startService(intent)

        Log.d(LOG, "CreateView")


        return binding.root
    }

    override fun onResume() {
        super.onResume()
        Log.d(LOG, "Resume")
    }

    override fun onPause() {
        super.onPause()
        Log.d(LOG, "Pause")
    }

    override fun onStop() {
        super.onStop()
        Log.d(LOG, "Stop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(LOG, "Destroy")
    }

    @SuppressLint("SetTextI18n")
    private fun controlMusic() {
        lifecycleScope.launch {
            connectivityObserver.observer().collect {
                when (it) {
                    ConnectivityObserver.StatusInternet.AVAILABLE -> {
                        viewModel.songById(getID).observe(viewLifecycleOwner) { song ->
                            viewModel.setSong(song)
                            Log.d(LOG, song.urlImage.toString())

                            Glide.with(this@MusicPlayerFragment)
                                .load(song.urlImage.toString()).circleCrop()
                                .into(binding.musicDisc)

                            createMediaPlayer(song.urlSong!!)
                        }
                        binding.vm = viewModel
                        binding.lifecycleOwner = this@MusicPlayerFragment
                    }
                    ConnectivityObserver.StatusInternet.LOSING -> {
                        binding.tvNameSong.text = "$it"
                    }
                    ConnectivityObserver.StatusInternet.LOST -> {
                        binding.tvNameSong.text = "$it"
                    }
                    ConnectivityObserver.StatusInternet.UNAVAILABLE -> {
                        binding.tvNameSong.text = "$it"
                    }
                }
            }
        }
    }

    private fun getImage(link: String, listSong: List<Song>) {
        Glide.with(this@MusicPlayerFragment)
            .load(listSong[1].urlImage.toString()).circleCrop()
            .into(binding.musicDisc)
    }

    private fun createMediaPlayer(link: String) {
        try {
            if (musicPlayerService?.mediaPlayer == null) musicPlayerService?.mediaPlayer =
                MediaPlayer()
            musicPlayerService?.mediaPlayer?.reset()
            musicPlayerService?.mediaPlayer?.setDataSource(parse(link).toString())
            musicPlayerService?.mediaPlayer?.prepare()
            musicPlayerService?.mediaPlayer?.start()
            isPlaying = true
            binding.btnPlayStopMusic.setImageResource(R.drawable.ic_pause)
            //seekbar
            binding.timeReal.text = formatSongDuration(
                musicPlayerService?.mediaPlayer?.currentPosition.toString().toLong()
            )
            binding.timeTotal.text =
                formatSongDuration(musicPlayerService?.mediaPlayer?.duration.toString().toLong())
            binding.seekbarTime.progress = 0
            binding.seekbarTime.max = musicPlayerService!!.mediaPlayer!!.duration

            musicPlayerService?.mediaPlayer?.setOnCompletionListener(this)

            Log.d(LOG, musicPlayerService?.mediaPlayer?.duration.toString())
            Log.d(LOG, "ID: ${musicPlayerService?.mediaPlayer!!.audioSessionId}")
        } catch (ex: Exception) {

        }
    }

    private fun playMusic() {
        binding.btnPlayStopMusic.setImageResource(R.drawable.ic_pause)
        musicPlayerService?.showNotification(R.drawable.ic_pause)
        isPlaying = true
        musicPlayerService?.mediaPlayer?.start()
    }

    private fun pauseMusic() {
        binding.btnPlayStopMusic.setImageResource(R.drawable.ic_play_arrow)
        musicPlayerService?.showNotification(R.drawable.ic_play_arrow)
        isPlaying = false
        musicPlayerService?.mediaPlayer?.pause()
    }

    override fun onServiceConnected(componentName: ComponentName?, service: IBinder?) {
        val binder = service as MusicPlayerService.MyBinder
        musicPlayerService = binder.currentService()
        musicPlayerService!!.showNotification(R.drawable.ic_pause)
        musicPlayerService?.seekBarSetup()
    }

    override fun onServiceDisconnected(componentName: ComponentName?) {
        musicPlayerService = null
    }

    //MediaPlayer.OnCompletionListener
    override fun onCompletion(mediaPlayer: MediaPlayer?) {
        playMusic()
    }
}