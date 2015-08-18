package com.example.frank.spotifystreamer;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by frank on 05.08.15.
 */
public class PlayerFragment extends DialogFragment implements View.OnClickListener,
        ServiceConnection, SeekBar.OnSeekBarChangeListener {

    private static final String LOG_TAG = PlayerFragment.class.getName();
    private static PlayerFragment mFragment = null;
    private TrackParcelable[] mTracks;
    private View mRootView;
    private int mPosition;
    private TrackParcelable mTrack;

    private Context mContext;
    private PlayerService mPlayerService;
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
    private boolean mIsBound = false;
    private boolean mIsPaused = false;
    private boolean mIsInit = false;


//    private boolean mStateIsPlaying = false;

    private int mPositionInTrack = 0;
    private int mDuration = Constants.TRACK_DEFAULT_LENGTH;   // workaround for unknown duration and division
//    private boolean mIsInit = false;

//    private IntentFilter receiverFilter;
    private LocalBroadcastManager mBroadcastManager;


    // seems to be wrong ...
//    // using a music-player-service:
//    // http://code.tutsplus.com/tutorials/create-a-music-player-on-android-song-playback--mobile-22778
//    private ServiceConnection mPlayerConnection = new ServiceConnection() {
//        @Override
//        public void onServiceConnected(ComponentName name, IBinder service) {
//            PlayerService.PlayerBinder binder = (PlayerService.PlayerBinder) service;
//            Log.v(LOG_TAG, "onServiceConnected - binder created");
//            // get service
//            mPlayerService = binder.getService();
//            Log.v(LOG_TAG, "onServiceConnected - service created");
//
////            // pass list
////            mPlayerService.setmTracks(mTracks);
////            mPlayerService.setPosition(mPosition);
//
//            Log.v(LOG_TAG, "onServiceConnected - setter through");
//            mIsBound = true;
//        }
//
//        @Override
//        public void onServiceDisconnected(ComponentName name) {
//            mIsBound = false;
//        }
//    };


//    @Override
//    public void onStop() {
//        super.onStop();
//        if (mBound){
//            getActivity().unbindService(conn);
//        }
//    }


    // singleton wont help
    public static PlayerFragment getInstance(ArrayList<TrackParcelable> trackList, int postion){

        TrackParcelable[] tracks = trackList.toArray(new TrackParcelable[trackList.size()]);

        Bundle args = new Bundle();
        args.putParcelableArray(Constants.ARGS_TRACKS, tracks);
        args.putInt(Constants.ARGS_TRACK_NUMBER, postion);

        if (mFragment == null){
            mFragment = new PlayerFragment();
            Log.v(LOG_TAG, "getInstance - create new Fragment for track " + tracks[postion].getName());
        } else {
            Log.v(LOG_TAG, "getInstance - used singelton-Fragment for track " + tracks[postion].getName());
        }
//        PlayerFragment mFragment;


        mFragment.setArguments(args);

        return mFragment;
    }


    // use broadcastReceiver for seekBar
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (Constants.ACTION_TRACK_STATE.equals(intent.getAction())) {
                mTrack = intent.getParcelableExtra(Constants.EXTRA_CURRENT_TRACK);
//
                if (intent.hasExtra(Constants.EXTRA_TRACK_NUMBER)){
                    int number = intent.getIntExtra(Constants.EXTRA_TRACK_NUMBER, -1);

                    if (number >=0 && number != mPosition) {
//                        sendChangedTrack(number);

                        mPosition = number;
                    }
                }

                if (intent.hasExtra(Constants.EXTRA_IS_PLAYING)){
                    // set progress
                    mPositionInTrack = intent.getIntExtra(Constants.EXTRA_CURRENT_TRACK_POSITION, 0) +
                            Constants.UPDATE_INTERVAL;
                    Log.v(LOG_TAG, "in updateProgressViews, progress: " + mPositionInTrack);
                    mSeekBar.setProgress(mPositionInTrack);
                    mElapsedView.setText(Util.formatTime(mPositionInTrack));
                }

                // maybe needed for on finish
                if (mPlayerService.isPlaying()){
                    mPlayButton.setImageResource(android.R.drawable.ic_media_pause);
                } else {
                    //use pause button
                    mPlayButton.setImageResource(android.R.drawable.ic_media_play);
                }
            }
        }
    };



