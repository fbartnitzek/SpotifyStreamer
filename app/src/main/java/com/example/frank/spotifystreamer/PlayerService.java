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
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

/**
 * Created by frank on 07.08.15.
 */
public class PlayerService extends Service implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

    private static final String LOG_TAG = PlayerService.class.getName();
    private boolean mIsPlaying;
    private boolean mStateIsCompleted;
    private int mStateProgress;
    private String mUrl;
    private Handler mProgressHandler;
    private MediaPlayer mPlayer;
    private final IBinder mPlayerBinder = new PlayerBinder();


    private Runnable mProgressRunnable = new Runnable() {
        @Override
        public void run() {
            mStateProgress = mPlayer.getCurrentPosition();
            mProgressHandler.postDelayed(this, Constants.UPDATE_INTERVAL);
        }
    };
    private int mStateDuration = Constants.TRACK_DEFAULT_LENGTH;

    private void watchProgress() {
        mProgressHandler = new Handler();
        mProgressHandler.post(mProgressRunnable );
    }

    public int getmStateDuration() {
        return mStateDuration;
    }


    private WifiManager.WifiLock mWifiLock;

    /*  states
        created => idle
        setDataSource => initialized
        prepare[Async] => prepared
        start => started    ... => playbackComplete
        pause => paused
        seekTo ...
        stop  => stopped
     */

    public class PlayerBinder extends Binder {
        PlayerService getService() {
            Log.v(LOG_TAG, "in PlayerBinder.getService - returning PlayerService");
            return PlayerService.this;
        }
    }

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
//        mIsPlaying = false;
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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void pause() {
        Log.v(LOG_TAG, "pause");
        if (mPlayer != null) {
            mPlayer.pause();
            mIsPlaying = false;
        }
    }

    public void resume() {
        Log.v(LOG_TAG, "resume");
        mPlayer.start();
        mIsPlaying = true;
    }

    public void seekToPositon(int milliSeconds){

//        Log.v(LOG_TAG, "seeks to position " + milliSeconds + "ms)");
        mPlayer.seekTo(milliSeconds);

    }

    public void initPlayerAndStart() {
//        Log.v(LOG_TAG, "initPlayerAndStart");

        // state stuff
        mStateProgress = 0;
        mIsPlaying = false;
        mStateIsCompleted = false;

        if (mPlayer != null) {
//            Log.v(LOG_TAG, "initPlayerAndStart - existing player released");
            mPlayer.release();
        }

        mPlayer = new MediaPlayer();
        mPlayer.setOnPreparedListener(this);
        mPlayer.setOnErrorListener(this);
        mPlayer.setOnCompletionListener(this);

        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mPlayer.setDataSource(mUrl);
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "an error happened while opening stream",
                    Toast.LENGTH_LONG).show();
        }

        mPlayer.prepareAsync();
//        Log.v(LOG_TAG, "initPlayerAndStart - player initialized and async prepared");
    }

    @Override
    public void onPrepared(MediaPlayer mp) {

        mp.start();
        mIsPlaying = true;
        // mplayer.getDuration with track-duration, not preview-duration...
        mStateDuration = mp.getDuration();
        watchProgress();

        if (mWifiLock == null){
            mWifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                    .createWifiLock(WifiManager.WIFI_MODE_FULL, Constants.TAG_WIFI_LOCK);
        }
        mWifiLock.acquire();

//        Log.v(LOG_TAG, "onPrepared - player is started & playing, progress tracked, duration: "
//                + mPlayer.getDuration());
    }


    @Override
    public void onCompletion(MediaPlayer mp) {

        mStateIsCompleted = true;
        mIsPlaying = false;
        if (mWifiLock != null && mWifiLock.isHeld()) {
            mWifiLock.release();
        }
        // release and GC
        mProgressHandler.removeCallbacks(mProgressRunnable);
        mPlayer.release();
        mPlayer = null;

//        Log.v(LOG_TAG, "onCompletion - stop player and progressHandler");
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        // handle errors appropriate...
        Log.v(LOG_TAG, "an error happened...");

        // TODO: some better error handling ... MediaPlayer.MEDIA_ERROR_SERVER_DIED
        Toast.makeText(this, "an error happened while streaming the media", Toast.LENGTH_LONG);

        // reset player
        mp.reset();
        return true;
    }

    public void setmUrl(String mUrl) { this.mUrl = mUrl; }

    public int getStateProgress() { return mStateProgress; }

    public boolean isStateIsCompleted() {
        return mStateIsCompleted;
    }

    public boolean isPlaying() {
        return mIsPlaying;
    }

}
