package com.example.frank.spotifystreamer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;


public class MainActivity extends ActionBarActivity
        implements ArtistFragment.Callback, TrackFragment.TrackCallback{

    private static final String DETAILFRAGMENT_TAG = "DFTAG";
    private final String LOG_TAG = MainActivity.class.getName();

    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (findViewById(R.id.track_detail_container) != null){
            // the detail container will only be present for large screens (layout-sw600dp)
            mTwoPane = true;

            if (savedInstanceState == null) {
                Log.v(LOG_TAG, "onCreate - new fragment");
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.track_detail_container, new TrackFragment(),
                                DETAILFRAGMENT_TAG)
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
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // change TrackFragment f.e. other country selected
    }

    @Override
    public void onItemSelected(ArtistParcelable artistParcelable) {
        if (mTwoPane){
            Log.v(LOG_TAG, "onItemSelected - tablet");
            Bundle args = new Bundle();
            args.putParcelable(Constants.ARGS_ARTIST_PARCELABLE, artistParcelable);

            Fragment fragment = new TrackFragment();
            fragment.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.track_detail_container, fragment, DETAILFRAGMENT_TAG)
                    .commit();

        } else {
            Log.v(LOG_TAG, "onItemSelected - mobile");
            // use parcelableArtist directly
            Intent trackIntent = new Intent(this, TrackActivity.class)
                    .putExtra(TrackActivity.INTENT_ARTIST_KEY, artistParcelable);
            startActivity(trackIntent);
        }
    }

    @Override
    public void onTrackSelected(ArrayList<TrackParcelable> tracks, int position) {
        // TODO: how to use callback for selected item
        Bundle args = new Bundle();
        args.putParcelableArrayList(Constants.ARGS_TRACKS, tracks);
        args.putInt(Constants.ARGS_TRACK_NUMBER, position);

        PlayerFragment newFragment = new PlayerFragment();
        newFragment.setArguments(args);

        FragmentManager fm = getSupportFragmentManager();
        newFragment.show(fm, Constants.TAG_PLAYER_FRAGMENT);
    }
}
