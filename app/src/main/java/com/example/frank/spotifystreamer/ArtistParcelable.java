package com.example.frank.spotifystreamer;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by frank on 29.06.15.
 */
public class ArtistParcelable implements Parcelable {

    private final String name;
    private final String pictureUrl;
    private final String id;

    private ArtistParcelable(Parcel parcel) {
        this.name = parcel.readString();
        this.pictureUrl = parcel.readString();
        this.id = parcel.readString();
    }

    public ArtistParcelable(String name, String pictureUrl, String id){
        this.name = name;
        this.pictureUrl = pictureUrl;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(name);
        parcel.writeString(pictureUrl);
        parcel.writeString(id);
    }

    public static final Parcelable.Creator<ArtistParcelable> CREATOR = new Parcelable.Creator<ArtistParcelable>(){

        @Override
        public ArtistParcelable createFromParcel(Parcel source) {
            return new ArtistParcelable(source);
        }

        @Override
        public ArtistParcelable[] newArray(int size) {
            return new ArtistParcelable[size];
        }
    };

    public String getId() {
        return id;
    }
}
