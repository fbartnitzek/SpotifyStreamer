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
    private static final String LOG_TAG = PlayerService.class.getName();

    private WifiManager.WifiLock mWifiLock;
    private final Handler mHandler = new Handler();
    private LocalBroadcastManager mBroadcastManager;

    private MediaPlayer mPlayer;
    private TrackParcelable[] mTracks;
    private int mCurrentTrackIndex;

    private final IBinder mPlayerBinder = new PlayerBinder();
    private TrackParcelable mCurrentTrack;
    private int mSelectedMilliSeconds;

    // store all ...
    private boolean mIsPlaying = false;
    private boolean mStateIsComplete;
    private int mStateProgress;


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

    public class PlayerBinder extends Binder {
        PlayerService getService() {
            Log.v(LOG_TAG, "in PlayerBinder.getService - returning PlayerService");
            return PlayerService.this;
        }
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
//        return true;    // will call onRebind instead
        return super.onUnbind(intent);
    }



    @Override
    public void onCreate() {
        Log.v(LOG_TAG, "in onCreate - instance " + this.hashCode());
        super.onCreate();

//        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // maybe to early
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

    private void initPlayer() {
        Log.v(LOG_TAG, "initializing player");
//        mSelectedMilliSeconds = 0;
        mStateProgress = 0;
        mIsPlaying = false;
        mStateIsComplete = false;

        if (mPlayer != null) {
            Log.v(LOG_TAG, "initPlayer - releasing player");
            mPlayer.release();
        }
        mPlayer = new MediaPlayer();

        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mPlayer.setOnPreparedListener(this);
        mPlayer.setOnCompletionListener(this);
        mPlayer.setOnErrorListener(this);

//        mPlayer.reset();
//        resetPlayer();

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

//    public void resetPlayer() {
//        Log.v(LOG_TAG, "resetting player");
//        mIsPlaying = false;
//
//        mPlayer.reset();
//    }

    public void startTrack(int number) {

//        Log.v(LOG_TAG, "startTrack tracks: " + mTracks.toString());
//        initPlayer();
//        resetPlayer();

        Log.v(LOG_TAG, "startTrack " + number);
        initPlayer();

        mCurrentTrackIndex = number;
        mCurrentTrack = mTracks[number];

        Log.v(LOG_TAG, "startTrack " + mCurrentTrack.getName()
                + " (Nr. " + mCurrentTrackIndex + ")");
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

    @Override
    public void onPrepared(MediaPlayer mp) {

        if (!mp.isPlaying()) {
            Log.v(LOG_TAG, "onPrepared - now starts playing");
            mp.start();
            if (mSelectedMilliSeconds>0){
                Log.v(LOG_TAG, "onPrepared - and seek to " + mSelectedMilliSeconds);
                mp.seekTo(mSelectedMilliSeconds);
                mSelectedMilliSeconds = 0;
            }
        } else {
            Log.v(LOG_TAG, "onPrepared - already playing...?");
        }
        mIsPlaying = true;
//        mIsFullyLoaded = true;

        mHandler.postDelayed(mTrackUpdater, Constants.UPDATE_INTERVAL);
        sendUpdate();
    }


    @Override
    public void onCompletion(MediaPlayer mp) {

        mStateIsComplete = true;
        mIsPlaying = false;
        // release and GC
        mPlayer.release();
        mPlayer = null;

        Log.v(LOG_TAG, "onCompletion - stop player");

//        if (mp.isPlaying()){
//        if (mIsPlaying) {
//            mp.stop();
//            mIsPlaying = false;
//        }
//        this.stopSelf(); // would kill service after first track...
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


    public int getStateProgress() {
        return mStateProgress;
    }

    public boolean isStateIsComplete() {
        return mStateIsComplete;
    }

    public boolean isPlaying() {
        return mIsPlaying;
    }

}
