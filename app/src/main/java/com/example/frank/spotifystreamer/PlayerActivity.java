package com.example.frank.spotifystreamer;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by frank on 05.08.15.
 */
public class PlayerActivity extends ActionBarActivity{


    private static final String LOG_TAG = PlayerActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        Log.v(LOG_TAG, "activity started - parsing intent");

        android.support.v4.app.FragmentManager fm = getSupportFragmentManager();
        DialogFragment playerFragment = (DialogFragment) fm.findFragmentByTag(Constants.TAG_PLAYER);

        if (playerFragment == null) {

            ArrayList<Parcelable> objectList = getIntent().getParcelableArrayListExtra(
                    Constants.EXTRA_TRACKS);
            int position = getIntent().getIntExtra(Constants.EXTRA_CURRENT_TRACK, 0);
            ArrayList<TrackParcelable> trackList = new ArrayList<>();
            for (Parcelable object : objectList){
                trackList.add((TrackParcelable) object);
            }

            PlayerFragment newFragment = PlayerFragment.getInstance(trackList, position);
            FragmentTransaction transaction = fm.beginTransaction();
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            transaction.add(android.R.id.content, newFragment).commit();
        }

//        if (getIntent() != null && getIntent().hasExtra(Constants.EXTRA_TRACKS)) {
//            ArrayList<Parcelable> objectList = getIntent().getParcelableArrayListExtra(
//                    Constants.EXTRA_TRACKS);
//            int position = getIntent().getIntExtra(Constants.EXTRA_CURRENT_TRACK, 0);
//            ArrayList<TrackParcelable> trackList = new ArrayList<>();
//            for (Parcelable object : objectList){
//                trackList.add((TrackParcelable) object);
//            }
//
//            PlayerFragment playerFragment = PlayerFragment.getInstance(trackList, position);
//
////        // To make it fullscreen, use the 'content' root view as the container
////        // for the fragment, which is always the root view for the activity
//            if (savedInstanceState == null){
//                FragmentManager fragmentManager = getSupportFragmentManager();
//                FragmentTransaction transaction = fragmentManager.beginTransaction();
//                transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
//                transaction.add(android.R.id.content, playerFragment).commit();
//            }
//
//        } else {
//            Log.e(LOG_TAG, "no track found...");
//        }
    }

}
