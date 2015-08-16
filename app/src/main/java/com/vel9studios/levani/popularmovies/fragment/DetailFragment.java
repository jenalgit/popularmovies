package com.vel9studios.levani.popularmovies.fragment;

import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.vel9studios.levani.popularmovies.R;
import com.vel9studios.levani.popularmovies.constants.AppConstants;
import com.vel9studios.levani.popularmovies.constants.DetailFragmentConstants;
import com.vel9studios.levani.popularmovies.data.FetchVideosTask;
import com.vel9studios.levani.popularmovies.data.MoviesContract;
import com.vel9studios.levani.popularmovies.util.Utility;
import com.vel9studios.levani.popularmovies.views.TrailerAdapter;

import java.util.ArrayList;


public class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private final String LOG_TAG = DetailFragment.class.getSimpleName();
    public static final String DETAIL_URI = "URI";

    // id for the movie in detail view, and the id used to fetch videos and reviews
    String mMovieId;

    //views
    TextView mTitle;
    TextView mReleaseDate;
    TextView mVoteAverage;
    TextView mMovieOverview;
    String mFavoriteInd;
    TextView mFavorite;
    TextView mReviews;
    ImageView mPoster;

    // video/trailer values
    TrailerAdapter mTrailerAdapter;
    ListView mTrailerListView;

    //Uris
    Uri mUri;
    Uri mVideosUri;

    private static final int DETAIL_LOADER = 0;
    private static final int VIDEO_LOADER = 1;

    public DetailFragment() {

    }

    //Build detail view of the movie
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Bundle arguments = getArguments();
        if (arguments != null) {
            mUri = arguments.getParcelable(DETAIL_URI);
        }

        if (savedInstanceState != null){
            // Save the user's current game state
            mMovieId = savedInstanceState.getString("movieId");
        }

        //set text elements
        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);

        mTrailerAdapter = new TrailerAdapter(getActivity(), null, 0);
        mTrailerListView = (ListView) rootView.findViewById(R.id.listview_trailers);
        mTrailerListView.setAdapter(mTrailerAdapter);
        mTrailerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // CursorAdapter returns a cursor at the correct position for getItem(), or null
                // if it cannot seek to that position.
                Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
                if (cursor != null) {

                    //http://stackoverflow.com/questions/574195/android-youtube-app-play-video-intent
                    String youtubeKey = cursor.getString(DetailFragmentConstants.COLUMN_VIDEO_KEY);
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(AppConstants.YOUTUBE_URI + youtubeKey));
                    intent.putExtra(AppConstants.YOUTUBE_VIDEO_ID, youtubeKey);
                    startActivity(intent);
                }
            }
        });

        mTitle = (TextView) rootView.findViewById(R.id.detail_movie_title);
        mReleaseDate = (TextView) rootView.findViewById(R.id.detail_movie_release_date);
        mVoteAverage = (TextView) rootView.findViewById(R.id.detail_movie_vote_average);
        mMovieOverview = (TextView) rootView.findViewById(R.id.detail_movie_overview);
        mPoster = (ImageView) rootView.findViewById(R.id.detail_movie_image);
        mFavorite = (TextView) rootView.findViewById(R.id.detail_favorite);
        mReviews = (TextView) rootView.findViewById(R.id.detail_reviews);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(DETAIL_LOADER, null, this);

        super.onActivityCreated(savedInstanceState);
    }

    public void onSortOrderChanged(String sortType){

        mUri = null;
        getLoaderManager().restartLoader(DETAIL_LOADER, null, this);
    }

    public void onFavoriteToggle()
    {
        getLoaderManager().restartLoader(DETAIL_LOADER, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        if (id == DETAIL_LOADER){

            Uri moveDetailUri;

            // if mUri is available, use it, else display details for first movie in db for given sort criteria
            String sortOrder = null;
            if (mUri != null)
                moveDetailUri =  mUri;
            else{
                sortOrder = Utility.getSortOrderQuery(getActivity());
                moveDetailUri = MoviesContract.MoviesEntry.buildFirstMovieUri();
            }

            return new CursorLoader(
                    getActivity(),
                    moveDetailUri,
                    DetailFragmentConstants.MOVIE_DETAIL_COLUMNS,
                    null,
                    null,
                    sortOrder
            );
        }

        if (id == VIDEO_LOADER && mVideosUri != null){

            return new CursorLoader(
                    getActivity(),
                    mVideosUri,
                    DetailFragmentConstants.VIDEO_DETAIL_COLUMNS,
                    null,
                    null,
                    null
            );
        }

        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

        if (cursor != null && cursor.moveToFirst()) {

            int currentLoader = loader.getId();
            if (currentLoader == DETAIL_LOADER){

                // update details view
                String movieTitle = cursor.getString(DetailFragmentConstants.COLUMN_MOVIE_TITLE_ID);
                mTitle.setText(movieTitle);
                mReleaseDate.setText(cursor.getString(DetailFragmentConstants.COLUMN_RELEASE_DATE_ID));
                mVoteAverage.setText(String.valueOf(cursor.getDouble(DetailFragmentConstants.COLUMN_VOTE_AVERAGE_ID)));
                mMovieOverview.setText(cursor.getString(DetailFragmentConstants.COLUMN_OVERVIEW_ID));

                String currentMovieId = cursor.getString(DetailFragmentConstants.COLUMN_MOVIE_ID);

                // if displaying details for a new movie, fetch videos
                if (mMovieId == null || !mMovieId.equals(currentMovieId) )
                    getVideos(currentMovieId);

                mMovieId = currentMovieId;

                mFavoriteInd = cursor.getString(DetailFragmentConstants.COLUMN_FAVORITE_IND_ID);

                String posterPath = cursor.getString(DetailFragmentConstants.COLUMN_IMAGE_PATH_ID);
                String fullPosterPath = AppConstants.IMAGE_BASE_URL + AppConstants.DETAIL_IMAGE_QUERY_WIDTH + posterPath;
                Resources resources = getResources();
                int height = resources.getInteger(R.integer.grid_image_height);
                int width = resources.getInteger(R.integer.grid_image_width);

                Picasso.with(getActivity())
                        .load(fullPosterPath)
                        .resize(width, height)
                        .error(R.drawable.unavailable_poster_black)
                        .into(mPoster);

                // gather values for saving movie to favorites
                ArrayList<String> favoriteValues = new ArrayList<>();
                favoriteValues.add(mMovieId);
                favoriteValues.add(mFavoriteInd);
                favoriteValues.add(movieTitle);
                mFavorite.setTag(favoriteValues);

                // set value for launching ReviewsActivity
                mReviews.setTag(mMovieId);

            } else if (currentLoader == VIDEO_LOADER) {
                mTrailerAdapter.swapCursor(cursor);
            }
        }
    }

    private void getVideos(String currentMovieId) {

        FetchVideosTask fetchVideosTask = new FetchVideosTask(getActivity());
        fetchVideosTask.execute(currentMovieId);
        mVideosUri = MoviesContract.VideosEntry.buildVideosUri(currentMovieId);
        getLoaderManager().initLoader(VIDEO_LOADER, null, this);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's current game state
        savedInstanceState.putString("movieId", mMovieId);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (loader.getId() == VIDEO_LOADER)
            mTrailerAdapter.swapCursor(null);
    }

}