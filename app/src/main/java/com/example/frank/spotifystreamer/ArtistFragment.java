package com.example.frank.spotifystreamer;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;


/**
 * A placeholder fragment containing a simple view.
 */
public class ArtistFragment extends Fragment {

    private static final String LOG_TAG = ArtistFragment.class.getName();

    private ArtistAdapter mArtistAdapter;
    private ArrayList<ArtistParcelable> mArtists;
    private int mPosition = ListView.INVALID_POSITION;

    // loader and callbacks seems like overkill for an "always online app"
    // might just be useful for history...

    /**
     * DetailFragmentCallback for when an item has been selected.
     */
    public interface Callback {
        void onItemSelected(ArtistParcelable artistUri);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v(LOG_TAG, "onCreate");
        super.onCreate(savedInstanceState);
        mArtists = new ArrayList<>();
        mArtistAdapter = new ArtistAdapter(getActivity());

        if (savedInstanceState != null) {
            mArtists = savedInstanceState.getParcelableArrayList(Constants.STATE_ARTISTS);
            mPosition = savedInstanceState.getInt(Constants.STATE_SELECTED_ARTIST);
        }

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.v(LOG_TAG, "onCreateView");

        View rootView = inflater.inflate(R.layout.fragment_artist, container, false);

        // SearchView
        final SearchView searchView = (SearchView) rootView.findViewById(R.id.search_artist);
        // TODO: start with searchView in focus and keyboard opened
        // does not work as expected ...
//        searchView.requestFocus();
//        getActivity().getWindow().setSoftInputMode(
//                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        searchView.setOnQueryTextListener(
                new SearchView.OnQueryTextListener() {

                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        // interactive search
                        new FetchArtistTask().execute(query);    //start asyncFetchTask
                        return false;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        Log.v(LOG_TAG, "searchView - text changed");
                        if (newText != null && !newText.isEmpty()) {
                            new FetchArtistTask().execute(newText);    //start asyncFetchTask
                        } else {
//                            mArtistAdapter.clear();
                        }
                        return true;
                    }
                }
        );

        // listview to show artists
        ListView mListView = (ListView) rootView.findViewById(R.id.listview_artists);
        mListView.setAdapter(mArtistAdapter);

        if (mArtists != null && !mArtists.isEmpty()){
            mArtistAdapter.addAll(mArtists);

            if (ListView.INVALID_POSITION == mPosition) {
                mListView.smoothScrollToPosition(mPosition);
                mListView.setItemChecked(mPosition, true);
            }
        }

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // remove keyboard from view
                searchView.clearFocus();
                mPosition = position;

                // update MainActivity with ArtistParcelable of selected item

                ((Callback) getActivity()).onItemSelected(mArtistAdapter.getItem(position));
            }
        });

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        Log.v(LOG_TAG, "onSaveInstanceState");

        savedInstanceState.putParcelableArrayList(Constants.STATE_ARTISTS, mArtists);
        savedInstanceState.putInt(Constants.STATE_SELECTED_ARTIST, mPosition);
    }

    public class FetchArtistTask extends AsyncTask<String, Void, ArrayList<ArtistParcelable>>{

        @Override
        protected void onPostExecute(ArrayList<ArtistParcelable> artistParcelables) {
            Log.v(LOG_TAG, "onPostExecute");
            mArtistAdapter.clear();
            if (artistParcelables != null) {
                mArtists = artistParcelables;
                mArtistAdapter.addAll(artistParcelables);
            } else {
                toastError(null);
            }
            mArtistAdapter.notifyDataSetChanged();
        }

        @Override
        protected ArrayList<ArtistParcelable> doInBackground(String... params) {
            // void seems to be no valid option
            Log.v(LOG_TAG, "doInBackground");
            if (params.length == 0){
                return null;
            }

            String artistPattern = params[0];

            // will be called every time...
            Log.v(LOG_TAG, "fetch artists for artistPattern " + artistPattern);

            try {
                SpotifyApi api = new SpotifyApi();
                SpotifyService spotify = api.getService();

                ArtistsPager artists = spotify.searchArtists(artistPattern);

                if (artists == null){
                    return null;
                } else {
                    Log.v(LOG_TAG, artists.artists.items.size() + " artists found");
                    return convertArtists(artists.artists.items);
                }


            } catch (Exception e){
                Log.e(LOG_TAG, "could not fetch artist from spotify: " + artistPattern
                        + " (Exception: " + e.getMessage() + ")");
            }

            return null;
        }

        private ArrayList<ArtistParcelable> convertArtists(List<Artist> artists) {
            ArrayList<ArtistParcelable> list = new ArrayList<>();
            for (int i=0; i<artists.size();++i) {
                Artist artist = artists.get(i);
                list.add(new ArtistParcelable(
                        artist.name,
                        Util.getSmallestMatchingImage(artist.images, getActivity()),
                        artist.id));
            }

            return list;
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
