package com.example.frank.spotifystreamer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;


/**
 * A placeholder fragment containing a simple view.
 */
public class TrackFragment extends Fragment {

    private static final String LOG_TAG = TrackFragment.class.getName();

    private TrackAdapter mTrackAdapter;
    private ArtistParcelable mArtist;
    private ArrayList<TrackParcelable> mTracks;
    private int mPosition;
    private ListView mListView;

    //TODO: does not work
//    08-11 00:28:53.748  22964-22964/? D/Finsky﹕ [1] PackageVerificationReceiver.onReceive: Verification requested, id = 74
//    08-11 00:28:54.376  22964-22964/? D/Finsky﹕ [1] PackageVerificationReceiver.onReceive: Verification requested, id = 74
//    08-11 00:29:00.849  22143-22143/? W/ContextImpl﹕ Calling a method in the system process without a qualified user: android.app.ContextImpl.startService:1686 android.content.ContextWrapper.startService:515 android.content.ContextWrapper.startService:515 com.android.keychain.KeyChainBroadcastReceiver.onReceive:12 android.app.ActivityThread.handleReceiver:2579
//    08-11 00:29:05.663  21950-21950/? I/AmazonVideo.connectivity﹕ NetworkConnectionManager$ConnectivityChangeReceiver.onReceive: Received CONNECTIVITY_ACTION intent. Refreshing network info.

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (Constants.ACTION_TRACK_CHANGED.equals(intent.getAction())) {

                int number = intent.getIntExtra(Constants.EXTRA_TRACK_NUMBER, 0);
                Log.v(LOG_TAG, "onReceive, track changed: " + number
                        + " (previously: " + mPosition + ")");
                mPosition = number;
                mListView.setSelection(mPosition);
                mListView.smoothScrollToPosition(mPosition);
            }
        }
    };
    private LocalBroadcastManager mBroadcastManager;
    private Context mContext;

    public TrackFragment() {}



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getActivity().getApplicationContext();

        mTracks = new ArrayList<>();
        mTrackAdapter = new TrackAdapter(getActivity());
        Bundle args = getArguments();

        mBroadcastManager = LocalBroadcastManager.getInstance(mContext);

        if (args != null && args.containsKey(Constants.ARGS_ARTIST_PARCELABLE)){  // get artist from bundle
            mArtist = args.getParcelable(Constants.ARGS_ARTIST_PARCELABLE);
        } else {
            Log.e(LOG_TAG, "no artist-argument set - should never happen");
            return;
        }

        if (savedInstanceState != null && savedInstanceState.containsKey(
                Constants.STATE_TRACK_LISTVIEW)){
            //restore tracks
            mTracks = savedInstanceState.getParcelableArrayList(Constants.STATE_TRACK_LISTVIEW);
            if (savedInstanceState.containsKey(Constants.STATE_SELECTED_TRACK)) {
                mPosition = savedInstanceState.getInt(Constants.STATE_SELECTED_TRACK);
            }
        } else {    // nothing restored from state -> fetch tracks

            // fill list
            new FetchTrackTask().execute(mArtist.getId());
        }



    }

    @Override
    public void onResume() {
        mBroadcastManager.registerReceiver(mReceiver,
                new IntentFilter(Constants.ACTION_TRACK_CHANGED));
        super.onResume();
    }

    @Override
    public void onPause() {
        Log.v(LOG_TAG, "on pause - unregisters receiver");
        mBroadcastManager.unregisterReceiver(mReceiver);
        super.onPause();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_track, container, false);

        // listview to show tracks
        mListView = (ListView) rootView.findViewById(R.id.listview_tracks);
        mListView.setAdapter(mTrackAdapter);

        if (!mTracks.isEmpty() && mTrackAdapter.isEmpty()){
            mTrackAdapter.addAll(mTracks);
        }

        if (!mTrackAdapter.isEmpty() && mPosition != ListView.INVALID_POSITION){
            mListView.smoothScrollToPosition(mPosition);
        }

        // try open uri in browser to play the track
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

//                String previewUrl = null;

