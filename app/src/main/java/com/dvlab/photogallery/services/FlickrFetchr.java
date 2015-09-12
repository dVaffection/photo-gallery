package com.dvlab.photogallery.services;

import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

import com.dvlab.photogallery.entities.GalleryItem;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class FlickrFetchr {

    public static final String TAG = FlickrFetchr.class.getSimpleName();
    public static final String PREF_SEARCH_QUERY = "searchQuery";
    public static final String PREF_LAST_RESULT_ID = "lastResultId";

    private static final String ENDPOINT = "https://api.flickr.com/services/rest/";
    private static final String API_KEY = "2af9cd87f876d505eeb111f0958e732e";
    private static final String METHOD_GET_RECENT = "flickr.photos.getRecent";
    private static final String METHOD_SEARCH = "flickr.photos.search";
    private static final String PARAM_EXTRAS = "extras";
    private static final String PARAM_TEXT = "text";
    private static final String EXTRA_SMALL_URL = "url_s";
    private static final String EXTRA_LARGE_URL = "url_l";

    private static final String XML_PHOTO = "photo";


    @Nullable
    byte[] getUrlBytes(String urlSpec) throws IOException {
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }

            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            return out.toByteArray();
        } finally {
            connection.disconnect();
        }
    }

    @Nullable
    public String getUrl(String urlSpec) throws IOException {
        byte[] bytes = getUrlBytes(urlSpec);

        if (bytes != null) {
            return new String(bytes);
        } else {
            return null;
        }
    }

    public List<GalleryItem> downloadGalleryItems(String url) {
        List<GalleryItem> items = new ArrayList<>();

        try {
            Log.i(TAG, "URL: " + url);

            String xmlString = getUrl(url);

            if (xmlString != null) {
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                XmlPullParser parser = factory.newPullParser();
                parser.setInput(new StringReader(xmlString));
                parseItems(items, parser);
            }

        } catch (IOException e) {
            Log.e(TAG, "Failed to fetch items", e);
        } catch (XmlPullParserException e) {
            Log.e(TAG, "Failed to parse items", e);
        }

        return items;
    }

    public List<GalleryItem> fetchItems() {
        return fetchItems(1);
    }

    public List<GalleryItem> fetchItems(int page) {
        if (page < 1) {
            throw new IllegalArgumentException("Page must be greater than zero");
        }

        String url = Uri.parse(ENDPOINT).buildUpon()
                .appendQueryParameter("method", METHOD_GET_RECENT)
                .appendQueryParameter("api_key", API_KEY)
                .appendQueryParameter(PARAM_EXTRAS, EXTRA_SMALL_URL + "," + EXTRA_LARGE_URL)
                .appendQueryParameter("page", String.format("%d", page))
                .build().toString();

        return downloadGalleryItems(url);
    }

    public List<GalleryItem> search(String query) {
        String url = Uri.parse(ENDPOINT).buildUpon()
                .appendQueryParameter("method", METHOD_SEARCH)
                .appendQueryParameter("api_key", API_KEY)
                .appendQueryParameter(PARAM_EXTRAS, EXTRA_SMALL_URL + "," + EXTRA_LARGE_URL)
                .appendQueryParameter(PARAM_TEXT, query)
                .build().toString();

        return downloadGalleryItems(url);
    }

    void parseItems(List<GalleryItem> items, XmlPullParser parser) throws XmlPullParserException, IOException {
        int eventType = parser.next();

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && XML_PHOTO.equals(parser.getName())) {
                String id = parser.getAttributeValue(null, "id");
                String caption = parser.getAttributeValue(null, "title");
                String smallUrl = parser.getAttributeValue(null, EXTRA_SMALL_URL);
                String largeUrl = parser.getAttributeValue(null, EXTRA_LARGE_URL);
                String owner = parser.getAttributeValue(null, "owner");

                GalleryItem item = new GalleryItem();
                item.setId(id);
                item.setCaption(caption);
                item.setUrl(smallUrl);
                item.setLargeUrl(largeUrl);
                item.setOwner(owner);
                items.add(item);
            }

            eventType = parser.next();
        }
    }

}
