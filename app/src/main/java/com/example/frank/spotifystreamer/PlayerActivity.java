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
public class PlayerActivity extends ActionBarActivity{
    // TODO: extends AppCompatActivity???

    private static final String LOG_TAG = PlayerActivity.class.getName();

    private DialogFragment mDialogFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        Log.v(LOG_TAG, "onCreate - activity started");

        FragmentManager fm = getSupportFragmentManager();
        mDialogFragment = (DialogFragment) fm.findFragmentByTag(Constants.TAG_PLAYER_FRAGMENT);

        if (mDialogFragment == null) {

            // parsing intent
            Bundle args = new Bundle();
            args.putParcelableArrayList(Constants.ARGS_TRACKS,
                    getIntent().getParcelableArrayListExtra(Constants.EXTRA_TRACKS));
            args.putInt(Constants.ARGS_TRACK_NUMBER,
                    getIntent().getIntExtra(Constants.EXTRA_TRACK_NUMBER, 0));
            Log.v(LOG_TAG, "onCreate - parsed intent");

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

}
