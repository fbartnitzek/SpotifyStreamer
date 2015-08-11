package com.example.frank.spotifystreamer;

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

    private static final String LOG_TAG = PlayerFragment.class.getSimpleName();
    public static final String TRACK_CHANGED = "TRACK_CHANGED";
    //    private static PlayerFragment mFragment;
    private TrackParcelable[] mTracks;
    private View mRootView;
    private int mPosition;
    private TrackParcelable mTrack;
    public static final String TRACKS = "TRACKS_EXTRA";
    public static final String CURRENT_TRACK = "CURRENT_TRACK_EXTRA";
    private Context mContext;
    private PlayerService mPlayerService;
    private TextView mArtistView;
    private TextView mAlbumNameView;
    private ImageView mAlbumImageView;
    private TextView mTrackView;
    private ImageButton mPreviousButton;
    private ImageButton mNextButton;
    private ImageButton mPlayButton;
    private SeekBar mSeekBar;
    private boolean mIsBound = false;
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

    private double mPositionInTrack = 0;
    private double mDuration = 1;   // workaround for unknown duration and division
    // use broadcastReceiver for seekBar
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (PlayerService.PLAYER_TRACK_STATE.equals(intent.getAction())) {
                mTrack = intent.getParcelableExtra(PlayerService.CURRENT_TRACK);

                if (intent.hasExtra(PlayerService.TRACK_NUMBER)){
                    int number = intent.getIntExtra(PlayerService.TRACK_NUMBER, -1);

                    if (number >=0 && number != mPosition) {
                        sendChangedTrack(number);

                        mPosition = number;
                    }
                }

                // maybe needed for on finish
                if (mPlayerService.isPlaying()){
                    mPlayButton.setImageResource(android.R.drawable.ic_media_pause);
                } else {
                    //use pause button
                    mPlayButton.setImageResource(android.R.drawable.ic_media_play);
                }

                mDuration = intent.getIntExtra(PlayerService.TRACK_DURATION, 1);
                mPositionInTrack = intent.getIntExtra(PlayerService.TRACK_CURRENT_POSITION, 0);

                // set progress in %
                if (intent.getBooleanExtra(PlayerService.NEW_TRACK, true)) {
                    // new track

                    updateViewsWithCurrentTrack();
                } else {
                    // known track
                    mSeekBar.setProgress((int) (100 * mPositionInTrack / mDuration));
//                    mSeekBar.setProgress((int)mPositionInTrack);
                }

            }

        }
    };

    private void sendChangedTrack(int number) {
        Log.v(LOG_TAG, "onReceive, track changed: " + number
                + " (previously: " + mPosition + ")");
        Intent tcIntent = new Intent(TRACK_CHANGED); // action, not tag...
        tcIntent.putExtra(TrackFragment.SELECTED_TRACK, number);
        mBroadcastManager.sendBroadcast(tcIntent);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // init service later

        mContext = getActivity().getApplicationContext();

        Bundle args = getArguments();
        if (args != null && args.containsKey(TRACKS)){

            Parcelable[] objects = args.getParcelableArray(TRACKS);
            mTracks = Arrays.asList(objects).toArray(new TrackParcelable[objects.length]);
            mPosition = args.getInt(PlayerService.TRACK_NUMBER);
            mTrack = mTracks[mPosition];
            Log.v(LOG_TAG, " in onCreate with track " + mTrack.getName());
        } else {
            Log.v(LOG_TAG, " no args set - should never happen");
        }

//        if (mPlayIntent == null){
        if (!mIsBound){
            startPlayerService();

        }

        mBroadcastManager = LocalBroadcastManager.getInstance(mContext);
    }

    private void startPlayerService() {
//        mPlayIntent = new Intent(mContext, PlayerService.class);
//        mPlayIntent.putExtra(PlayerService.TRACK_NUMBER, mPosition);
//        mContext.startService(mPlayIntent);
//        mContext.bindService(mPlayIntent, this, Context.BIND_AUTO_CREATE);
        Intent intent = new Intent(mContext, PlayerService.class);
        mContext.startService(intent);
        mContext.bindService(intent, this, Context.BIND_AUTO_CREATE);

        Log.v(LOG_TAG, " in OnCreate - service should be started and binded anytime soon");
    }

    @Override
    public void onResume() {
        Log.v(LOG_TAG, "on resume - registers receiver");
        mBroadcastManager.registerReceiver(mReceiver,
                new IntentFilter(PlayerService.PLAYER_TRACK_STATE));
        super.onResume();
    }

    @Override
    public void onPause() {
        Log.v(LOG_TAG, "on pause - unregisters receiver");
        mBroadcastManager.unregisterReceiver(mReceiver);
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
        mTrackView = (TextView) mRootView.findViewById(R.id.track_name);
        mPreviousButton = (ImageButton) mRootView.findViewById(R.id.button_previous);
        mNextButton = (ImageButton) mRootView.findViewById(R.id.button_next);
        mPlayButton = (ImageButton) mRootView.findViewById(R.id.button_play);

        if (mTrack != null) {
            // TODO: needs knowledge of track-infos (position/duration) ... available?
            updateViewsWithCurrentTrack();
        }

        return mRootView;
    }

    private void updateViewsWithCurrentTrack() {
        Log.v(LOG_TAG, "in updateViewsWithCurrentTrack - updates are coming...");

        mPositionInTrack = 0;
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
        mSeekBar.setProgress((int) (100 * mPositionInTrack / mDuration));
        mSeekBar.setMax(100);
//        mSeekBar.setProgress((int) mPositionInTrack);

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
    public void onStart() {
        super.onStart();

        Log.v(LOG_TAG, "onStart");

        if (!mIsBound){
            Intent intent = new Intent(mContext, PlayerService.class);
            mContext.bindService(intent, this, Context.BIND_AUTO_CREATE);

        }

//        if (!mIsBound) {
//            mPlayIntent = new Intent(mContext, PlayerService.class);
//            getActivity().bindService(mPlayIntent, musicConnection, Context.BIND_AUTO_CREATE);
//            getActivity().startService(mPlayIntent);
//        }

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
        Log.v(LOG_TAG, "onDestroyView");

        mContext.unbindService(this);
        mIsBound = false;


//        if (mPlayerService != null) {
//            getActivity().stopService(mPlayIntent);
//            mPlayerService = null;
//            mContext.unbindService(this);
//        }

        super.onDestroy();
    }

    private void startNewTrack(int number) {
        mPosition = number;
        Log.v(LOG_TAG, "startNewTrack " + number);
        mPlayerService.startTrack(mPosition);
        mPositionInTrack = 0;
        updateViewsWithCurrentTrack();
    }

//    public void update(TrackParcelable[] tracks, int position){
//
//        mPosition = position;
//
//        if (tracks != null){
//            mTracks = tracks;
//            TrackParcelable track = tracks[position];
//
//            // start track anyway
////            if (mTrack != null && track != null
////                    && mTrack.getPreviewUrl() != null && track.getPreviewUrl() != null
////                    && mTrack.getPreviewUrl().equals(track.getPictureUrl())){
////
////                // same title - use previous position, if not at the end
////                Log.v(LOG_TAG, "update - same title");
////                if (mPositionInTrack == mDuration){
////                    mPositionInTrack = 0;
////                }
////            } else {
////                Log.v(LOG_TAG, "update - other title");
////                mTrack = track;
////                mPositionInTrack = 0;
////            }
//            mTrack = track;
//
//            if (mPlayerService != null){
////                mPlayerService.
////                mPlayIntent = new Intent(mContext, PlayerService.class);
////                mPlayIntent.putExtra(PlayerService.TRACK_NUMBER, mPosition);
//                // use old intent to bind
//
//                Log.v(LOG_TAG, "update - playerService exists, try binding");
//                mContext.bindService(mPlayIntent, this, Context.BIND_AUTO_CREATE);
//            } else {
//                Log.v(LOG_TAG, "update - playerService is null - has to start again...");
//                startPlayerService();
//            }
//
////            if (mPlayerService != null){
////                mPlayerService.setmTracks(mTracks);
////                mPlayerService.startTrack(mPosition);
////                Log.v(LOG_TAG, "update - playerService exists");
////            } else {
////                mPlayIntent
////                mContext.bindService(mPlayIntent, this, Context.BIND_AUTO_CREATE);
////                Log.v(LOG_TAG, "update - playerService is null");
////            }
//        }
//    }

    // singleton wont help
    public static PlayerFragment getInstance(ArrayList<TrackParcelable> trackList, int postion){

        TrackParcelable[] tracks = trackList.toArray(new TrackParcelable[trackList.size()]);

        Bundle args = new Bundle();
        args.putParcelableArray(TRACKS, tracks);
        args.putInt(PlayerService.TRACK_NUMBER, postion);

        PlayerFragment mFragment;
        Log.v(LOG_TAG, "getInstance - create new Fragment for track " + tracks[postion].getName());
        mFragment = new PlayerFragment();
        mFragment.setArguments(args);

        return mFragment;
    }

    @Override
    public void onClick(View view) {

        switch (view.getId()){

            case R.id.button_previous:
                if (mPosition>0){
                    --mPosition;
                } else {
                    mPosition = mTracks.length-1;
                }

                mTrack = mTracks[mPosition];
                Log.v(LOG_TAG, "previous clicked - playing previous track " + mTrack.getName());

                if (mPlayerService != null){

                    startNewTrack(mPosition);
                }

                // TODO: show the changed track in other fragment ...

                break;

            case R.id.button_play:

                if (mPlayerService != null){
                    Log.v(LOG_TAG, "buttonPlay - isBound=" + mIsBound
                            + ", isPlaying=" + mPlayerService.isPlaying());
                    if (!mIsBound){
                        Log.v(LOG_TAG, "buttonPlay - starts new track " + mTracks[mPosition]);
                        startNewTrack(mPosition);
                    } else {
                        if (mPlayerService.isPlaying()){
                            Log.v(LOG_TAG, "buttonPlay - pauses");
                            mPlayButton.setImageResource(android.R.drawable.ic_media_play);
                            mPlayerService.pause();
                        } else {
                            Log.v(LOG_TAG, "buttonPlay - resumes");
                            mPlayButton.setImageResource(android.R.drawable.ic_media_pause);
                            mPlayerService.resume();
                        }
                    }

                }


                break;
            case R.id.button_next:
                if (mPosition<mTracks.length-1){
                    ++mPosition;
                } else {
                    mPosition = 0;
                }

                mTrack = mTracks[mPosition];
                Log.v(LOG_TAG, "next clicked - playing next track " + mTrack.getName());
                if (mPlayerService != null){
                    startNewTrack(mPosition);
                }

                break;
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.v(LOG_TAG, "onServiceConnected");
        // use binder to get service
        mPlayerService = ((PlayerService.PlayerBinder) service).getService();
        mIsBound = true;
//        mIsInit = true;

        mPlayerService.setmTracks(mTracks);
        mPlayerService.startTrack(mPosition);
//        // later?
//        if (!mIsInit){
//            mPlayerService.setmTracks(mTracks);
//            mPlayerService.startTrack(mPosition);
//            mPlayerService.initPlayer();
//            mIsInit = true;
//        }
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
        //TODO: real integers ...
        Log.v(LOG_TAG, "onStopTrackingTouch " + seekBar.getProgress());
//        mSeekBar.setProgress((int) (100 * mPositionInTrack / mDuration));
        mPlayerService.seekToPositon((int) (seekBar.getProgress() * mDuration / 100 ));
    }
}
