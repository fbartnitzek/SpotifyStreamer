package com.example.frank.spotifystreamer;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

/**
 * Created by frank on 07.08.15.
 */
public class PlayerService extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {
    private static final String LOG_TAG = PlayerService.class.getSimpleName();

    private WifiManager.WifiLock mWifiLock;
    private final Handler mHandler = new Handler();
    private LocalBroadcastManager mBroadcastManager;

    private MediaPlayer mPlayer;
    private TrackParcelable[] mTracks;
    private int mCurrentTrackIndex;
    private boolean mIsPlaying = false;
    private final IBinder mPlayerBinder = new PlayerBinder();
    private TrackParcelable mCurrentTrack;
    private int mSelectedMilliSeconds;
//    private boolean mIsFullyLoaded = false;

    /*  states
        created => idle
        setDataSource => initialized
        prepare[Async] => prepared
        start => started    ... => playbackComplete
        pause => paused
        seekTo ...
        stop  => stopped
     */


    private Runnable mTrackUpdater = new Runnable() {
        @Override
        public void run() {
            if (mPlayer != null){

                // only post if fully loaded
                // TODO: partial posts?

                if (mIsPlaying){
                    if (!mWifiLock.isHeld()){
                        mWifiLock.acquire();
                    }
                    // broadcast intent to UI (fragment)
                    sendUpdate();
//                    Intent intent = new Intent(Constants.ACTION_TRACK_STATE); // action, not tag...
//                    intent.putExtra(Constants.EXTRA_CURRENT_TRACK, mCurrentTrack);
//                    intent.putExtra(Constants.EXTRA_TRACK_NUMBER, mCurrentTrackIndex);
//                    intent.putExtra(Constants.EXTRA_IS_PLAYING, mPlayer.isPlaying());
//                    intent.putExtra(Constants.EXTRA_CURRENT_TRACK_POSITION, mPlayer.getCurrentPosition());
//                    intent.putExtra(Constants.EXTRA_TRACK_DURATION, mPlayer.getDuration());
////                    intent.putExtra(Constants.EXTRA_NEW_TRACK, false);
//                    mBroadcastManager.sendBroadcast(intent);
                } else {
                    if (mWifiLock.isHeld()){
                        mWifiLock.release();
                    }
                }


            }
            mHandler.postDelayed(this, Constants.UPDATE_INTERVAL);
        }
    };

    public boolean isPlaying() {
        return mIsPlaying;
    }

    @Override
    public void onCreate() {
        Log.v(LOG_TAG, "in onCreate - instance " + this.hashCode());
        super.onCreate();

//        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        initPlayer();

        // use broadcast manager for seekbar
//        mSeekIntent = new Intent(BROADCAST_SEEK);
        mBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(LOG_TAG, "onDestroy, wifi is held: " + mWifiLock.isHeld());
//        if (mPlayer != null) {
//            if (mPlayer.isPlaying()){
//                mPlayer.stop();
//            }
//            mPlayer.release();
//        }
//        mWifiLock.release();


        mPlayer.release();

        // maybe earlier...
        mHandler.removeCallbacks(mTrackUpdater);

    }

    public void resetPlayer() {
        Log.v(LOG_TAG, "resetting player");
        mIsPlaying = false;

        mPlayer.reset();
    }

    private void initPlayer() {
        Log.v(LOG_TAG, "initializing player");
//        mSelectedMilliSeconds = 0;
        if (mPlayer != null){
            mPlayer.release();
        }
        mPlayer = new MediaPlayer();

        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mPlayer.setOnPreparedListener(this);
        mPlayer.setOnCompletionListener(this);
        mPlayer.setOnErrorListener(this);
//        mPlayer.setOnSeekCompleteListener(this);
//        mPlayer.setOnInfoListener(this);

//        mPlayer.reset();
        resetPlayer();


        if (mWifiLock == null){
            mWifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                    .createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");
        } else {
            if (!mWifiLock.isHeld()){
                mWifiLock.acquire();
            }
        }

        Log.v(LOG_TAG, "initialized player");
    }

    public void setmTracks(TrackParcelable[] mTracks) {
        Log.v(LOG_TAG, mTracks.length + " tracks set");
        this.mTracks = mTracks;
    }


    public void pause() {
        Log.v(LOG_TAG, "pause");
        mPlayer.pause();
        mIsPlaying = false;
    }

    public void resume(){
        Log.v(LOG_TAG, "resume track " + mCurrentTrack.getName());
        mPlayer.start();
        mIsPlaying = true;
    }

