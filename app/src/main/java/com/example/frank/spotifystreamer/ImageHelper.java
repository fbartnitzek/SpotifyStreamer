package com.example.frank.spotifystreamer;

import java.util.List;

import kaaes.spotify.webapi.android.models.Image;

/**
 * Created by frank on 24.06.15.
 */
class ImageHelper {

    public static String getSmallestMatchingImage(List<Image> images, float iconSize) {

        if (images != null && images.size()>0){
            for (int i = images.size()-1;i>=0;--i){
                if (images.get(i).width >= iconSize){
                    return images.get(i).url;
                }
            }
            return images.get(0).url;
        }
        return null;
    }
}
