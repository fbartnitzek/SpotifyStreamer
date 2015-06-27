package com.example.frank.spotifystreamer;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import kaaes.spotify.webapi.android.models.Artist;

/**
 * Created by frank on 24.06.15.
 */
class ArtistAdapter extends ArrayAdapter<Artist> {


    private static final String LOG_TAG = ArtistAdapter.class.getSimpleName();
    private final float iconSize;
    private final Activity activity;    //seems to be resource-friendlier this way (part of ViewHolder-pattern)

    public ArtistAdapter(Activity activity) {
        super(activity, R.layout.list_item_artist);   // second part just for documentation...
        this.activity = activity;

        // seems to convert out of the box (dp to px)
        iconSize = activity.getResources().getDimension(R.dimen.artist_image_size);

//        float dp = activity.getResources().getDimension(R.dimen.artist_image_size);
//        float density = getContext().getApplicationContext().getResources().getDisplayMetrics().density;
//        iconSize = Math.round(dp * density);
    }
//
    private static class ViewHolder {
        ImageView imageView;
        TextView textView;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View rowView = convertView;
        if (rowView == null){
            // previously
//            LayoutInflater inflater = LayoutInflater.from(getContext());
//            convertView = inflater.inflate(R.layout.list_item_artist, parent, false);
            // webcast-sample
//            convertView = LayoutInflater.from(getContext()).
//                    inflate(R.layout.list_item_artist, parent, false);
            // http://www.vogella.com/tutorials/AndroidListView/article.html
            LayoutInflater inflater = activity.getLayoutInflater();
            rowView = inflater.inflate(R.layout.list_item_artist, null);

            ViewHolder viewHolder = new ViewHolder();
            viewHolder.imageView = (ImageView) rowView.findViewById(R.id.list_item_artist_image);
            viewHolder.textView = (TextView) rowView.findViewById(R.id.list_item_artist_name);
            rowView.setTag(viewHolder);
        }

        ViewHolder viewHolder = (ViewHolder) rowView.getTag();
        Artist artist = this.getItem(position);

        // artistName
        viewHolder.textView.setText(artist.name);

        // artistIcon
        String url = ImageHelper.getSmallestMatchingImage(artist.images, iconSize);
        if (url != null){
            Picasso.with(getContext()).load(url).into(viewHolder.imageView);
        }

        return rowView;
    }

}
