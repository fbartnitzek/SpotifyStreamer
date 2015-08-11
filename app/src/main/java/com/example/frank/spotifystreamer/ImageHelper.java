package com.example.frank.spotifystreamer;

import java.util.List;

import kaaes.spotify.webapi.android.models.Image;

/**
 * Created by frank on 24.06.15.
 */
class ImageHelper {

    private static final String LOG_TAG = ImageHelper.class.getSimpleName();

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

    public static String getLargestImage(List<Image> images) {

        if (images != null && images.size()>0){
            float maxSize = 0;
            int maxSizeIndex = 0;
            for (int i = 0;i<images.size();++i){
                if (images.get(i).width >= maxSize) {
                    maxSize = images.get(i).width;
                    maxSizeIndex = i;
                }
            }
            return images.get(maxSizeIndex).url;
        }
        return null;
    }
}
