package com.example.frank.spotifystreamer;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by frank on 29.06.15.
 */
public class TrackParcelable implements Parcelable {

    private final String name;
    private final String album;
    private final String pictureUrl;
    private final String previewUrl;
    private final String artist;
//    private final String pictureUrlLarge;

    private TrackParcelable(Parcel parcel) {
        this.name = parcel.readString();
        this.album = parcel.readString();
        this.pictureUrl = parcel.readString();
        this.previewUrl = parcel.readString();
        this.artist = parcel.readString();
//        this.pictureUrlLarge = parcel.readString();
    }

    public TrackParcelable(String name, String album, String pictureUrl, String previewUrl,
                           String artist) {
        this.name = name;
        this.album = album;
        this.pictureUrl = pictureUrl;
        this.previewUrl = previewUrl;
        this.artist = artist;
//        this.pictureUrlLarge = pictureUrlLarge;
    }


    public String getName() {
        return name;
    }

    public String getAlbum() {
        return album;
    }

    public String getArtist() {return artist;}

    public String getPictureUrl() {
        return pictureUrl;
    }

//    public String getPictureUrlLarge() {
//        return pictureUrlLarge;
//    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(name);
        parcel.writeString(album);
        parcel.writeString(pictureUrl);
        parcel.writeString(previewUrl);
        parcel.writeString(artist);
//        parcel.writeString(pictureUrlLarge);
    }

    public static final Parcelable.Creator<TrackParcelable> CREATOR = new Parcelable.Creator<TrackParcelable>(){

        @Override
        public TrackParcelable createFromParcel(Parcel source) {
            return new TrackParcelable(source);
        }

        @Override
        public TrackParcelable[] newArray(int size) {
            return new TrackParcelable[size];
        }
    };

}
