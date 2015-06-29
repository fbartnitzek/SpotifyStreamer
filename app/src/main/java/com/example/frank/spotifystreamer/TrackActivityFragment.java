package com.example.frank.spotifystreamer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
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
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


/**
 * A placeholder fragment containing a simple view.
 */
public class TrackActivityFragment extends Fragment {

    private static final String LOG_TAG = TrackActivityFragment.class.getSimpleName();
    private static final String TRACK_LISTVIEW_STATE = "TRACK_LISTVIEW_STATE";
    private static final String TITLE_STATE= "TITLE_STATE";
    private float iconSize;
    private TrackAdapter mTrackAdapter;

    public TrackActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        iconSize = getActivity().getResources().getDimension(R.dimen.album_image_size);
        mTrackAdapter = new TrackAdapter(getActivity());

        View rootView = inflater.inflate(R.layout.fragment_track, container, false);

        // listview to show tracks
        ListView listView = (ListView) rootView.findViewById(R.id.listview_tracks);
        listView.setAdapter(mTrackAdapter);

        if (savedInstanceState != null){

            if (savedInstanceState.containsKey(TITLE_STATE)){    // restore artist-title
                getActivity().setTitle(savedInstanceState.getString(TITLE_STATE));
            }

            if (savedInstanceState.containsKey(TRACK_LISTVIEW_STATE)){    //restore tracks
                ArrayList<TrackParcelable> allTracks =
                        savedInstanceState.getParcelableArrayList(TRACK_LISTVIEW_STATE);
                if (!allTracks.isEmpty()){
                    mTrackAdapter.addAll(allTracks);
                }
            }

        } else {    // fetch tracks
            Intent intent = getActivity().getIntent();
            if (intent != null && intent.hasExtra(getString(R.string.intent_artist_key))) {
                ArtistParcelable artist = intent.getParcelableExtra(getString(R.string.intent_artist_key));

                getActivity().setTitle(
                        getString(R.string.title_activity_track) + " (" + artist.getName() + ")");

                // fill list
                new FetchTrackTask().execute(artist.getId());
            }
        }

        // try open uri in browser to play the track
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                String previewUrl = null;
                TrackParcelable track = mTrackAdapter.getItem(position);
                if (track != null) {
                    previewUrl = track.getPreviewUrl();
                }

                if (previewUrl != null) {
                    Intent urlIntent = new Intent(Intent.ACTION_VIEW);
                    urlIntent.setData(Uri.parse(previewUrl));
                    startActivity(urlIntent);
                }
            }
        });

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        if (!mTrackAdapter.isEmpty()){
            // array just working with casts...
            ArrayList<TrackParcelable> parcelables = new ArrayList<>();
            for (int i = 0; i<mTrackAdapter.getCount();++i){
                parcelables.add(mTrackAdapter.getItem(i));
            }
            savedInstanceState.putParcelableArrayList(TRACK_LISTVIEW_STATE, parcelables);
        }

        savedInstanceState.putCharSequence(TITLE_STATE, getActivity().getTitle());
    }

    private class FetchTrackTask extends AsyncTask<String, Void, TrackParcelable[]> {

        @Override
        protected TrackParcelable[] doInBackground(String... params) {

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

            spotify.getArtistTopTrack(artistId, map, new Callback<Tracks>() {
                @Override
                public void success(Tracks tracks, Response response) {
                    if (tracks != null && tracks.tracks.size()>0){
                        mTrackAdapter.clear();
                        mTrackAdapter.addAll(convertTracks(tracks.tracks));
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

        /** converts spotify-wrapper-tracks to parcelable tracks
        (containing name, album, albumImageUrl)*/
        private List<TrackParcelable> convertTracks(List<Track> tracks) {
            List<TrackParcelable> result = new ArrayList<>();
            for (int i = 0; i<tracks.size();++i){
                Track track = tracks.get(i);
                result.add(new TrackParcelable(
                        track.name,
                        track.album.name,
                        ImageHelper.getSmallestMatchingImage(track.album.images, iconSize),
                        track.preview_url));
            }

            return result;
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
