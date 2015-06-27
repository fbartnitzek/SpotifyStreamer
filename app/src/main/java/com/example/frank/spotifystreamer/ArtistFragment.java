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
    private ArtistAdapter mArtistAdapter;

    public ArtistFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mArtistAdapter = new ArtistAdapter(getActivity());

        View rootView = inflater.inflate(R.layout.fragment_artist, container, false);

        // listview to show artists
        ListView listView = (ListView) rootView.findViewById(R.id.listview_artists);
        listView.setAdapter(mArtistAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String artistId = mArtistAdapter.getItem(position).id;
                String artistName = mArtistAdapter.getItem(position).name;
                String[] artistArray = new String[]{artistId, artistName};
                Intent trackIntent = new Intent(getActivity(), TrackActivity.class)
                        .putExtra(Intent.EXTRA_TEXT, artistArray);
                startActivity(trackIntent);
            }
        });

        // textbox to search artist
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


    public class FetchArtistTask extends AsyncTask<String, Void, Artist[]>{

        @Override
        protected Artist[] doInBackground(String... params) {
            // void seems to be no valid option

            if (params.length == 0){
                return null;
            }

            String artistPattern = params[0];

            try {
                SpotifyApi api = new SpotifyApi();
                SpotifyService spotify = api.getService();

                spotify.searchArtists(artistPattern, new Callback<ArtistsPager>() {
                    @Override
                    public void success(ArtistsPager artistsPager, Response response) {
                        if (artistsPager!=null && artistsPager.artists.items.size()>0){
                            mArtistAdapter.clear();
                            mArtistAdapter.addAll(artistsPager.artists.items);

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