//    private void sendChangedTrack(int number) {
//        Log.v(LOG_TAG, "onReceive, track changed: " + number
//                + " (previously: " + mPosition + ")");
//        Intent tcIntent = new Intent(Constants.TRACK_CHANGED); // action, not tag...
//        tcIntent.putExtra(Constants.SELECTED_TRACK, number);
//        mBroadcastManager.sendBroadcast(tcIntent);
//    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // init service later

        // maybe better?
//        mContext = getActivity().getApplicationContext();
        mContext = getActivity();
        setRetainInstance(true);
        Log.v(LOG_TAG, "onCreate - now retain instance");

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(Constants.STATE_TRACKS)) {
                mTracks = (TrackParcelable[]) savedInstanceState.getParcelableArray(Constants.STATE_TRACKS);
                mPosition = savedInstanceState.getInt(Constants.STATE_SELECTED_TRACK);
                mPositionInTrack = savedInstanceState.getInt(Constants.STATE_CURRENT_TRACK_POSITION);
                mDuration = savedInstanceState.getInt(Constants.STATE_DURATION,
                        Constants.TRACK_DEFAULT_LENGTH);
                mTrack = mTracks[mPosition];
//                mStateIsPlaying = savedInstanceState.getBoolean(Constants.STATE_IS_PLAYING);
                mIsInit = savedInstanceState.getBoolean(Constants.STATE_IS_INIT);
                mIsBound = savedInstanceState.getBoolean(Constants.STATE_IS_BOUND);
                mIsPaused = savedInstanceState.getBoolean(Constants.STATE_IS_PAUSED);

                Log.v(LOG_TAG, "state restored with position: " + mPositionInTrack
                        + ", isBound: " + mIsBound + ", isInit: " + mIsInit
                        + ", isPaused: " + mIsPaused);
            }
        } else {
            Bundle args = getArguments();
            if (args != null && args.containsKey(Constants.ARGS_TRACKS)){
                Parcelable[] objects = args.getParcelableArray(Constants.ARGS_TRACKS);
                mTracks = Arrays.asList(objects).toArray(new TrackParcelable[objects.length]);
                mPosition = args.getInt(Constants.ARGS_TRACK_NUMBER);
                mTrack = mTracks[mPosition];
                Log.v(LOG_TAG, " in onCreate with track " + mTrack.getName());
            } else {
                Log.v(LOG_TAG, " no args set - should never happen");
            }
        }

//        if (mPlayIntent == null){
//        if (!mIsBound){
//            startPlayerService();
//        }

        mBroadcastManager = LocalBroadcastManager.getInstance(mContext);
    }

