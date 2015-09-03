package com.dvlab.photogallery;


import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.dvlab.photogallery.model.GalleryItem;
import com.dvlab.photogallery.service.FlickrFetchr;
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
        new FetchItemsTask().execute();
//        task = new FetchItemsTask();
//        task.execute();

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
            List<GalleryItem> items = new FlickrFetchr().fetchItems(1 /*currentPage*/);

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
