package com.example.frank.spotifystreamer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
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

    public void onCountryChanged() {
        Log.v(LOG_TAG, "onCountryChanged");
        if (mArtist != null) {
            new FetchTrackTask().execute(mArtist.getId());
        }
    }

//    private ShareActionProvider mShareActionProvider;
// seems to be working only with contentProviders...

    public interface TrackCallback {
        void onTrackSelected(ArrayList<TrackParcelable> trackParcelables, int position);
    }


    public TrackFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(LOG_TAG, "onCreate");

        mTracks = new ArrayList<>();
        mTrackAdapter = new TrackAdapter(getActivity());
        Bundle args = getArguments();

        if (args != null && args.containsKey(Constants.ARGS_ARTIST_PARCELABLE)){  // get artist from bundle
            mArtist = args.getParcelable(Constants.ARGS_ARTIST_PARCELABLE);
        } else {
            Log.v(LOG_TAG, "no artist-argument set - should never happen");
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

//    @Override
//    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
////        super.onCreateOptionsMenu(menu, inflater);
//        inflater.inflate(R.menu.menu_track, menu);

//        MenuItem menuItem = menu.findItem(R.id.action_share);
//        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);
//
//        if ( mPosition != ListView.INVALID_POSITION) {
//            mShareActionProvider.setShareIntent(createShareTrackIntent());
//        }
//    }

//    private Intent createShareTrackIntent() {
//        Intent shareIntent = new Intent(Intent.ACTION_SEND);
//        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
//        shareIntent.setType("text/plain");
//        shareIntent.putExtra(Intent.EXTRA_TEXT, "listening to " + mTracks.get(mPosition).getName()
//                + " from " + mArtist.getName() + " (listen now: " + mTracks.get(mPosition).getPreviewUrl()
//                + ")" + Constants.SHARE_APP);
//        return shareIntent;
//    }

    public void updateSelectedTrack(int position) {
        Log.v(LOG_TAG, "updateSelectedTrack to position " + position);
        mPosition = position;
        mListView.setItemChecked(mPosition, true);
        mListView.smoothScrollToPosition(mPosition);
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
//            updateSelectedTrack(mPosition);
        }

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                ArrayList<TrackParcelable> trackList = new ArrayList<>();
                mPosition = position;
                for (int i = 0; i < mTrackAdapter.getCount(); ++i) {
                    trackList.add(mTrackAdapter.getItem(i));
                }
                ((TrackCallback) getActivity()).onTrackSelected(trackList, position);

            }
        });

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        Log.v(LOG_TAG, "onSaveInstanceState");
        if (!mTracks.isEmpty()){

            savedInstanceState.putParcelableArrayList(Constants.STATE_TRACK_LISTVIEW, mTracks);
            savedInstanceState.putInt(Constants.STATE_SELECTED_TRACK, mPosition);
        }

    }

    private class FetchTrackTask extends AsyncTask<String, Void, ArrayList<TrackParcelable>> {

        @Override
        protected void onPostExecute(ArrayList<TrackParcelable> trackList) {
            Log.v(LOG_TAG, "onPostExecute");
            mTrackAdapter.clear();
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
                        return new ArrayList<>();
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