//                mPosition = position;
                startPlayer(position);
            }
        });

        return rootView;
    }

    private void startPlayer(int position) {
        ArrayList<TrackParcelable> trackList = new ArrayList<>();
        mPosition = position;
        for (int i = 0; i < mTrackAdapter.getCount(); ++i) {
            trackList.add(mTrackAdapter.getItem(i));
        }

        // TODO: use TwoPane from mainActivity ...
        if (getResources().getBoolean(R.bool.large_layout)) {
            // dialog over both views in same activity
            Log.v(LOG_TAG, "in onItemClickListener - starting player dialog with track "
                    + trackList.get(mPosition).getName());

            FragmentManager fm = getActivity().getSupportFragmentManager();

            PlayerFragment player = PlayerFragment.getInstance(trackList, mPosition);
            player.show(fm, Constants.TAG_PLAYER);

        } else {
            // intent to new activity for small screens
            Intent playerIntent = new Intent(getActivity(), PlayerActivity.class)
                    .putParcelableArrayListExtra(Constants.EXTRA_TRACKS, trackList)
                    .putExtra(Constants.EXTRA_CURRENT_TRACK, mPosition);
            Log.v(LOG_TAG, "in onItemClickListener - starting player intent with track "
                    + trackList.get(mPosition).getName());
            startActivity(playerIntent);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        Log.v(LOG_TAG, "onSaveInstanceState");
        if (!mTracks.isEmpty()){
            // array just working with casts...
//            ArrayList<TrackParcelable> parcelables = new ArrayList<>();
//            for (int i = 0; i<mTrackAdapter.getCount();++i){
//                parcelables.add(mTrackAdapter.getItem(i));
//            }
            savedInstanceState.putParcelableArrayList(Constants.STATE_TRACK_LISTVIEW, mTracks);
            savedInstanceState.putInt(Constants.STATE_SELECTED_TRACK, mPosition);
        }

//        savedInstanceState.putCharSequence(TITLE_STATE, getActivity().getTitle());
    }

    private class FetchTrackTask extends AsyncTask<String, Void, ArrayList<TrackParcelable>> {

        @Override
        protected void onPostExecute(ArrayList<TrackParcelable> trackList) {
            Log.v(LOG_TAG, "onPostExecute");
            mTrackAdapter.clear();
            // DONE: Notification for artists without toptracks, sample: Freaks in Love
            if (trackList == null){
                toastError(null);
            } else {
                mTracks = trackList;
                mTrackAdapter.addAll(trackList);
                if (trackList.size()<1){
                    toastError("no top tracks found for artist");
                }
            }

            mTrackAdapter.notifyDataSetChanged();
        }

        @Override
        protected ArrayList<TrackParcelable> doInBackground(String... params) {
            Log.v(LOG_TAG, "doInBackground");
            if (params.length == 0){
                return null;
            }

            String artistId = params[0];

            // wont be called every time :-)
//            Log.v(LOG_TAG, "fetch tracks for artistId " + artistId);

            SpotifyApi api = new SpotifyApi();
            SpotifyService spotify = api.getService();

            // country code is required
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String countryCode = prefs.getString(getString(R.string.pref_country_key),
                    getString(R.string.pref_country_default));

            Map<String, Object> map = new HashMap<>();
            map.put(getString(R.string.pref_country_key), countryCode);

            try {
                Tracks topTracks  = spotify.getArtistTopTrack(artistId, map);

                if (topTracks == null){
                    return null;
                } else {
                    if (topTracks.tracks.size()<1){
                        return new ArrayList<TrackParcelable>();
                    }
                    Log.v(LOG_TAG, topTracks.tracks.size() + " topTracks found");
                    return convertTracks(topTracks.tracks);
                }

            } catch (Exception e){
                Log.e(LOG_TAG, "could not fetch tracks from spotify, artistId: " + artistId
                        + " (Exception: " + e.getMessage() + ")");
            }

            return null;
        }

        /** converts spotify-wrapper-tracks to parcelable tracks
        (containing name, album, albumImageUrl)*/
        private ArrayList<TrackParcelable> convertTracks(List<Track> tracks) {
            ArrayList<TrackParcelable> list = new ArrayList<>();
            for (int i = 0; i<tracks.size();++i){
                Track track = tracks.get(i);
                list.add(new TrackParcelable(
                        track.name,
                        track.album.name,
                        Util.getLargestImage(track.album.images),
                        track.preview_url,
                        mArtist.getName()
                ));
            }
//            TrackParcelable[] result = list.toArray(new TrackParcelable[list.size()]);
            return list;
        }

        private void toastError (String errorMsg){
            Context context = getActivity();
            CharSequence text;
            if (errorMsg == null){
                text = "Could not fetch top tracks from spotify!";
            } else {
                text = "Could not fetch top tracks from spotify (" + errorMsg + ")!";
            }

            Toast.makeText(context, text, Toast.LENGTH_LONG).show();
        }
    }
}
