package com.example.frank.spotifystreamer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.ArrayList;


public class TrackActivity extends AppCompatActivity implements TrackFragment.TrackCallback,
        PlayerFragment.PlayerTrackListener{

    private static final String LOG_TAG = TrackActivity.class.getName();
    private ArrayList<TrackParcelable> mTrackParcelables;
    private int mTrackNumber;
    private boolean mPlayerStarted = false;

    @Override
    public void onTrackSelected(ArrayList<TrackParcelable> trackParcelables, int position) {

        mTrackParcelables = trackParcelables;
        mTrackNumber = position;

        startPlayer(true);
    }

    private void startPlayer(boolean restartPlaying) {
        Log.v(LOG_TAG, "start player");
        Intent intent = new Intent(this, PlayerActivity.class);

        if (restartPlaying){
            intent.putParcelableArrayListExtra(Constants.EXTRA_TRACKS, mTrackParcelables);
            intent.putExtra(Constants.EXTRA_TRACK_NUMBER, mTrackNumber);
            mPlayerStarted = true;
        } else if (!mPlayerStarted){
            Toast.makeText(this, R.string.now_playing_unavailable, Toast.LENGTH_LONG).show();
            return;
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);   // singleton-like
        intent.putExtra(Constants.EXTRA_RESTART_PLAYING, restartPlaying);

        startActivity(intent);
        mPlayerStarted = true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track);

        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }


        // not needed here, but in main-activity...?
        Bundle args = new Bundle();

        if (!getIntent().hasExtra(Constants.EXTRA_ARTIST)) {
            Log.v(LOG_TAG, "onCreate without artist");
            return;
        }
        ArtistParcelable artist = getIntent().getParcelableExtra(Constants.EXTRA_ARTIST);
        args.putParcelable(Constants.ARGS_ARTIST_PARCELABLE, artist);
        TrackFragment fragment = new TrackFragment();
        fragment.setArguments(args);


        this.setTitle(getString(R.string.title_activity_track)
                + " (" + artist.getName() + ")");

        if (savedInstanceState == null) {

            Log.v(LOG_TAG, "onCreate - new fragment");
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.track_detail_container, fragment)
                    .commit();
        } else {
            Log.v(LOG_TAG, "onCreate - restored state, player started " + mPlayerStarted);
            mPlayerStarted = savedInstanceState.getBoolean(Constants.STATE_PLAYER_STARTED);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_track, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(Constants.STATE_PLAYER_STARTED, mPlayerStarted);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case R.id.action_now_playing:
                startPlayer(false);
                return true;
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
//        if (id == R.id.action_settings) {
//            startActivity(new Intent(this, SettingsActivity.class));
//            return true;
//        }

            return super.onOptionsItemSelected(item);
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
