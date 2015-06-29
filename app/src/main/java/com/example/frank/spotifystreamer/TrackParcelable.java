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

    private TrackParcelable(Parcel parcel) {
        this.name = parcel.readString();
        this.album = parcel.readString();
        this.pictureUrl = parcel.readString();
        this.previewUrl = parcel.readString();
    }

    public TrackParcelable(String name, String album, String pictureUrl, String previewUrl) {
        this.name = name;
        this.album = album;
        this.pictureUrl = pictureUrl;
        this.previewUrl = previewUrl;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

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

    public String getName() {
        return name;
    }

    public String getAlbum() {
        return album;
    }
}
