package com.example.frank.spotifystreamer;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by frank on 05.08.15.
 */
public class PlayerActivity extends ActionBarActivity{


//    public static final String PLAYERFRAGMENT_TAG = "dialog";
    private static final String LOG_TAG = PlayerActivity.class.getSimpleName();
//    private TrackParcelable[] mTracks;
//    private int mPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_player);

        Log.v(LOG_TAG, "activity started - parsing intent");
        if (getIntent() != null && getIntent().hasExtra(Constants.EXTRA_TRACKS)) {
            ArrayList<Parcelable> objectList = getIntent().getParcelableArrayListExtra(
                    Constants.EXTRA_TRACKS);
            int position = getIntent().getIntExtra(Constants.EXTRA_CURRENT_TRACK, 0);
            ArrayList<TrackParcelable> trackList = new ArrayList<>();
            for (Parcelable object : objectList){
                trackList.add((TrackParcelable) object);
            }

            PlayerFragment playerFragment = PlayerFragment.getInstance(trackList, position);
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
//        // To make it fullscreen, use the 'content' root view as the container
//        // for the fragment, which is always the root view for the activity
            transaction.add(android.R.id.content, playerFragment).commit();
        } else {
            Log.e(LOG_TAG, "no track found...");
        }

//        if (mTracks != null){
//            Log.v(LOG_TAG, "track found: " + mTracks[mPosition].getName());
//            showDialog();
//        } else {
//            Log.v(LOG_TAG, "no track found...");
//        }

    }

//    public void showDialog() {
//        FragmentManager fragmentManager = getSupportFragmentManager();
////        PlayerFragment newFragment = new PlayerFragment();
////        Bundle args = new Bundle();
////        args.putParcelableArray(PlayerFragment.EXTRA_TRACKS, mTracks);
////        newFragment.setArguments(args);
//
//        PlayerFragment.getInstance()
//        // only for small layout
//        FragmentTransaction transaction = fragmentManager.beginTransaction();
//        // For a little polish, specify a transition animation
//        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
//        // To make it fullscreen, use the 'content' root view as the container
//        // for the fragment, which is always the root view for the activity
//        transaction.add(android.R.id.content, newFragment)
//                .commit();
//
////        Log.v(LOG_TAG, "in showDialog, new fragment created ...");
////
////        if (getResources().getBoolean(R.bool.large_layout)) {
////            // The device is using a large layout, so show the fragment as a dialog
////            Log.v(LOG_TAG, "in showDialog large screen = new Dialog fragment will be created");
////            newFragment.show(fragmentManager, PLAYERFRAGMENT_TAG);
////        } else {
////            Log.v(LOG_TAG, "in showDialog small screen = new fragment will be created");
////            // The device is smaller, so show the fragment fullscreen
////            FragmentTransaction transaction = fragmentManager.beginTransaction();
////            // For a little polish, specify a transition animation
////            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
////            // To make it fullscreen, use the 'content' root view as the container
////            // for the fragment, which is always the root view for the activity
////            transaction.add(android.R.id.content, newFragment)
//////                    .addToBackStack(null).commit();
////                    .commit();
////        }
//    }
}
