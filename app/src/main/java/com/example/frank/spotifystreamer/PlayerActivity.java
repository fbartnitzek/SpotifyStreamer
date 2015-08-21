package com.example.frank.spotifystreamer;


import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;

/**
 * Created by frank on 05.08.15.
 */
public class PlayerActivity extends ActionBarActivity implements
        PlayerFragment.PlayerTrackListener {
    // TODO: extends AppCompatActivity???

    private static final String LOG_TAG = PlayerActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        Log.v(LOG_TAG, "onCreate - activity started");

        FragmentManager fm = getSupportFragmentManager();
        DialogFragment mDialogFragment = (DialogFragment) fm.findFragmentByTag(Constants.TAG_PLAYER_FRAGMENT);

        if (mDialogFragment == null) {

            // parsing intent
            Bundle args = new Bundle();
            if (!getIntent().hasExtra(Constants.EXTRA_RESTART_PLAYING)
                || getIntent().getBooleanExtra(Constants.EXTRA_RESTART_PLAYING, true)){
                args.putParcelableArrayList(Constants.ARGS_TRACKS,
                        getIntent().getParcelableArrayListExtra(Constants.EXTRA_TRACKS));
                args.putInt(Constants.ARGS_TRACK_NUMBER,
                        getIntent().getIntExtra(Constants.EXTRA_TRACK_NUMBER, 0));
                Log.v(LOG_TAG, "onCreate - parsed initial intent");
            } else {
                args.putBoolean(Constants.ARGS_RESTART_PLAYING, getIntent()
                        .getBooleanExtra(Constants.EXTRA_RESTART_PLAYING, false));
                args.putParcelableArrayList(Constants.ARGS_TRACKS,
                        getIntent().getParcelableArrayListExtra(Constants.EXTRA_TRACKS));
                args.putInt(Constants.ARGS_TRACK_NUMBER,
                        getIntent().getIntExtra(Constants.EXTRA_TRACK_NUMBER, 0));
                Log.v(LOG_TAG, "onCreate - parsed resume intent");
            }


            // add new fragment
            PlayerFragment newFragment = new PlayerFragment();
            newFragment.setArguments(args);
            FragmentTransaction transaction = fm.beginTransaction();
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            transaction.add(R.id.player_container, newFragment, Constants.TAG_PLAYER_FRAGMENT)
                    .commit();
            Log.v(LOG_TAG, "onCreate - added new fragment");

        }

    }

    @Override
    public void onPlayerTrackChange(int position) {

        Log.v(LOG_TAG, "onPlayerTrackChange, position: " + position);
//        Activity parent = getParent();
        //TODO: how to modify fragment from parent activity...?
//        TrackFragment tf = (TrackFragment) getSupportFragmentManager()
//                .findFragmentById(R.id.track_detail_container);
//
//        if (tf != null) {
//            Log.v(LOG_TAG, "onPlayerTrackChange - fragment found, position: " + position);
//            tf.updateSelectedTrack(position);
//        } else {
//            Log.v(LOG_TAG, "onPlayerTrackChange - fragment unknown, position: " + position);
//        }
    }
}
