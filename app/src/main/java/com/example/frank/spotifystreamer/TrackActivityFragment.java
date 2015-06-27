package com.example.frank.spotifystreamer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
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

import java.util.HashMap;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


/**
 * A placeholder fragment containing a simple view.
 */
public class TrackActivityFragment extends Fragment {

    private static final String LOG_TAG = TrackActivityFragment.class.getSimpleName();
    private TrackAdapter mTrackAdapter;

    public TrackActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mTrackAdapter = new TrackAdapter(getActivity());

        View rootView = inflater.inflate(R.layout.fragment_track, container, false);

        ListView listView = (ListView) rootView.findViewById(R.id.listview_details);
        listView.setAdapter(mTrackAdapter);


        Intent intent = getActivity().getIntent();
        if (intent != null && intent.hasExtra(Intent.EXTRA_TEXT)) {
            String[] artistArray = intent.getStringArrayExtra(Intent.EXTRA_TEXT);
            String artistId = artistArray[0];
            String artistName = artistArray[1];

            // change title to artistName
            getActivity().setTitle(getString(R.string.title_activity_track) + " (" + artistName + ")");

            // fill list
            new FetchTrackTask().execute(artistId);

        }

        // try open uri in browser to play the track
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                String previewUrl = null;
                Track track = mTrackAdapter.getItem(position);
                if (track != null){
                    previewUrl = track.preview_url;
                }

                if (previewUrl!=null){
                    Intent urlIntent = new Intent(Intent.ACTION_VIEW);
                    urlIntent.setData(Uri.parse(previewUrl));
                    startActivity(urlIntent);
                }
            }
        });

        return rootView;
    }

    private class FetchTrackTask extends AsyncTask<String, Void, Track[]> {

        @Override
        protected Track[] doInBackground(String... params) {

            if (params.length == 0){
                return null;
            }

            String artistId = params[0];
            Log.v(LOG_TAG, "fetch tracks for artistId " + artistId);

            SpotifyApi api = new SpotifyApi();
            SpotifyService spotify = api.getService();

            // country code is required
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String countryCode = prefs.getString(getString(R.string.pref_country_key),
                    getString(R.string.pref_country_default));

            Map<String, Object> map = new HashMap<>();
            map.put(getString(R.string.pref_country_key), countryCode);

            spotify.getArtistTopTrack(artistId, map, new Callback<Tracks>() {
                @Override
                public void success(Tracks tracks, Response response) {
                    if (tracks != null && tracks.tracks.size()>0){
                        mTrackAdapter.clear();
                        mTrackAdapter.addAll(tracks.tracks);
                    } else {
                        mTrackAdapter.clear();
                        toastError(null);
                    }
                }

                @Override
                public void failure(RetrofitError error) {
                    mTrackAdapter.clear();
                    toastError(error.getLocalizedMessage());

                }
            });

            return null;
        }

        private void toastError (String errorMsg){
            Context context = getActivity();
            CharSequence text;
            if (errorMsg == null){
                text = "Could not fetch top tracks from spotify!";
            } else {
                text = "Could not fetch top tracks from spotify (" + errorMsg + ")!";
            }

            Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
        }
    }
}
