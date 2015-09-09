package com.dvlab.photogallery;


import android.app.Activity;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.dvlab.photogallery.model.GalleryItem;
import com.dvlab.photogallery.service.FlickrFetchr;
import com.dvlab.photogallery.service.PollService;
import com.dvlab.photogallery.service.ThumbnailDownloader;

import java.util.ArrayList;
import java.util.List;

public class PhotoGalleryFragment extends Fragment {

    public static final String TAG = PhotoGalleryFragment.class.getSimpleName();

    private List<GalleryItem> items = new ArrayList<>();
    private GridView gridView;
    private ThumbnailDownloader<ImageView> thumbnailThread;
//    private int currentPage = 1;
//    private FetchItemsTask task;


    public PhotoGalleryFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
        setHasOptionsMenu(true);

        updateItems();


        thumbnailThread = new ThumbnailDownloader<>(new Handler());
        thumbnailThread.setListener(new ThumbnailDownloader.Listener<ImageView>() {
            @Override
            public void onThumbnailDownloaded(ImageView imageView, Bitmap thumbnail) {
                if (isVisible()) {
                    imageView.setImageBitmap(thumbnail);
                }
            }
        });
        thumbnailThread.start();
        thumbnailThread.getLooper();
        Log.i(TAG, "Background thread started");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        thumbnailThread.quit();
        Log.i(TAG, "Background thread destroyed");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photo_gallery, container, false);

        gridView = (GridView) view.findViewById(R.id.grid_view);
//        gridView.setOnScrollListener(new AbsListView.OnScrollListener() {
//            @Override
//            public void onScrollStateChanged(AbsListView absListView, int i) {
//
//            }
//
//            @Override
//            public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
//                if (firstVisibleItem + visibleItemCount >= totalItemCount) {
//                    if (currentPage < 10) {
//                        currentPage ++;
//
//                        // I don't think it works properly
//                        Log.i(TAG, "Fetch item for page: " + currentPage);
//                        if (task != null) {
//                            task.cancel(false);
//
//                            task = new FetchItemsTask();
//                            task.execute();
//                        }
//                    }
//                }
//            }
//        });

        setupAdapter();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        thumbnailThread.clearQueue();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_gallery, menu);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Pull out the SearchView
            MenuItem searchItem = menu.findItem(R.id.menu_item_search);
            SearchView searchView = (SearchView) searchItem.getActionView();

            // Get the data from our searchable.xml as a SearchableInfo
            SearchManager searchManager = (SearchManager) getActivity()
                    .getSystemService(Context.SEARCH_SERVICE);
            ComponentName name = getActivity().getComponentName();
            SearchableInfo searchInfo = searchManager.getSearchableInfo(name);
            searchView.setSearchableInfo(searchInfo);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // this is never called on 5.1
            case R.id.menu_item_search:
                getActivity().onSearchRequested();
                return true;
            case R.id.menu_item_clear:
                PreferenceManager.getDefaultSharedPreferences(getActivity())
                        .edit()
                        .putString(FlickrFetchr.PREF_SEARCH_QUERY, null)
                        .commit();

                // otherwise `onPrepareOptionsMenu` is not called
                getActivity().invalidateOptionsMenu();

                updateItems();
                return true;
            case R.id.menu_item_toggle_polling:
                boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getActivity());
                PollService.setServiceAlarm(getActivity(), shouldStartAlarm);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    // otherwise `onPrepareOptionsMenu` is not called
                    getActivity().invalidateOptionsMenu();
                }

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        // changing title for polling Flickr menu item
        MenuItem toggleItem = menu.findItem(R.id.menu_item_toggle_polling);

        if (PollService.isServiceAlarmOn(getActivity())) {
            toggleItem.setTitle(R.string.stop_polling);
        } else {
            toggleItem.setTitle(R.string.start_polling);
        }


        // removing query from the search input
        // Why here and not in `onOptionsItemSelected`?
        // Well, looks like this is the only place where we can reach out menu item

        MenuItem searchItem = menu.findItem(R.id.menu_item_search);
        SearchView searchView = (SearchView) searchItem.getActionView();

        String query = PreferenceManager.getDefaultSharedPreferences(getActivity())
                .getString(FlickrFetchr.PREF_SEARCH_QUERY, null);
        if (TextUtils.isEmpty(query)) {
            searchView.setQuery("", false);
        }
    }

    public void updateItems() {
        new FetchItemsTask().execute();
    }

    private void setupAdapter() {
        if (getActivity() == null || gridView == null) return;

        if (items != null) {
            Log.i(TAG, "Current number of items: " + items.size());
//            gridView.setAdapter(new ArrayAdapter<GalleryItem>(getActivity(), android.R.layout.simple_gallery_item, items));
            gridView.setAdapter(new GalleryItemAdapter(items));
        } else {
            gridView.setAdapter(null);
        }
    }

    private class FetchItemsTask extends AsyncTask<Void, Void, List<GalleryItem>> {
        @Override
        protected List<GalleryItem> doInBackground(Void... voids) {
            Activity activity = getActivity();
            if (activity == null)
                return new ArrayList<GalleryItem>();

            String query = PreferenceManager.getDefaultSharedPreferences(activity)
                    .getString(FlickrFetchr.PREF_SEARCH_QUERY, null);


            List<GalleryItem> items;
            if (query != null) {
                items = new FlickrFetchr().search(query);
            } else {
                items = new FlickrFetchr().fetchItems();
            }

            Log.i(TAG, "Items size: " + items.size());

            return items;
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            super.onPostExecute(items);

            PhotoGalleryFragment.this.items = items;
            setupAdapter();
        }
    }

    private class GalleryItemAdapter extends ArrayAdapter<GalleryItem> {
        public GalleryItemAdapter(List<GalleryItem> items) {
            super(getActivity(), 0, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.gallery_item, parent, false);
            }

            ImageView imageView = (ImageView) convertView.findViewById(R.id.gallery_item_image_view);
            imageView.setImageResource(R.mipmap.flickr);

            thumbnailThread.queueThumbnail(imageView, getItem(position).getUrl());

            return convertView;
        }
    }


}
