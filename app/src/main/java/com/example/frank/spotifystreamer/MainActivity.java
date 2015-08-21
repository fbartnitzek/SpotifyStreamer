package com.example.frank.spotifystreamer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity
        implements ArtistFragment.Callback, TrackFragment.TrackCallback,
        PlayerFragment.PlayerTrackListener{

    private final String LOG_TAG = MainActivity.class.getName();

    private boolean mTwoPane;
    private String mCountry;
    private boolean mPlayerStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCountry = Util.getCountry(this);

        if (savedInstanceState != null) {
            mPlayerStarted = savedInstanceState.getBoolean(Constants.STATE_PLAYER_STARTED);
//            mArtists = savedInstanceState.getParcelableArray(Constants.STATE_ARTISTS);
            Log.v(LOG_TAG, "onCreate, restored tracks and mPlayerStarted " + mPlayerStarted);
        }

        if (findViewById(R.id.track_detail_container) != null){
            // the detail container will only be present for large screens (layout-sw600dp)
            mTwoPane = true;

            if (savedInstanceState == null) {
                Log.v(LOG_TAG, "onCreate - new fragment");
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.track_detail_container, new TrackFragment(),
                                Constants.TAG_DETAIL_FRAGMENT)
                        .commit();
            } else {
                Log.v(LOG_TAG, "onCreate - saved state ");
            }
        } else {
            mTwoPane =false;
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.action_now_playing:
                startPlayer(false, null, 0);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // change TrackFragment f.e. other country selected
        String country = Util.getCountry(this);
        if (!country.equals(mCountry)) {
            Log.v(LOG_TAG, "country changed");
//            ArtistFragment af = (ArtistFragment) getSupportFragmentManager()
//              .findFragmentById(R.id.fragment_artist);
            TrackFragment tf = (TrackFragment) getSupportFragmentManager()
                    .findFragmentByTag(Constants.TAG_DETAIL_FRAGMENT);
            if (tf != null) {
                tf.onCountryChanged();
            }
        }
    }

    @Override
    public void onItemSelected(ArtistParcelable artistParcelable) {
//        mArtists = artists;
        if (mTwoPane) {
            Log.v(LOG_TAG, "onItemSelected - tablet");
            Bundle args = new Bundle();
            args.putParcelable(Constants.ARGS_ARTIST_PARCELABLE, artistParcelable);

            Fragment fragment = new TrackFragment();
            fragment.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.track_detail_container, fragment, Constants.TAG_DETAIL_FRAGMENT)
                    .commit();

        } else {
            Log.v(LOG_TAG, "onItemSelected - mobile");
            // use parcelableArtist directly
            Intent intent = new Intent(this, TrackActivity.class)
                    .putExtra(Constants.EXTRA_ARTIST, artistParcelable);
            startActivity(intent);
        }
    }

    @Override
    public void onTrackSelected(ArrayList<TrackParcelable> tracks, int position) {
        Log.v(LOG_TAG, "onTrackSelected with position: " + position);
//        Bundle args = new Bundle();
//        mTracks = tracks;
//        mPosition = position;
        startPlayer(true, tracks, position);
//        args.putParcelableArrayList(Constants.ARGS_TRACKS, tracks);
//        args.putInt(Constants.ARGS_TRACK_NUMBER, position);
//
//        PlayerFragment newFragment = new PlayerFragment();
//        newFragment.setArguments(args);
//
//        FragmentManager fm = getSupportFragmentManager();
//        newFragment.show(fm, Constants.TAG_PLAYER_FRAGMENT);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(Constants.STATE_PLAYER_STARTED, mPlayerStarted);
//        outState.putParcelableArrayList(Constants.STATE_ARTISTS, mArtists);
        // TODO: why is mplayerStarted false, when service is running???
        Log.v(LOG_TAG, "onSaveInstanceState, stored tracks and playerStarted " + mPlayerStarted);
    }

    private void startPlayer(boolean restartPlaying, ArrayList<TrackParcelable> tracks,
                             int position) {
        Log.v(LOG_TAG, "start player, restartPlaying " + restartPlaying + ", position " + position);
        Bundle args = new Bundle();

        if (restartPlaying){
            args.putParcelableArrayList(Constants.ARGS_TRACKS, tracks);
            args.putInt(Constants.ARGS_TRACK_NUMBER, position);
            mPlayerStarted = true;

        } else if (!mPlayerStarted){

            Toast.makeText(this, R.string.now_playing_unavailable, Toast.LENGTH_LONG).show();
            return;

        }
        args.putBoolean(Constants.ARGS_RESTART_PLAYING, restartPlaying);
        PlayerFragment newFragment = new PlayerFragment();
        newFragment.setArguments(args);
        FragmentManager fm = getSupportFragmentManager();
        newFragment.show(fm, Constants.TAG_PLAYER_FRAGMENT);
    }

    @Override
    public void onPlayerTrackChange(int position) {
        TrackFragment tf = (TrackFragment) getSupportFragmentManager()
                .findFragmentById(R.id.track_detail_container);

        if (tf != null) {
            Log.v(LOG_TAG, "onPlayerTrackChange - fragment found, position: " + position);
            tf.updateSelectedTrack(position);
        } else {
            Log.v(LOG_TAG, "onPlayerTrackChange - fragment unknown, position: " + position);
        }
    }
}
