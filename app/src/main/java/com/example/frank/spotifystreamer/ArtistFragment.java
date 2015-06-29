package com.example.frank.spotifystreamer;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


/**
 * A placeholder fragment containing a simple view.
 */
public class ArtistFragment extends Fragment {

    private static final String LOG_TAG = ArtistFragment.class.getSimpleName();
    private static final String ARTIST_LISTVIEW_STATE = "ARTIST_LISTVIEW_STATE";
    private ArtistAdapter mArtistAdapter;
    private float iconSize;
    public ArtistFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // seems to convert out of the box (dp to px)
//        iconSize = activity.getResources().getDimension(R.dimen.artist_image_size);

//        float dp = activity.getResources().getDimension(R.dimen.artist_image_size);
//        float density = getContext().getApplicationContext().getResources().getDisplayMetrics().density;
//        iconSize = Math.round(dp * density);

        iconSize = getActivity().getResources().getDimension(R.dimen.artist_image_size);
        mArtistAdapter = new ArtistAdapter(getActivity());

        View rootView = inflater.inflate(R.layout.fragment_artist, container, false);

        // listview to show artists
        ListView listView = (ListView) rootView.findViewById(R.id.listview_artists);
        listView.setAdapter(mArtistAdapter);

        if (savedInstanceState != null && savedInstanceState.containsKey(
                getString(R.string.intent_artist_key))){    // restore artists
            ArrayList<ArtistParcelable> allArtists =
                    savedInstanceState.getParcelableArrayList(ARTIST_LISTVIEW_STATE);
            if(!allArtists.isEmpty()){
                mArtistAdapter.addAll(allArtists);
            }
        } // else {    // empty remains empty - do not fetch artists

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // use parcelableArtist directly
                Intent trackIntent = new Intent(getActivity(), TrackActivity.class)
                        .putExtra(
                                getString(R.string.intent_artist_key),
                                mArtistAdapter.getItem(position));
                startActivity(trackIntent);
            }
        });

        // textbox to search artist
        // will be called every time - parcelableArtist seems a bit useless here
        EditText txtArtist = (EditText) rootView.findViewById(R.id.txt_artist);
        txtArtist.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                new FetchArtistTask().execute(s.toString());    //start asyncFetchTask
            }
        });

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        if (mArtistAdapter.isEmpty()){
            return;
        }

        ArrayList<ArtistParcelable> artists = new ArrayList<>();

        for (int i = 0; i<mArtistAdapter.getCount();++i){
            artists.add(mArtistAdapter.getItem(i));
        }

        savedInstanceState.putParcelableArrayList(ARTIST_LISTVIEW_STATE, artists);
    }

    public class FetchArtistTask extends AsyncTask<String, Void, ArtistParcelable[]>{

        @Override
        protected ArtistParcelable[] doInBackground(String... params) {
            // void seems to be no valid option

            if (params.length == 0){
                return null;
            }

            String artistPattern = params[0];

            // will be called every time...
//            Log.v(LOG_TAG, "fetch artists for artistPattern " + artistPattern);

            try {
                SpotifyApi api = new SpotifyApi();
                SpotifyService spotify = api.getService();

                spotify.searchArtists(artistPattern, new Callback<ArtistsPager>() {
                    @Override
                    public void success(ArtistsPager artistsPager, Response response) {
                        if (artistsPager!=null && artistsPager.artists.items.size()>0){
                            mArtistAdapter.clear();
                            mArtistAdapter.addAll(convertArtists(artistsPager.artists.items));
                        } else {
                            mArtistAdapter.clear();
                            toastError(null);
                        }
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        mArtistAdapter.clear();
                        toastError(error.getLocalizedMessage());

                    }
                });

            } catch (Exception e){
                Log.e(LOG_TAG, "could not fetch artist from spotify: " + artistPattern
                        + " (Exception: " + e.getMessage() + ")");
            }

            return null;
        }

        private List<ArtistParcelable> convertArtists(List<Artist> artists) {
            List<ArtistParcelable> result = new ArrayList<>();
            for (int i=0; i<artists.size();++i) {
                Artist artist = artists.get(i);
                result.add(new ArtistParcelable(
                        artist.name,
                        ImageHelper.getSmallestMatchingImage(artist.images, iconSize),
                        artist.id));
            }

            return result;
        }

        private void toastError (String errorMsg){
            Context context = getActivity();
            CharSequence text;
            if (errorMsg == null){
                text = "No matching artists - try to refine the search.";
            } else {
                text = "No matching artists - try to refine the search (" + errorMsg + ").";
            }

            Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
        }
    }
}
