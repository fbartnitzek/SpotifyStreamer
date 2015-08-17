package com.example.frank.spotifystreamer;

import java.util.List;
import java.util.concurrent.TimeUnit;

import kaaes.spotify.webapi.android.models.Image;

/**
 * Created by frank on 24.06.15.
 */
class Util {

    private static final String LOG_TAG = Util.class.getSimpleName();

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

    // src: http://stackoverflow.com/questions/9027317/how-to-convert-milliseconds-to-hhmmss-format
    public static String formatTime(int millis){
        if (millis < Constants.MILLIS_IN_HOUR){
            return String.format("%02d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes(millis) -
                            TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
                    TimeUnit.MILLISECONDS.toSeconds(millis) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
        } else {
            return String.format("%02d:%02d:%02d",
                    TimeUnit.MILLISECONDS.toHours(millis),
                    TimeUnit.MILLISECONDS.toMinutes(millis) -
                            TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
                    TimeUnit.MILLISECONDS.toSeconds(millis) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
        }

    }

}
