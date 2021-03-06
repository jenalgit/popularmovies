package com.vel9studios.levani.popularmovies.data;

import android.net.Uri;
import android.util.Log;

import com.vel9studios.levani.popularmovies.constants.AppConstants;
import com.vel9studios.levani.popularmovies.constants.AppConstantsPrivate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Data access object for retrieving movie data
 */
public class MoviesDAO {

    private final String LOG_TAG = MoviesDAO.class.getSimpleName();

    public MoviesDAO (){}

    /**
     * Core code from Udacity gist: https://gist.github.com/udacityandroid/d6a7bb21904046a91695
     *
     * Moved code outside of fragment to modularize flow more.
     *
     */
    private String getJSON(Uri uri){

        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        String jsonData = null;

        try {

            //check for API key
            if (AppConstantsPrivate.API_KEY.length() == 0){
                Log.e(LOG_TAG, AppConstants.MESSAGE_API_KEY_WARNING);
                return null;
            }

            URL url = new URL(uri.toString());
            Log.d(LOG_TAG, uri.toString());

            // Create the request and open connection to movie db
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuilder buffer = new StringBuilder();
            if (inputStream == null) {
                // Nothing to do.
                return null;
            }

            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line).append("\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return null;
            }

            jsonData = buffer.toString();

        } catch (IOException e) {

            Log.e(LOG_TAG, AppConstants.MESSAGE_CONNECTION_ERROR);
            return null;

        } finally{
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }

        return jsonData;

    }

    public String getMovieData(String sortParam) {

        Uri uri = Uri.parse(AppConstants.QUERY_BASE_DISCOVER_URL).buildUpon()
                .appendQueryParameter(AppConstants.QUERY_SORT_PARAM, sortParam)
                .appendQueryParameter(AppConstants.QUERY_API_KEY_PARAM, AppConstantsPrivate.API_KEY)
                .build();

        return getJSON(uri);
    }

    public String getVideos(String movieId) {

        Uri uri = Uri.parse(AppConstants.QUERY_BASE_ITEM_URL).buildUpon()
                .appendEncodedPath(movieId)
                .appendEncodedPath(AppConstants.QUERY_VIDEOS_PATH)
                .appendQueryParameter(AppConstants.QUERY_API_KEY_PARAM, AppConstantsPrivate.API_KEY)
                .build();

        return getJSON(uri);
    }

    public String getReviews(String movieId) {

        Uri uri = Uri.parse(AppConstants.QUERY_BASE_ITEM_URL).buildUpon()
                .appendEncodedPath(movieId)
                .appendEncodedPath(AppConstants.QUERY_REVIEWS_PATH)
                .appendQueryParameter(AppConstants.QUERY_API_KEY_PARAM, AppConstantsPrivate.API_KEY)
                .build();

        return getJSON(uri);
    }

}
