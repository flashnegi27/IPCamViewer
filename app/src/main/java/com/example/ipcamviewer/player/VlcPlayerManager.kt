package com.example.ipcamviewer.player

import android.content.Context
import android.net.Uri
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

/**
 * Thin wrapper around libVLC for playing an RTSP stream inside a
 * VLCVideoLayout. One instance is created per active video view and
 * released when the view goes away.
 */
class VlcPlayerManager(context: Context) {

    private val libVlc: LibVLC = LibVLC(
        context.applicationContext,
        arrayListOf(
            "--rtsp-tcp",
            "--network-caching=300",
            "--no-audio",
            "--drop-late-frames",
            "--skip-frames"
        )
    )

    val mediaPlayer: MediaPlayer = MediaPlayer(libVlc)

    fun attach(layout: VLCVideoLayout) {
        mediaPlayer.attachViews(layout, null, false, false)
    }

    fun play(rtspUrl: String, useTcp: Boolean = true) {
        stop()
        val media = Media(libVlc, Uri.parse(rtspUrl))
        media.addOption(if (useTcp) ":rtsp-tcp" else ":no-rtsp-tcp")
        media.addOption(":network-caching=300")
        mediaPlayer.media = media
        media.release()
        mediaPlayer.play()
    }

    fun stop() {
        if (mediaPlayer.isPlaying) mediaPlayer.stop()
    }

    fun release() {
        mediaPlayer.detachViews()
        mediaPlayer.release()
        libVlc.release()
    }
}
