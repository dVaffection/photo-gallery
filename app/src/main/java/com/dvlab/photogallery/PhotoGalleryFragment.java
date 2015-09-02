package com.dvlab.photogallery;


import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.GridView;

import com.dvlab.photogallery.model.GalleryItem;
import com.dvlab.photogallery.service.FlickrFetchr;

import java.util.ArrayList;
import java.util.List;

public class PhotoGalleryFragment extends Fragment {

    public static final String TAG = PhotoGalleryFragment.class.getSimpleName();

    private List<GalleryItem> items = new ArrayList<>();
    private GridView gridView;
    private int currentPage = 1;
    private FetchItemsTask task;

    public PhotoGalleryFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
        task = new FetchItemsTask();
        task.execute();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photo_gallery, container, false);

        gridView = (GridView) view.findViewById(R.id.grid_view);
        gridView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {

            }

            @Override
            public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (firstVisibleItem + visibleItemCount >= totalItemCount) {
                    if (currentPage < 10) {
                        currentPage ++;

                        // I don't think it works properly
                        Log.i(TAG, "Fetch item for page: " + currentPage);
                        if (task != null) {
                            task.cancel(false);

                            task = new FetchItemsTask();
                            task.execute();
                        }
                    }
                }
            }
        });

        setupAdapter();

        return view;
    }

    private void setupAdapter() {
        if (getActivity() == null || gridView == null) return;

        if (items != null) {
            Log.i(TAG, "Current number of items: " + items.size());
            gridView.setAdapter(new ArrayAdapter<GalleryItem>(getActivity(), android.R.layout.simple_gallery_item, items));
        } else {
            gridView.setAdapter(null);
        }
    }

    private class FetchItemsTask extends AsyncTask<Void, Void, List<GalleryItem>> {
        @Override
        protected List<GalleryItem> doInBackground(Void... voids) {
            List<GalleryItem> items = new FlickrFetchr().fetchItems(currentPage);

            Log.i(TAG, "Items size: " + items.size());

            return items;
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            super.onPostExecute(items);

            PhotoGalleryFragment.this.items.addAll(items);
            setupAdapter();
        }
    }

}