//    private void startPlayerService() {
////        mPlayIntent = new Intent(mContext, PlayerService.class);
////        mPlayIntent.putExtra(PlayerService.TRACK_NUMBER, mPosition);
////        mContext.startService(mPlayIntent);
////        mContext.bindService(mPlayIntent, this, Context.BIND_AUTO_CREATE);
//        Intent intent = new Intent(mContext, PlayerService.class);
//        mContext.startService(intent);
//        mContext.bindService(intent, this, Context.BIND_AUTO_CREATE);
//
//        Log.v(LOG_TAG, " in OnCreate - service should be started and binded anytime soon");
//    }

    @Override
    public void onResume() {
        Log.v(LOG_TAG, "onResume ");
        // to onStart
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
        mRootView = inflater.inflate(R.layout.fragment_player, container, false);

        mArtistView = (TextView) mRootView.findViewById(R.id.artist_name);
        mAlbumNameView = (TextView) mRootView.findViewById(R.id.album_name);
        mAlbumImageView = (ImageView) mRootView.findViewById(R.id.album_image);
        mSeekBar = (SeekBar) mRootView.findViewById(R.id.seek_bar);
        mSeekBar.setOnSeekBarChangeListener(this);
        mElapsedView = (TextView) mRootView.findViewById(R.id.elapsed_time);
        mDurationView = (TextView) mRootView.findViewById(R.id.duration);
        mTrackView = (TextView) mRootView.findViewById(R.id.track_name);
        mPreviousButton = (ImageButton) mRootView.findViewById(R.id.button_previous);
        mNextButton = (ImageButton) mRootView.findViewById(R.id.button_next);
        mPlayButton = (ImageButton) mRootView.findViewById(R.id.button_play);

        if (!mIsInit) {
            Log.v(LOG_TAG, "might be TODO: init tracks and position");
        }

        if (mTrack != null) {
            // TODO: needs knowledge of track-infos (position/duration) ... available?
            updateViewsWithCurrentTrack();
        }

        return mRootView;
    }


    private void updateViewsWithCurrentTrack() {
        Log.v(LOG_TAG, "in updateViewsWithCurrentTrack ");

//        mPositionInTrack = 0;
        // artist name
        mArtistView.setText(mTrack.getArtist());

        // album name
        mAlbumNameView.setText(mTrack.getAlbum());


        // album image
        String url = mTrack.getPictureUrl();
        if (url != null && !url.isEmpty()){
            Picasso.with(getActivity()).load(url).into(mAlbumImageView);
        }

        Log.v(LOG_TAG, "preview_url: " + mTrack.getPreviewUrl());

        // track name
        mTrackView.setText(mTrack.getName());

        // progress bar
        mSeekBar.setProgress(mPositionInTrack);
        mSeekBar.setMax(mDuration);

        mDurationView.setText(Util.formatTime(mDuration));
        mElapsedView.setText(Util.formatTime(mPositionInTrack));

        // buttons
        mPreviousButton.setOnClickListener(this);

        mNextButton.setOnClickListener(this);

        mPlayButton.setOnClickListener(this);
    }


    /** The system calls this only when creating the layout in a dialog. */
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

        Log.v(LOG_TAG, "onAttach - startService and bindService");
        Intent intent = new Intent(activity, PlayerService.class);
        activity.startService(intent);
        activity.bindService(intent, this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStart() {
        super.onStart();

        Log.v(LOG_TAG, "onStart");

//        if (!mIsBound){
//        Intent intent = new Intent(mContext, PlayerService.class);
//        mContext.startService(intent);
//        mContext.bindService(intent, this, Context.BIND_AUTO_CREATE);

//        }

//        if (!mIsBound) {
//            mPlayIntent = new Intent(mContext, PlayerService.class);
//            getActivity().bindService(mPlayIntent, musicConnection, Context.BIND_AUTO_CREATE);
//            getActivity().startService(mPlayIntent);
//        }

    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.v(LOG_TAG, "onDetach - unregister and unbindService");
//        mContext.unbindService(this);
        mIsBound = false;
//        mBroadcastManager.unregisterReceiver(mReceiver);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.v(LOG_TAG, "onStop - unbind and unregister");
//        mContext.unbindService(this);
//        mIsBound = false;
//        mBroadcastManager.unregisterReceiver(mReceiver);
    }

    @Override
    public void onDestroyView() {
        Log.v(LOG_TAG, "onDestroyView");

        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        // onCreate: mContext.startService(mPlayIntent);
        //        mContext.bindService(mPlayIntent, this, Context.BIND_AUTO_CREATE);
        //http://code.tutsplus.com/tutorials/create-a-music-player-on-android-song-playback--mobile-22778
        Log.v(LOG_TAG, "onDestroy");

        // at onStop
//        mContext.unbindService(this);
//        mIsBound = false;


//        if (mPlayerService != null) {
//            getActivity().stopService(mPlayIntent);
//            mPlayerService = null;
//            mContext.unbindService(this);
//        }

        super.onDestroy();
    }

    private void startNewTrack() {

        Log.v(LOG_TAG, "startNewTrack " + mPosition);
        mPlayerService.startTrack(mPosition);
        mPositionInTrack = 0;
        mIsInit = true;
        mIsPaused = false;

        updateViewsWithCurrentTrack();
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

//        Log.v(LOG_TAG, "onSaveInstanceState isPlaying: " + mPlayerService.isPlaying());
        Log.v(LOG_TAG, "onSaveInstanceState with position: " + mPositionInTrack
                + ", isBound: " + mIsBound + ", isInit: " + mIsInit
                + ", isPaused: " + mIsPaused);
        if (mTracks != null) {
            outState.putParcelableArray(Constants.STATE_TRACKS, mTracks);
            outState.putInt(Constants.STATE_SELECTED_TRACK, mPosition);
            outState.putInt(Constants.STATE_DURATION, mDuration);
            outState.putInt(Constants.STATE_CURRENT_TRACK_POSITION, mPositionInTrack);
//            outState.putBoolean(Constants.STATE_IS_PLAYING, mPlayerService.isPlaying());
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
                    mPosition = mTracks.length - 1;
                }

                mIsInit = false;
                mIsPaused = true;
                mTrack = mTracks[mPosition];
                Log.v(LOG_TAG, "previous clicked - playing previous track " + mTrack.getName());

                startNewTrack();


                // TODO: show the changed track in other fragment ...

                break;

            case R.id.button_play:

                if (mPlayerService != null) {
                    Log.v(LOG_TAG, "buttonPlay - isBound=" + mIsBound
                            + ", isPlaying=" + mPlayerService.isPlaying());
                    if (!mIsInit) {
                        Log.v(LOG_TAG, "buttonPlay - starts new track " + mTracks[mPosition]);
                        startNewTrack();
                    } else {
                        if (!mIsPaused) {
                            Log.v(LOG_TAG, "buttonPlay - pauses");
//                            mPlayButton.setImageResource(android.R.drawable.ic_media_play);
                            mIsPaused = true;
                            mPlayerService.pause();
                        } else {
                            Log.v(LOG_TAG, "buttonPlay - resumes");
//                            mPlayButton.setImageResource(android.R.drawable.ic_media_pause);
                            mIsPaused = false;
                            mPlayerService.resume();
                        }
                    }

                }

                break;
            case R.id.button_next:
                if (mPosition < mTracks.length - 1) {
                    ++mPosition;
                } else {
                    mPosition = 0;
                }

                mTrack = mTracks[mPosition];
                mIsInit = false;
                mIsPaused = true;
                Log.v(LOG_TAG, "next clicked - playing next track " + mTrack.getName());

                startNewTrack();


                break;
        }

        // reset play/pause buttons
        if (mIsPaused){
            mPlayButton.setImageResource(android.R.drawable.ic_media_play);
        } else {
            mPlayButton.setImageResource(android.R.drawable.ic_media_pause);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.v(LOG_TAG, "onServiceConnected with position: " + mPositionInTrack
                + ", isBound: " + mIsBound + ", isInit: " + mIsInit
                + ", isPaused: " + mIsPaused);

//        Log.v(LOG_TAG, "onServiceConnected, isPlaying: "
//                + mStateIsPlaying + ", position: " + mPositionInTrack);
        // use binder to get service
        mPlayerService = ((PlayerService.PlayerBinder) service).getService();
        mIsBound = true;

//        mPlayerService.setmTracks(mTracks);
//        mPlayerService.startTrack(mPosition);
//        if (mPositionInTrack>0){
//            mPlayerService.seekToPositon(mPositionInTrack);
//        }
//        if (mStateIsPlaying){
//            mPlayerService.resume();
//            mStateIsPlaying = false;
//        }


        // lets try again?

        // TODO: seems useless
        if (!mIsInit) {
            Log.v(LOG_TAG, "onServiceConnected - initializing player");
            mPlayerService.setmTracks(mTracks);
            mPlayerService.startTrack(mPosition);
            mIsInit = true;
            mIsPaused = false;
        }

        // register receiver
        mBroadcastManager.registerReceiver(mReceiver,
                new IntentFilter(Constants.ACTION_TRACK_STATE));
        Log.v(LOG_TAG, "onServiceConnected - registered receivers");
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.v(LOG_TAG, "onServiceDisconnected");
        mIsBound = false;
//        mPlayerService = null;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        // will be called on every change (really often)
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

        Log.v(LOG_TAG, "onStopTrackingTouch " + seekBar.getProgress());
        mPlayerService.seekToPositon(seekBar.getProgress());
    }
}
