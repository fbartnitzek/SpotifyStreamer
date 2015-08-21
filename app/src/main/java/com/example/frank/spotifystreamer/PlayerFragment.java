package com.example.frank.spotifystreamer;

import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Created by frank on 05.08.15.
 */


public class PlayerFragment extends DialogFragment
        implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    private static final String LOG_TAG = PlayerFragment.class.getName();
    private boolean mIsBound = false;
    // true = bound and connected to service
    // false = unbound and disconnected to service
    private boolean mIsInit = false;
    // true = track selected and eventually playing
    // false = no track selected / new track
    private boolean mIsPaused = false;  // playing or paused

    private ArrayList<TrackParcelable> mTracks;
    private int mPosition;
    private String mUrl;

    private PlayerService mPlayerService;
    private Handler mProgressHandler;

    private TextView mArtistView;
    private TextView mAlbumNameView;
    private ImageView mAlbumImageView;
    private TextView mTrackView;
    private TextView mElapsedView;
    private TextView mDurationView;
    private ImageButton mPreviousButton;
    private ImageButton mNextButton;
    private ImageButton mPlayButton;
    private SeekBar mSeekBar;

    private final int mDuration = Constants.TRACK_DEFAULT_LENGTH;   // workaround for unknown duration and division
    private PlayerTrackListener mTrackChangeCallback;

    public interface PlayerTrackListener {
        void onPlayerTrackChange(int position);
    }

    // Broadcast-receiver and rotation did not work so well...
    private final Runnable mProgressRunnable = new Runnable() {
        @Override
        public void run() {
            if (mPlayerService != null) {
                Log.v(LOG_TAG, "mProgressRunnable runs, mplayerService is playing: " + mPlayerService.isPlaying());
                if (mPlayerService.isPlaying()) {
                    mSeekBar.setProgress(mPlayerService.getStateProgress());
                    mSeekBar.setMax(mPlayerService.getmStateDuration());
                    mDurationView.setText(Util.formatTime(mPlayerService.getmStateDuration()));
//                    Log.v(LOG_TAG, "in run duration: " + mPlayerService.getmStateDuration());
                    mElapsedView.setText(Util.formatTime(mPlayerService.getStateProgress()
                            + Constants.UPDATE_INTERVAL));
                }

                if (mPlayerService.isStateIsCompleted()) {
                    mIsInit = false;
                    mPlayButton.setImageResource(android.R.drawable.ic_media_play);
                }
            }

            mProgressHandler.postDelayed(this, Constants.UPDATE_INTERVAL);
        }
    };
    // src: http://code.tutsplus.com/tutorials/create-a-music-player-on-android-song-playback--mobile-22778
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PlayerService.PlayerBinder playerBinder = (PlayerService.PlayerBinder) service;
            mPlayerService = playerBinder.getService();
            mIsBound = true;

            if (!mIsInit) {
                Log.v(LOG_TAG, "onServiceConnected - initPlayerAndStart");
                mPlayerService.setmUrl(mUrl);
                mPlayerService.setmTracks(mTracks);
                mPlayerService.setmPosition(mPosition);
                mPlayerService.initPlayerAndStart();

                mIsInit = true;
                mIsPaused = false;
            } else {
                mPlayerService.reconnectPlayer();
                mPosition = mPlayerService.getmPosition();
                mTracks = mPlayerService.getmTracks();
                if (mTracks == null || mTracks.isEmpty()){
                    Toast.makeText(getActivity(), R.string.now_playing_unavailable,
                            Toast.LENGTH_LONG).show();
                    return;
                }

                Log.v(LOG_TAG, "onServiceConnected - reconnectPlayer and get position " + mPosition);
                updateViewsWithCurrentTrack();
            }
            watchProgress();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) { mIsBound = false; }
    };

    private void watchProgress() {
        mProgressHandler = new Handler();
        Log.v(LOG_TAG, "watchProgress Handler created");
        mProgressHandler.post(mProgressRunnable);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            Log.v(LOG_TAG, "onCreate - restore state");
            mTracks = savedInstanceState.getParcelableArrayList(Constants.STATE_TRACKS);
            mPosition = savedInstanceState.getInt(Constants.STATE_SELECTED_TRACK);
            mIsInit = savedInstanceState.getBoolean(Constants.STATE_IS_INIT);
            mIsBound = savedInstanceState.getBoolean(Constants.STATE_IS_BOUND);
            mIsPaused = savedInstanceState.getBoolean(Constants.STATE_IS_PAUSED);
            mUrl = savedInstanceState.getString(Constants.STATE_URL);
        } else {
            Log.v(LOG_TAG, "onCreate - no state to restore");
        }

    }

    @Override
    public void onResume() {
        Log.v(LOG_TAG, "onResume ");
//        mBroadcastManager.registerReceiver(mReceiver,
//                new IntentFilter(Constants.ACTION_TRACK_STATE));
        super.onResume();
    }

    @Override
    public void onPause() {
        // to onStop
        Log.v(LOG_TAG, "on pause ");
//        mBroadcastManager.unregisterReceiver(mReceiver);
        super.onPause();
    }


    /** The system calls this to get the DialogFragment's layout, regardless
     of whether it's being displayed as a dialog or an embedded fragment. */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.v(LOG_TAG, "onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_player, container, false);

        mArtistView = (TextView) rootView.findViewById(R.id.artist_name);
        mAlbumNameView = (TextView) rootView.findViewById(R.id.album_name);
        mAlbumImageView = (ImageView) rootView.findViewById(R.id.album_image);
        mSeekBar = (SeekBar) rootView.findViewById(R.id.seek_bar);
        mSeekBar.setOnSeekBarChangeListener(this);
        mElapsedView = (TextView) rootView.findViewById(R.id.elapsed_time);
        mDurationView = (TextView) rootView.findViewById(R.id.duration);
        mTrackView = (TextView) rootView.findViewById(R.id.track_name);
        mPreviousButton = (ImageButton) rootView.findViewById(R.id.button_previous);
        mNextButton = (ImageButton) rootView.findViewById(R.id.button_next);
        mPlayButton = (ImageButton) rootView.findViewById(R.id.button_play);

        if (getArguments().containsKey(Constants.ARGS_RESTART_PLAYING) &&
                !getArguments().getBoolean(Constants.ARGS_RESTART_PLAYING)){

            mIsInit = true;
            mIsBound = true;
            mIsPaused = false;
            Log.v(LOG_TAG, "onCreateView - resume playing without args");
        }

        // init buttons and seekbar
        updatePlayButton();
        mPlayButton.setOnClickListener(this);
        mNextButton.setOnClickListener(this);
        mPreviousButton.setOnClickListener(this);
        mSeekBar.setOnSeekBarChangeListener(this);
        mSeekBar.setMax(Constants.TRACK_DEFAULT_LENGTH);

        if (!mIsInit) { // init data
            mTracks = getArguments().getParcelableArrayList(Constants.ARGS_TRACKS);
            mPosition = getArguments().getInt(Constants.ARGS_TRACK_NUMBER);
            Log.v(LOG_TAG, "onCreateView - reading args for track "
                    + mTracks.get(mPosition).getName());
            mUrl = mTracks.get(mPosition).getPreviewUrl();
            updateViewsWithCurrentTrack();
        }

        return rootView;
    }

    private void updatePlayButton(){
        if (mIsPaused){
            mPlayButton.setImageResource(android.R.drawable.ic_media_play);
        } else {
            mPlayButton.setImageResource(android.R.drawable.ic_media_pause);
        }
    }


    private void updateViewsWithCurrentTrack() {
        Log.v(LOG_TAG, "in updateViewsWithCurrentTrack position: " + mPosition
                + ", track: " + mTracks.get(mPosition).getName());

        // artist name
        TrackParcelable track = mTracks.get(mPosition);

        mArtistView.setText(track.getArtist());

        // album name
        mAlbumNameView.setText(track.getAlbum());

        // track name
        mTrackView.setText(track.getName());

        // album image
        String url = track.getPictureUrl();
        if (url != null && !url.isEmpty()) {
            Picasso.with(getActivity()).load(url).into(mAlbumImageView);
        }

        mDurationView.setText(Util.formatTime(Constants.TRACK_DEFAULT_LENGTH));
        mElapsedView.setText(Util.formatTime(0));

    }


    /** The system calls this only when creating the layout in a dialog. */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.v(LOG_TAG, "in onCreateDialog");
        // The only reason you might override this method when using onCreateView() is
        // to modify any dialog characteristics. For example, the dialog includes a
        // title by default, but your custom layout might not need it. So here you can
        // remove the dialog title, but you must call the superclass to get the Dialog.
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mTrackChangeCallback = (PlayerTrackListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement PlayerTrackListener");
        }
        Log.v(LOG_TAG, "onAttach");
    }

    @Override
    public void onStart() {
        super.onStart();

        Log.v(LOG_TAG, "onStart - starting & binding service");

        Intent intent = new Intent(getActivity(), PlayerService.class);
        getActivity().startService(intent);
        getActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.v(LOG_TAG, "onDetach");

    }

    @Override
    public void onStop() {
        super.onStop();
        Log.v(LOG_TAG, "onStop - unbind service and remove callbacks");
        getActivity().unbindService(mConnection);
        mIsBound = false;
        if (mProgressHandler != null){
            mProgressHandler.removeCallbacks(mProgressRunnable);
        }
    }

    @Override
    public void onDestroyView() {
        Log.v(LOG_TAG, "onDestroyView");
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {

        Log.v(LOG_TAG, "onDestroy");
        super.onDestroy();
    }

    private void restartPlayer() {
        mSeekBar.setProgress(0);
        mElapsedView.setText(Util.formatTime(0));
        mUrl = mTracks.get(mPosition).getPreviewUrl();
        mPlayerService.setmUrl(mUrl);
        mPlayerService.setmTracks(mTracks);
        mPlayerService.setmPosition(mPosition);
        mPlayerService.initPlayerAndStart();
        mIsInit = true;
        mIsPaused = false;
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Log.v(LOG_TAG, "onSaveInstanceState with isBound: " + mIsBound
                + ", isInit: " + mIsInit + ", isPaused: " + mIsPaused);
        if (mTracks != null) {
            outState.putParcelableArrayList(Constants.STATE_TRACKS, mTracks);
            outState.putInt(Constants.STATE_SELECTED_TRACK, mPosition);
            outState.putInt(Constants.STATE_DURATION, mDuration);
            outState.putString(Constants.STATE_URL, mUrl);
            outState.putBoolean(Constants.STATE_IS_BOUND, mIsBound);
            outState.putBoolean(Constants.STATE_IS_INIT, mIsInit);
            outState.putBoolean(Constants.STATE_IS_PAUSED, mIsPaused);
        }
    }

    @Override
    public void onClick(View view) {

        if (mPlayerService == null) {
            return;
        }

        switch (view.getId()) {

            case R.id.button_previous:
                if (mPosition > 0) {
                    --mPosition;
                } else {
                    mPosition = mTracks.size() - 1;
                }
                mIsInit = false;
                mIsPaused = true;
                restartPlayer();
                updateViewsWithCurrentTrack();
                mTrackChangeCallback.onPlayerTrackChange(mPosition);
//                Log.v(LOG_TAG, "previous clicked - playing previous track "
//                        + mTracks.get(mPosition).getName());

                break;

            case R.id.button_play:

//                Log.v(LOG_TAG, "buttonPlay - isBound=" + mIsBound
//                        + ", isPlaying=" + mPlayerService.isPlaying());
                if (!mIsInit) {
//                    Log.v(LOG_TAG, "buttonPlay - starts new track "
//                            + mTracks.get(mPosition).getName());
                    restartPlayer();
                } else {
                    if (!mIsPaused) {
//                        Log.v(LOG_TAG, "buttonPlay - pauses");
                        mIsPaused = true;
                        mPlayerService.pause();
                    } else {
//                        Log.v(LOG_TAG, "buttonPlay - resumes");
                        mIsPaused = false;
                        mPlayerService.resume();
                    }
                }

                break;
            case R.id.button_next:
                if (mPosition < mTracks.size() - 1) {
                    ++mPosition;
                } else {
                    mPosition = 0;
                }
                mIsInit = false;
                mIsPaused = true;
//                Log.v(LOG_TAG, "next clicked - playing next track "
//                        + mTracks.get(mPosition).getName());
                restartPlayer();
                updateViewsWithCurrentTrack();
                mTrackChangeCallback.onPlayerTrackChange(mPosition);

                break;
        }

        // reset play/pause buttons
        updatePlayButton();
    }



    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        // will be called on every change (really often)
        if (mPlayerService != null && fromUser){
//            Log.v(LOG_TAG, "onStopTrackingTouch " + seekBar.getProgress());
            mPlayerService.seekToPositon(seekBar.getProgress());
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {    }
}
