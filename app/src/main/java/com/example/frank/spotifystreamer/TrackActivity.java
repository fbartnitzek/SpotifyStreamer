package com.example.frank.spotifystreamer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;


public class TrackActivity extends ActionBarActivity implements TrackFragment.TrackCallback,
        PlayerFragment.PlayerTrackListener{

    public static final String INTENT_ARTIST_KEY = "INTENT_ARTIST_KEY";
    private static final String LOG_TAG = TrackActivity.class.getName();

    @Override
    public void onTrackSelected(ArrayList<TrackParcelable> trackParcelables, int position) {
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putParcelableArrayListExtra(Constants.EXTRA_TRACKS, trackParcelables);
        intent.putExtra(Constants.EXTRA_TRACK_NUMBER, position);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);   // TODO: ???
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

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
            Log.v(LOG_TAG, "onCreate - saved state");
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_track, menu);
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
            // TODO: refresh top10 tracks after country-change

            return true;
        }

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
