package com.plcoding.spotifycloneyt.exoplayer

import android.content.ComponentName
import android.content.Context
import android.media.browse.MediaBrowser
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.plcoding.spotifycloneyt.Constants.Constants.NETWORK_ERROR
import com.plcoding.spotifycloneyt.other.Event
import com.plcoding.spotifycloneyt.other.Resource

class MusicServiceConnection(
    context: Context
) {
    private val _isConnected = MutableLiveData<Event<Resource<Boolean>>>()
    val isConnected: LiveData<Event<Resource<Boolean>>> = _isConnected

    private val _networkError = MutableLiveData<Event<Resource<Boolean>>>()
    val networkError: LiveData<Event<Resource<Boolean>>> = _networkError

    //Todo 1f playbackState is being posted in MusicControllerCallback
    private val _playbackState = MutableLiveData<PlaybackStateCompat?>()
    val playbackState: LiveData<PlaybackStateCompat?> = _playbackState

    //Todo 1g curPlayingSong is being posted in MusicControllerCallback
    private val _curPlayingSong = MutableLiveData<MediaMetadataCompat?>()
    val curPlayingSong: LiveData<MediaMetadataCompat?> = _curPlayingSong

    //Todo 1d mediaController is being initialized
    lateinit var mediaController: MediaControllerCompat

    //Todo 1e mediaBrowserConnectionCallback is responsible for initializing mediaController in the onConnected fun.
    private val mediaBrowserConnectionCallback = MediaBrowserConnectionCallback(context)

    //Todo 1b mediaBrowser created after subscribing to it. It requires a MusicService::class.java, mediaBrowserConnectionCallback.
    private val mediaBrowser = MediaBrowserCompat(
        context,
        ComponentName(
            context,
            MusicService::class.java
        ), mediaBrowserConnectionCallback,
        null
    ).apply {
        Log.i("check1", "first Run")
        connect()

    }


    val transportControls: MediaControllerCompat.TransportControls
        get() = mediaController.transportControls

    fun subscribe(parentId: String, callback: MediaBrowserCompat.SubscriptionCallback){

        mediaBrowser.subscribe(parentId, callback)
    }

    fun unsubscribe(parentId: String, callback: MediaBrowserCompat.SubscriptionCallback){
        mediaBrowser.unsubscribe(parentId, callback)
    }


    private inner class MediaBrowserConnectionCallback
        (private val context: Context
        ): MediaBrowserCompat.ConnectionCallback()
    {
        //Todo 1c MediaBrowserConnectionCallback is being established.
        override fun onConnected() {
            Log.i("check1", "Second Run")

            mediaController = MediaControllerCompat(context, mediaBrowser.sessionToken).apply {
                registerCallback(MediaControllerCallback())
            }
            _isConnected.postValue(Event(Resource.success(true)))
        }

        override fun onConnectionSuspended() {
            _isConnected.postValue(Event(Resource.error(
                "The connection was suspended", false
            )))
        }

        override fun onConnectionFailed() {
            _isConnected.postValue(Event(Resource.error(
                "Couldn't connect to media browser", false
            )))
        }
    }


    private inner class MediaControllerCallback: MediaControllerCompat.Callback(){
        //Helps to identify the state of media
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            _playbackState.postValue(state)
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            Log.i("changes", "Changes")
            _curPlayingSong.postValue(metadata)
        }

        override fun onSessionEvent(event: String?, extras: Bundle?) {
            super.onSessionEvent(event, extras)
            when(event){
                NETWORK_ERROR -> _networkError.postValue(
                    Event(
                        Resource.error(
                            "Couldn't connect to the server. Please check your network connection",
                            null
                        )
                    )
                )
            }
        }

        override fun onSessionDestroyed() {
            mediaBrowserConnectionCallback.onConnectionSuspended()
        }
    }

}