    public void seekToPositon(int milliSeconds){

        if (mIsPlaying){
            Log.v(LOG_TAG, "seeks to postion " + mCurrentTrack.getName()
                    + " (" + milliSeconds + "ms)");
            mPlayer.seekTo(milliSeconds);
        } else {
            Log.v(LOG_TAG, "waits to seek to postion " + mCurrentTrack.getName()
                    + " (" + milliSeconds + "ms)");
            mSelectedMilliSeconds = milliSeconds;
        }

    }

    public void startTrack(int number) {

//        Log.v(LOG_TAG, "startTrack tracks: " + mTracks.toString());
//        initPlayer();
        resetPlayer();
        mCurrentTrackIndex = number;
        mCurrentTrack = mTracks[number];

        Log.v(LOG_TAG, "startTrack " + mCurrentTrack.getName()
                + " (Nr. " + mCurrentTrackIndex +")");
        String trackUri = mCurrentTrack.getPreviewUrl();

        mIsPlaying = false;
//        mIsFullyLoaded = false;
        try {
            mPlayer.setDataSource(trackUri);
        } catch (IOException e) {
            Log.e(LOG_TAG, "an error happened while trying to play");
        }
        mPlayer.prepareAsync();

    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.v(LOG_TAG, "onBind");
        return mPlayerBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        Log.v(LOG_TAG, "onRebind");
        super.onRebind(intent);
    }



    @Override
    public boolean onUnbind(Intent intent) {
        Log.v(LOG_TAG, "onUnbind");
        mIsPlaying = false;
//        mPlayer.stop();
//        mPlayer.release();    // lets application crash on reset
//        mWifiLock.release();    // also onPause / onStop
        return true;    // will call onRebind instead
    }

    @Override
    public void onCompletion(MediaPlayer mp) {

//        if (mp.isPlaying()){
        if (mIsPlaying){
            mp.stop();
            mIsPlaying = false;
        }

//        this.stopSelf(); // would kill service after first track...

        Log.v(LOG_TAG, "onCompletion");
    }



    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        // handle errors appropriate...
        Log.v(LOG_TAG, "an error happened...");

        // TODO: some better error handling ... MediaPlayer.MEDIA_ERROR_SERVER_DIED
        Toast.makeText(this, "an error happened while streaming the media", Toast.LENGTH_LONG);

        // reset player
        mIsPlaying = false;
        mp.reset();
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.v(LOG_TAG, "onPrepared");

        mPlayer = mp;
        if (!mp.isPlaying()) {
            mp.start();
            if (mSelectedMilliSeconds>0){
                mp.seekTo(mSelectedMilliSeconds);
                mSelectedMilliSeconds = 0;
            }
        }
        mIsPlaying = true;
//        mIsFullyLoaded = true;

        mHandler.postDelayed(mTrackUpdater, Constants.UPDATE_INTERVAL);
        sendUpdate();
//        Intent intent = new Intent(Constants.ACTION_TRACK_STATE);
//        intent.putExtra(Constants.EXTRA_TRACK_NUMBER, mCurrentTrackIndex);
//        intent.putExtra(Constants.EXTRA_CURRENT_TRACK_POSITION, 0);
//        intent.putExtra(Constants.EXTRA_IS_PLAYING, mp.isPlaying());
//        intent.putExtra(Constants.EXTRA_TRACK_DURATION, mp.getDuration());
//        intent.putExtra(Constants.EXTRA_CURRENT_TRACK, mCurrentTrack);
//        intent.putExtra(Constants.EXTRA_NEW_TRACK, true);
//        mBroadcastManager.sendBroadcast(intent);
    }

    private void sendUpdate(){
        Intent intent = new Intent(Constants.ACTION_TRACK_STATE);
        intent.putExtra(Constants.EXTRA_TRACK_NUMBER, mCurrentTrackIndex);
        intent.putExtra(Constants.EXTRA_CURRENT_TRACK, mCurrentTrack);
        intent.putExtra(Constants.EXTRA_IS_PLAYING, mPlayer.isPlaying());
        if (mIsPlaying) {
            intent.putExtra(Constants.EXTRA_CURRENT_TRACK_POSITION, mPlayer.getCurrentPosition());
            intent.putExtra(Constants.EXTRA_TRACK_DURATION, mPlayer.getDuration());
        }

//        intent.putExtra(Constants.EXTRA_NEW_TRACK, true);
        mBroadcastManager.sendBroadcast(intent);
    }


    public class PlayerBinder extends Binder {
        PlayerService getService() {
            Log.v(LOG_TAG, "in PlayerBinder.getService - returning PlayerService");
            return PlayerService.this;
        }
    }

}
