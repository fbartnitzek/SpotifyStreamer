package com.example.frank.spotifystreamer;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import kaaes.spotify.webapi.android.models.Track;

/**
 * Created by frank on 24.06.15.
 */
class TrackAdapter extends ArrayAdapter<Track> {

    private static final String LOG_TAG = TrackAdapter.class.getSimpleName();
    private final float iconSize;
    private final Activity activity;

    public TrackAdapter(Activity activity) {

        super(activity, R.layout.list_item_track);
        this.activity = activity;
        iconSize = activity.getResources().getDimension(R.dimen.album_image_size);
    }

    private static class ViewHolder {
        ImageView image;
        TextView track;
        TextView album;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View rowView = convertView;
        if (rowView == null){

            // ViewHolder-pattern based on
            // http://www.vogella.com/tutorials/AndroidListView/article.html

            LayoutInflater inflater = activity.getLayoutInflater();
            rowView = inflater.inflate(R.layout.list_item_track, null);

            ViewHolder viewHolder = new ViewHolder();
            viewHolder.image = (ImageView) rowView.findViewById(R.id.list_item_track_album_image);
            viewHolder.album= (TextView) rowView.findViewById(R.id.list_item_track_album_name);
            viewHolder.track = (TextView) rowView.findViewById(R.id.list_item_track_name);
            rowView.setTag(viewHolder);
        }

        ViewHolder viewHolder = (ViewHolder) rowView.getTag();

        Track track = this.getItem(position);

        // track name
        viewHolder.track.setText(track.name);

        // album name
        viewHolder.album.setText(track.album.name);

        // album icon
        String url = ImageHelper.getSmallestMatchingImage(track.album.images, iconSize);
        if (url != null){
            Picasso.with(getContext()).load(url).into(viewHolder.image);
        }

        return rowView;
    }
}
