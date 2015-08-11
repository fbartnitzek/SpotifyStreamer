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


// TODO: wlan not active - refine search...! => service not available

/**
 * A placeholder fragment containing a simple view.
 */
public class ArtistFragment extends Fragment {

    private static final String LOG_TAG = ArtistFragment.class.getSimpleName();
    private static final String ARTIST_LISTVIEW_STATE = "ARTIST_LISTVIEW_STATE";
    private static final String SELECTED_KEY = "SELECTED_KEY";
    private ArtistAdapter mArtistAdapter;
    private float iconSize;
    private ListView mListView;
    private int mPosition = ListView.INVALID_POSITION;

    // loader and callbacks seems like overkill for an "always online app"
    // might just be useful for history...

    /**
     * DetailFragmentCallback for when an item has been selected.
     */
    public interface Callback {
        public void onItemSelected(ArtistParcelable artistUri);
    }


    public ArtistFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        iconSize = getActivity().getResources().getDimension(R.dimen.artist_image_size);
        mArtistAdapter = new ArtistAdapter(getActivity());

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
//                        new FetchArtistTask().execute(query);    //start asyncFetchTask
                        return false;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        if (newText != null && !newText.isEmpty()) {
                            new FetchArtistTask().execute(newText);    //start asyncFetchTask
                        } else {
                            mArtistAdapter.clear();
                        }
                        return true;
                    }
                }
        );

        // listview to show artists
        mListView = (ListView) rootView.findViewById(R.id.listview_artists);
        mListView.setAdapter(mArtistAdapter);

        if (savedInstanceState != null){
            if (savedInstanceState.containsKey(ARTIST_LISTVIEW_STATE)) {
                // restore artist list
                ArrayList<ArtistParcelable> allArtists =
                        savedInstanceState.getParcelableArrayList(ARTIST_LISTVIEW_STATE);
                if (!allArtists.isEmpty()) {
                    mArtistAdapter.addAll(allArtists);
                }
            }
            if (savedInstanceState.containsKey(SELECTED_KEY)) {
                mPosition = savedInstanceState.getInt(SELECTED_KEY);
                if (mPosition != ListView.INVALID_POSITION){
                    mListView.smoothScrollToPosition(mPosition);
                }
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

        if (mArtistAdapter.isEmpty()){
            return;
        }

        ArrayList<ArtistParcelable> artists = new ArrayList<>();

        for (int i = 0; i<mArtistAdapter.getCount();++i){
            artists.add(mArtistAdapter.getItem(i));
        }

        savedInstanceState.putParcelableArrayList(ARTIST_LISTVIEW_STATE, artists);
        savedInstanceState.putInt(SELECTED_KEY, mPosition);
    }

    public class FetchArtistTask extends AsyncTask<String, Void, ArtistParcelable[]>{

        @Override
        protected void onPostExecute(ArtistParcelable[] artistParcelables) {
            mArtistAdapter.clear();
            if (artistParcelables != null){
                mArtistAdapter.addAll(artistParcelables);
            } else {
                toastError(null);
            }
            mArtistAdapter.notifyDataSetChanged();
        }

        @Override
        protected ArtistParcelable[] doInBackground(String... params) {
            // void seems to be no valid option

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

        private ArtistParcelable[] convertArtists(List<Artist> artists) {
            List<ArtistParcelable> list = new ArrayList<>();
            for (int i=0; i<artists.size();++i) {
                Artist artist = artists.get(i);
                list.add(new ArtistParcelable(
                        artist.name,
                        ImageHelper.getSmallestMatchingImage(artist.images, iconSize),
                        artist.id));
            }

            ArtistParcelable[] result = list.toArray(new ArtistParcelable[list.size()]);
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
